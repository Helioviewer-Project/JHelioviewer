package org.helioviewer.jhv.viewmodel.view.jp2view;

import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.PlayStateChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.view.AnimationMode;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.cache.DateTimeCache;
import org.helioviewer.jhv.viewmodel.view.cache.HelioviewerDateTimeCache;
import org.helioviewer.jhv.viewmodel.view.cache.ImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.cache.LocalImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.cache.RemoteImageCacheStatus;
import org.helioviewer.jhv.viewmodel.view.jp2view.J2KRender.RenderReasons;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;

/**
 * Implementation of TimedJHVJPXView for JPX files.
 * 
 * <p>
 * This class is an extions of {@link JHVJP2View} for JPX-Files, providing
 * additional movie commands.
 * 
 * <p>
 * For information about image series, see
 * {@link org.helioviewer.jhv.viewmodel.view.JHVJPXView} and
 * {@link org.helioviewer.jhv.viewmodel.view.TimedJHVJPXView}.
 * 
 * @author Markus Langenberg
 */
public class JHVJPXView extends JHVJP2View implements View {

    public int texID;
	// Caching
    private ImageCacheStatus imageCacheStatus;
    private DateTimeCache dateTimeCache;
    
    // Linking movies
    private LinkedMovieManager linkedMovieManager; // if the move is not

    private SpeedType speedType = SpeedType.RELATIVE;
    
    public enum SpeedType{
    	ABSOLUTE, RELATIVE
    }
    // linked, this has to
    // be null

    /**
     * Default constructor.
     * 
     * <p>
     * When the view is not marked as a main view, it is assumed, that the view
     * will only serve one single image and will not have to perform any kind of
     * update any more. The effect of this assumption is, that the view will not
     * try to reconnect to the JPIP server when the connection breaks and that
     * there will be no other timestamps used than the first one.
     * 
     * @param isMainView
     *            Whether the view is a main view or not
     */
    public JHVJPXView(boolean isMainView) {
        super(isMainView);
    }

    /**
     * {@inheritDoc}
     */
    public void setJP2Image(JP2Image newJP2Image) {
        if (!isMainView) {
            super.setJP2Image(newJP2Image);
            imageCacheStatus = ((JHVJPXView) jp2Image.getParentView()).getImageCacheStatus();
            dateTimeCache = ((JHVJPXView) jp2Image.getParentView()).getDateTimeCache();
            return;
        }

        if (jp2Image != null) {
            abolish(false);
        }

        jp2Image = newJP2Image;

        if (newJP2Image.isRemote()) {
            imageCacheStatus = new RemoteImageCacheStatus(this);
        } else {
            imageCacheStatus = new LocalImageCacheStatus(this);
        }
        jp2Image.setImageCacheStatus(imageCacheStatus);

        dateTimeCache = new HelioviewerDateTimeCache(this, jp2Image);

        super.setJP2Image(newJP2Image);

        dateTimeCache.startParsing();
    }

    /**
     * {@inheritDoc}
     */
    public DateTimeCache getDateTimeCache() {
        return dateTimeCache;
    }

    /**
     * {@inheritDoc}
     */
    public ImageCacheStatus getImageCacheStatus() {
        return imageCacheStatus;
    }

    /**
     * {@inheritDoc}
     */
    public void setCurrentFrame(int frameNumber, ChangeEvent event) {
        setCurrentFrame(frameNumber, event, false);
    }

    /**
     * {@inheritDoc}
     */
    public void setCurrentFrame(int frameNumber, ChangeEvent event, boolean forceSignal) {

        frameNumber = Math.max(0, Math.min(getMaximumFrameNumber(), frameNumber));

        if (forceSignal && linkedMovieManager != null) {
            while (getMaximumAccessibleFrameNumber() < imageViewParams.compositionFrame) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            linkedMovieManager.setCurrentFrame(getFrameDateTime(frameNumber), event, forceSignal);
        } else {
            boolean changed;
            changed = setCurrentFrameNumber(frameNumber, event, forceSignal);
            if (changed && linkedMovieManager != null) {
                linkedMovieManager.setCurrentFrame(getFrameDateTime(frameNumber), event, forceSignal);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public void setCurrentFrame(ImmutableDateTime time, ChangeEvent event) {
        setCurrentFrame(time, event, false);
    }

    /**
     * {@inheritDoc}
     */
    public void setCurrentFrame(ImmutableDateTime time, ChangeEvent event, boolean forceSignal) {

        if (time == null)
            return;

        if (linkedMovieManager != null && linkedMovieManager.setCurrentFrame(time, event, forceSignal)) {
            return;
        }

        int frameNumber = -1;
        long timeMillis = time.getMillis();
        long lastDiff, currentDiff = -Long.MAX_VALUE;

        do {
            lastDiff = currentDiff;

            if (dateTimeCache.getDateTime(++frameNumber) == null) {
                return;
            }

            currentDiff = dateTimeCache.getDateTime(frameNumber).getMillis() - timeMillis;
        } while (currentDiff < 0 && frameNumber < jp2Image.getCompositionLayerRange().end);

        if (-lastDiff < currentDiff) {
            setCurrentFrameNumber(frameNumber - 1, event, forceSignal);
        } else {
            setCurrentFrameNumber(frameNumber, event, forceSignal);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentFrameNumber() {
        return imageViewParams.compositionFrame;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumFrameNumber() {
        return jp2Image.getCompositionLayerRange().end;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumAccessibleFrameNumber() {
        if (dateTimeCache == null || imageCacheStatus == null) {
            return -1;
        }
        return Math.min(dateTimeCache.getMetaStatus(), imageCacheStatus.getImageCachedPartiallyUntil());
    }

    /**
     * {@inheritDoc}
     */
    public ImmutableDateTime getCurrentFrameDateTime() {
        return dateTimeCache.getDateTime(getCurrentFrameNumber());
    }

    /**
     * {@inheritDoc}
     */
    public ImmutableDateTime getFrameDateTime(int frameNumber) {
        return dateTimeCache.getDateTime(frameNumber);
    }

    /**
     * {@inheritDoc}
     */
    public void setAnimationMode(AnimationMode mode) {
        if (render != null) {
            render.setAnimationMode(mode);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void setDesiredRelativeSpeed(int framesPerSecond) {
    	speedType = SpeedType.RELATIVE;
        if (render != null) {
            render.setMovieRelativeSpeed(framesPerSecond);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDesiredAbsoluteSpeed(int observationSecondsPerSecond) {
    	speedType = SpeedType.ABSOLUTE;
        if (render != null) {
            render.setMovieAbsoluteSpeed(observationSecondsPerSecond);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void linkMovie() {
        linkedMovieManager = LinkedMovieManager.getActiveInstance();
        linkedMovieManager.linkMovie(this);
    }

    /**
     * {@inheritDoc}
     */
    public void unlinkMovie() {
        if (linkedMovieManager != null) {
            LinkedMovieManager temp = linkedMovieManager;
            linkedMovieManager = null;
            temp.unlinkMovie(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public float getActualFramerate() {

        if (render != null)
            return render.getActualMovieFramerate();

        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void pauseMovie() {
        if (!isMoviePlaying()) {
            return;
        }

        if (linkedMovieManager != null) {
            linkedMovieManager.pauseLinkedMovies();
        }

        readerSignal.signal();
        if (render != null) {
            render.setMovieMode(false);
            render.setLinkedMovieMode(false);
        }

        // send notification
        ChangeEvent fireEvent = null;
        synchronized (event) {
            fireEvent = event.clone();
            event.reinitialize();
        }
        fireEvent.addReason(new PlayStateChangedReason(this, this.linkedMovieManager, false));
        notifyViewListeners(fireEvent);

    }

    /**
     * {@inheritDoc}
     */
    public void playMovie() {
        if (getMaximumFrameNumber() > 1) {
            if (linkedMovieManager == null || !linkedMovieManager.playLinkedMovies()) {
                if (linkedMovieManager == null || linkedMovieManager.isMaster(this)) {
                    if (render != null) {
                        render.setMovieMode(true);
                    }
                    readerSignal.signal();
                    if (readerMode != ReaderMode.ONLYFIREONCOMPLETE) {
                        renderRequestedSignal.signal(RenderReasons.MOVIE_PLAY);
                    }
                } else {
                    if (render != null) {
                        render.setLinkedMovieMode(true);
                    }
                }
                // send notification
                ChangeEvent fireEvent = null;
                synchronized (event) {
                    fireEvent = event.clone();
                    event.reinitialize();
                }
                fireEvent.addReason(new PlayStateChangedReason(this, this.linkedMovieManager, true));
                notifyViewListeners(fireEvent);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    public boolean isMoviePlaying() {
        if (render != null) {
            return render.isMovieMode() || (linkedMovieManager != null && linkedMovieManager.isPlaying());
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void abolish() {
        abolish(true);
    }

    /**
     * Abolishes the jpx view
     * 
     * @param unlinkMovie
     *            true, if the movie should be unlinked before abolishing it
     */
    public void abolish(boolean unlinkMovie) {
        if (unlinkMovie && linkedMovieManager != null) {
            linkedMovieManager.unlinkMovie(this);
        }
        pauseMovie();
        dateTimeCache.stopParsing();
        super.abolish();
    }

    /**
     * {@inheritDoc}
     */
    void setSubimageData(ImageData newImageData, SubImage roi, int compositionLayer) {

        metaData.updateDateTime(dateTimeCache.getDateTime(compositionLayer));
        event.addReason(new TimestampChangedReason(this, dateTimeCache.getDateTime(compositionLayer)));
        
        super.setSubimageData(newImageData, roi, 0);
    }

    public LinkedMovieManager getLinkedMovieManager() {
        return linkedMovieManager;
    }

    /**
     * Internal function for setting the current frame number.
     * 
     * Before actually setting the new frame number, checks wheater that is
     * necessary. If the frame number has changed, also triggers an update of
     * the image.
     * 
     * @param frameNumber
     * @return true, if the frame number has changed
     */
    protected boolean setCurrentFrameNumber(int frameNumber, ChangeEvent event, boolean forceSignal) {
        if (frameNumber != imageViewParams.compositionFrame || forceSignal) {

            imageViewParams.compositionFrame = frameNumber;

            while (getMaximumAccessibleFrameNumber() < imageViewParams.compositionFrame) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            this.event.copyFrom(event);

            readerSignal.signal();
            if (readerMode != ReaderMode.ONLYFIREONCOMPLETE) {
                renderRequestedSignal.signal(RenderReasons.MOVIE_PLAY);
            }

            return true;
        }
        return false;
    }

    /**
     * Recalculates the image parameters.
     * 
     * <p>
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     * 
     * <p>
     * To achieve this, calls {@link #calculateParameter(int, int)} with the
     * currently used number of quality layers and the current frame number.
     * 
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter() {
        return calculateParameter(getCurrentNumQualityLayers(), getCurrentFrameNumber());
    }


	public int getDesiredSpeed() {
		return render.getDesiredSpeed();
	}

	public SpeedType getSpeedType() {
		return speedType;
	}

}
