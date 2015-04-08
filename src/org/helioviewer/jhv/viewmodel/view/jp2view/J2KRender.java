package org.helioviewer.jhv.viewmodel.view.jp2view;

import kdu_jni.KduException;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_ilayer_ref;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.base.math.Interval;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.NonConstantMetaDataChangedReason;
import org.helioviewer.jhv.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ColorMask;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.AnimationMode;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.cache.DateTimeCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_thread_env;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

/**
 * The J2KRender class handles all of the decompression, buffering, and
 * filtering of the image data. It essentially just waits for the shared object
 * in the JP2ImageView to signal it.
 * 
 * @author caplins
 * @author Benjamin Wamsler
 * @author Desmond Amadigwe
 * @author Markus Langenberg
 */
class J2KRender implements Runnable {

	/**
	 * There could be multiple reason that the Render object was signaled. This
	 * enum lists them.
	 */
	public enum RenderReasons {
		NEW_DATA, OTHER, MOVIE_PLAY
	};

	/** The thread that this object runs on. */
	private volatile Thread myThread;

	/** A boolean flag used for stopping the thread. */
	private volatile boolean stop;

	/** A reference to the JP2Image this object is owned by. */
	private JP2Image parentImageRef;

	/** A reference to the JP2ImageView this object is owned by. */
	private JHVJP2View parentViewRef;

	/** A reference to the compositor used by this JP2Image. */
	private Kdu_region_compositor compositorRef;

	/** Used in run method to keep track of the current ImageViewParams */
	private JP2ImageParameter currParams = null;

	private int lastFrame = -1;

	private final static int NUM_BUFFERS = 2;

	/** An integer buffer used in the run method. */
	private int[] localIntBuffer = new int[0];
	private int[][] intBuffer = new int[NUM_BUFFERS][0];
	private int currentIntBuffer = 0;

	/** A byte buffer used in the run method. */
	private byte[][] byteBuffer = new byte[NUM_BUFFERS][0];
	private int currentByteBuffer = 0;

	/** Maximum of samples to process per rendering iteration */
	private static final int MAX_RENDER_SAMPLES = 500000;

	/** Maximum rendering iterations per layer allowed */
	// Is now calculated automatically as num_pix / MAX_RENDER_SAMPLES
	// private final int MAX_RENDER_ITERATIONS = 150;

	/** It says if the render is going to play a movie instead of a single image */
	private boolean movieMode = false;

	private boolean linkedMovieMode = false;

	private int movieSpeed = 20;
	private float actualMovieFramerate = 0.0f;
	private long lastSleepTime = 0;
	private int lastCompositionLayerRendered = -1;

	private NextFrameCandidateChooser nextFrameCandidateChooser = new NextFrameCandidateLoopChooser();
	private FrameChooser frameChooser = new RelativeFrameChooser();

	private JHV_Kdu_thread_env jhv_Kdu_thread_env;

	/**
	 * The constructor.
	 * 
	 * @param _parentViewRef
	 */
	J2KRender(JHVJP2View _parentViewRef) {
		if (_parentViewRef == null)
			throw new NullPointerException();
		parentViewRef = _parentViewRef;

		parentImageRef = parentViewRef.jp2Image;
		compositorRef = parentImageRef.getCompositorRef();

		jhv_Kdu_thread_env = new JHV_Kdu_thread_env();
		try {
			compositorRef.Set_thread_env(
					jhv_Kdu_thread_env, null);
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		stop = false;
		myThread = null;
	}

	/** Starts the J2KRender thread. */
	void start() {

		if (myThread != null)
			stop();

		myThread = new Thread(JHVJP2View.THREAD_GROUP, this, "J2KRender");
		stop = false;
		myThread.setDaemon(true);
		myThread.start();
	}

	/** Stops the J2KRender thread. */
	void stop() {
		if (myThread != null && myThread.isAlive()) {
			try {
				stop = true;

				do {
					myThread.interrupt();
					myThread.join(100);
				} while (myThread.isAlive());

			} catch (InterruptedException ex) {
			} catch (NullPointerException e) {
			} finally {
				myThread = null;

				intBuffer = new int[NUM_BUFFERS][0];
				byteBuffer = new byte[NUM_BUFFERS][0];
			}
		}
	}

	/** Destroys the resources associated with this object */
	void abolish() {
		stop();
		try
        {
            jhv_Kdu_thread_env.Destroy();
        }
        catch(KduException e)
        {
            e.printStackTrace();
        }
	}

	public void setMovieMode(boolean val) {

		if (movieMode) {
			myThread.interrupt();
		}

		movieMode = val;
		if (frameChooser instanceof AbsoluteFrameChooser) {
			((AbsoluteFrameChooser) frameChooser)
					.resetStartTime(currParams.compositionFrame);
		}

	}

	public void setLinkedMovieMode(boolean val) {
		linkedMovieMode = val;
	}

	public void setMovieRelativeSpeed(int framesPerSecond) {

		if (movieMode && lastSleepTime > 1000) {
			myThread.interrupt();
		}

		movieSpeed = framesPerSecond;
		frameChooser = new RelativeFrameChooser();
	}

	public void setMovieAbsoluteSpeed(int secondsPerSecond) {

		if (movieMode && lastSleepTime > 1000) {
			myThread.interrupt();
		}

		movieSpeed = secondsPerSecond;
		frameChooser = new AbsoluteFrameChooser();
	}

	public void setAnimationMode(AnimationMode mode) {
		switch (mode) {
		case LOOP:
			nextFrameCandidateChooser = new NextFrameCandidateLoopChooser();
			break;
		case STOP:
			nextFrameCandidateChooser = new NextFrameCandidateStopChooser();
			break;
		case SWING:
			nextFrameCandidateChooser = new NextFrameCandidateSwingChooser();
			break;
		}
	}

	public float getActualMovieFramerate() {
		return actualMovieFramerate;
	}

	public boolean isMovieMode() {
		return movieMode;
	}

	private void renderFrame(int numLayer) {
		parentImageRef.getLock().lock();

		try {

			compositorRef.Refresh();
			compositorRef.Remove_ilayer(new Kdu_ilayer_ref(), true);

			parentImageRef.deactivateColorLookupTable(numLayer);

			Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();

			compositorRef.Add_ilayer(numLayer, dimsRef1, dimsRef2);

			if (lastCompositionLayerRendered != numLayer) {
				lastCompositionLayerRendered = numLayer;

				parentImageRef.updateResolutionSet(numLayer);

				MetaData metaData = parentViewRef.getMetaData();

				if (metaData.checkForModifications()) {

					parentViewRef.updateParameter();
					currParams = parentViewRef.getImageViewParams();

					parentViewRef
							.addChangedReason(new NonConstantMetaDataChangedReason(
									parentViewRef, metaData));
				}
			}

			compositorRef.Set_max_quality_layers(currParams.qualityLayers);
			compositorRef.Set_scale(false, false, false,
					currParams.resolution.getZoomPercent());

			Kdu_dims requestedBufferedRegion = KakaduUtils
					.roiToKdu_dims(currParams.subImage);

			compositorRef.Set_buffer_surface(requestedBufferedRegion);

			Kdu_dims actualBufferedRegion = new Kdu_dims();
			Kdu_compositor_buf compositorBuf = compositorRef
					.Get_composition_buffer(actualBufferedRegion);

			Kdu_coords actualOffset = new Kdu_coords();
			actualOffset.Assign(actualBufferedRegion.Access_pos());

			Kdu_dims newRegion = new Kdu_dims();

			if (parentImageRef.getNumComponents() < 3) {
				currentByteBuffer = (currentByteBuffer + 1) % NUM_BUFFERS;
				if (currParams.subImage.getNumPixels() != byteBuffer[currentByteBuffer].length
						|| (!movieMode && !linkedMovieMode && !false)) {
					byteBuffer[currentByteBuffer] = new byte[currParams.subImage
							.getNumPixels()];
				}
			} else {
				currentIntBuffer = (currentIntBuffer + 1) % NUM_BUFFERS;
				if (currParams.subImage.getNumPixels() != intBuffer[currentIntBuffer].length
						|| (!movieMode && !linkedMovieMode && !false)) {
					intBuffer[currentIntBuffer] = new int[currParams.subImage
							.getNumPixels()];
				}
			}

			while (!compositorRef.Is_processing_complete()) {
				compositorRef.Process(MAX_RENDER_SAMPLES, newRegion);
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();

				newOffset.Subtract(actualOffset);

				int newPixels = newSize.Get_x() * newSize.Get_y();
				if (newPixels == 0)
					continue;

				localIntBuffer = newPixels > localIntBuffer.length ? new int[newPixels << 1]
						: localIntBuffer;

				compositorBuf.Get_region(newRegion, localIntBuffer);
				
				int srcIdx = 0;
				int destIdx = newOffset.Get_x() + newOffset.Get_y()
						* currParams.subImage.width;

				int newWidth = newSize.Get_x();
				int newHeight = newSize.Get_y();

				if (parentImageRef.getNumComponents() < 3) {
					for (int row = 0; row < newHeight; row++, destIdx += currParams.subImage.width, srcIdx += newWidth) {
						for (int col = 0; col < newWidth; ++col) {
							byteBuffer[currentByteBuffer][destIdx + col] = (byte) ((localIntBuffer[srcIdx
									+ col] >> 8) & 0xFF);
						}
					}
				} else {
					for (int row = 0; row < newHeight; row++, destIdx += currParams.subImage.width, srcIdx += newWidth)
						System.arraycopy(localIntBuffer, srcIdx,
								intBuffer[currentIntBuffer], destIdx, newWidth);
				}
			}

			if (compositorBuf != null)
				compositorBuf.Native_destroy();

		} catch (KduException e) {
			e.printStackTrace();
		} finally {
			parentImageRef.getLock().unlock();
		}

	}

	/**
	 * The method that decompresses and renders the image. It pushes it to the
	 * ViewObserver.
	 */
	public void run() {
		int numFrames = 0;
		lastFrame = -1;
		long tfrm, tmax = 0;
		long tnow, tini = System.currentTimeMillis();

		while (!stop) {
			try {
				parentViewRef.renderRequestedSignal.waitForSignal();
			} catch (InterruptedException ex) {
				continue;
			}

			currParams = parentViewRef.getImageViewParams();

			nextFrameCandidateChooser.updateRange();

			while (!Thread.interrupted() && !stop) {
				tfrm = System.currentTimeMillis();
				int curFrame = currParams.compositionFrame;

				if (parentViewRef instanceof JHVJPXView) {

					JHVJPXView parent = (JHVJPXView) parentViewRef;

					if (parent.getMaximumAccessibleFrameNumber() < curFrame) {
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
						}
						parentViewRef.renderRequestedSignal
								.signal(RenderReasons.NEW_DATA);
						break;
					}
				}

				if (movieMode && parentViewRef instanceof JHVJPXView) {
					JHVJPXView jpxView = ((JHVJPXView) parentViewRef);
					LinkedMovieManager movieManager = jpxView
							.getLinkedMovieManager();
					if (movieManager != null && movieManager.isMaster(jpxView)) {
						movieManager
								.updateCurrentFrameToMaster(new ChangeEvent());
					}
				}

				renderFrame(curFrame);

				int width = currParams.subImage.width;
				int height = currParams.subImage.height;
				if (parentImageRef.getNumComponents() < 3) {

					if (currParams.subImage.getNumPixels() == byteBuffer[currentByteBuffer].length) {
						parentViewRef.setSubimageData(
								new SingleChannelByte8ImageData(width, height,
										byteBuffer[currentByteBuffer],
										new ColorMask()), currParams.subImage,
								curFrame);
					} else {
						System.out.println("J2KRender: Params out of sync, skip frame");
					}

				} else {
					if (currParams.subImage.getNumPixels() == intBuffer[currentIntBuffer].length) {
						parentViewRef
								.setSubimageData(new ARGBInt32ImageData(width,
										height, intBuffer[currentIntBuffer],
										new ColorMask()), currParams.subImage,
										curFrame);
					} else {
						System.out.println("J2KRender: Params out of sync, skip frame");
					}
				}

				if (!movieMode)
					break;
				else {
					currParams = parentViewRef.getImageViewParams();
					numFrames += currParams.compositionFrame - lastFrame;
					lastFrame = currParams.compositionFrame;
					tmax = frameChooser.moveToNextFrame();
					if (lastFrame > currParams.compositionFrame) {
						lastFrame = -1;
					}
					tnow = System.currentTimeMillis();

					if ((tnow - tini) >= 1000) {
						actualMovieFramerate = (numFrames * 1000.0f)
								/ (tnow - tini);
						tini = tnow;
						numFrames = 0;
					}
					lastSleepTime = tmax - (tnow - tfrm);

					if (lastSleepTime > 0) {
						try {
							Thread.sleep(lastSleepTime);
						} catch (InterruptedException ex) {
							break;
						}
					} else {
						Thread.yield();
					}
				}
			}
			numFrames += currParams.compositionFrame - lastFrame;
			lastFrame = currParams.compositionFrame;
			if (lastFrame > currParams.compositionFrame) {
				lastFrame = -1;
			}
			tnow = System.currentTimeMillis();

			if ((tnow - tini) >= 1000) {
				actualMovieFramerate = (numFrames * 1000.0f) / (tnow - tini);
				tini = tnow;
				numFrames = 0;
			}
		}
		byteBuffer = new byte[NUM_BUFFERS][0];
		intBuffer = new int[NUM_BUFFERS][0];
	}

	private abstract class NextFrameCandidateChooser {

		protected Interval<Integer> layers;

		public NextFrameCandidateChooser() {
			updateRange();
		}

		public void updateRange() {
			if (parentImageRef != null) {
				layers = parentImageRef.getCompositionLayerRange();
			}
		}

		protected void resetStartTime(int frameNumber) {
			if (frameChooser instanceof AbsoluteFrameChooser) {
				((AbsoluteFrameChooser) frameChooser)
						.resetStartTime(frameNumber);
			}
		}

		public abstract int getNextCandidate(int lastCandidate);
	}

	private class NextFrameCandidateLoopChooser extends
			NextFrameCandidateChooser {

		public int getNextCandidate(int lastCandidate) {
			if (++lastCandidate > layers.end) {
				resetStartTime(layers.start);
				return layers.start;
			}
			return lastCandidate;
		}
	}

	private class NextFrameCandidateStopChooser extends
			NextFrameCandidateChooser {

		public int getNextCandidate(int lastCandidate) {
			if (++lastCandidate > layers.end) {
				movieMode = false;
				resetStartTime(layers.start);
				return layers.start;
			}
			return lastCandidate;
		}
	}

	private class NextFrameCandidateSwingChooser extends
			NextFrameCandidateChooser {

		private int currentDirection = 1;

		public int getNextCandidate(int lastCandidate) {
			lastCandidate += currentDirection;
			if (lastCandidate < layers.start && currentDirection == -1) {
				currentDirection = 1;
				resetStartTime(layers.start);
				return layers.start + 1;
			} else if (lastCandidate > layers.end && currentDirection == 1) {
				currentDirection = -1;
				resetStartTime(layers.end);
				return layers.end - 1;
			}

			return lastCandidate;
		}
	}

	private interface FrameChooser {
		public long moveToNextFrame();
	}

	private class RelativeFrameChooser implements FrameChooser {
		public long moveToNextFrame() {
			currParams.compositionFrame = nextFrameCandidateChooser
					.getNextCandidate(currParams.compositionFrame);
			return 1000 / movieSpeed;
		}
	}

	private class AbsoluteFrameChooser implements FrameChooser {

		private DateTimeCache dateTimeCache = ((JHVJPXView) parentViewRef)
				.getDateTimeCache();

		private long absoluteStartTime = dateTimeCache.getDateTime(
				currParams.compositionFrame).getMillis();
		private long systemStartTime = System.currentTimeMillis();

		public void resetStartTime(int frameNumber) {
			absoluteStartTime = dateTimeCache.getDateTime(frameNumber)
					.getMillis();
			systemStartTime = System.currentTimeMillis();
		}

		public long moveToNextFrame() {
			int lastCandidate, nextCandidate = currParams.compositionFrame;
			long lastDiff, nextDiff = -Long.MAX_VALUE;

			do {
				lastCandidate = nextCandidate;
				nextCandidate = nextFrameCandidateChooser
						.getNextCandidate(nextCandidate);

				lastDiff = nextDiff;
				nextDiff = Math.abs(dateTimeCache.getDateTime(nextCandidate)
						.getMillis() - absoluteStartTime)
						- ((System.currentTimeMillis() - systemStartTime) * movieSpeed);
			} while (nextDiff < 0);

			if (-lastDiff < nextDiff) {
				currParams.compositionFrame = lastCandidate;
				return lastDiff / movieSpeed;
			} else {
				currParams.compositionFrame = nextCandidate;
				return nextDiff / movieSpeed;
			}
		}
	}

	public int getDesiredSpeed() {
		return movieSpeed;
	}
}
