package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Nullable;
import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.coordinates.HeliographicCoordinate;
import org.helioviewer.jhv.base.downloadmanager.DownloadManager;
import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.gui.statusLabels.CameraListener;
import org.helioviewer.jhv.gui.statusLabels.FramerateStatusPanel;
import org.helioviewer.jhv.gui.statusLabels.PanelMouseListener;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.ImageLayer.PreparedImage;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.PluginLayer;
import org.helioviewer.jhv.opengl.LoadingScreen;
import org.helioviewer.jhv.opengl.NoImageScreen;
import org.helioviewer.jhv.opengl.RayTrace;
import org.helioviewer.jhv.opengl.RayTrace.Ray;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.opengl.camera.CameraInteraction;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.opengl.camera.CameraMode.MODE;
import org.helioviewer.jhv.opengl.camera.CameraPanInteraction;
import org.helioviewer.jhv.opengl.camera.CameraRotationInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomBoxInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomInteraction;
import org.helioviewer.jhv.opengl.camera.animation.CameraAnimation;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.TimeLineListener;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.awt.TextRenderer;

public class MainPanel extends GLCanvas implements GLEventListener, Camera
{
	public static final double MAX_DISTANCE = Constants.SUN_MEAN_DISTANCE_TO_EARTH * 1.8;
	public static final double MIN_DISTANCE = Constants.SUN_RADIUS * 1.2;
	public static final double DEFAULT_CAMERA_DISTANCE = 22 * Constants.SUN_RADIUS;

	public static final double CLIP_NEAR = Constants.SUN_RADIUS / 10;
	public static final double CLIP_FAR = Constants.SUN_RADIUS * 1000;
	public static final double FOV = 10;
	private double aspect = 0.0;

	private Vector3d[] visibleAreaOutline;

	protected Quaternion rotationNow;
	protected Vector3d translationNow;
	protected Quaternion rotationEnd;
	protected Vector3d translationEnd;
	private ArrayList<MainPanel> synchronizedViews;

	private ArrayList<PanelMouseListener> panelMouseListeners;
	private ArrayList<CameraListener> cameraListeners;
	private ArrayList<CameraAnimation> cameraAnimations;

	protected CameraInteraction[] cameraInteractions;

	private boolean cameraTrackingEnabled = false;

	private long lastFrameChangeTime = -1;
	@Nullable
	private LocalDateTime lastDate;

	private int[] frameBufferObject;
	private int[] renderBufferDepth;
	private int[] renderBufferColor;

	protected Dimension sizeForDecoder;
	private float resolutionDivisor = 1;

	private static final int DEFAULT_TILE_WIDTH = 2048;
	private static final int DEFAULT_TILE_HEIGHT = 2048;

	@SuppressWarnings("null")
	public MainPanel(GLContext _context)
	{
		cameraAnimations = new ArrayList<>();
		synchronizedViews = new ArrayList<>();
		panelMouseListeners = new ArrayList<>();
		cameraListeners = new ArrayList<>();
		setSharedContext(_context);

		Layers.addLayerListener(new LayerListener()
		{
			@Override
			public void layerAdded()
			{
				repaint();
			}

			@Override
			public void layersRemoved()
			{
				repaint();
			}

			@Override
			public void activeLayerChanged(@Nullable Layer layer)
			{
				if (Layers.getActiveImageLayer() != null)
					lastDate = null;
				repaint();
			}
		});
		TimeLine.SINGLETON.addListener(new TimeLineListener()
		{
			
			@Override
			public void timeStampChanged(LocalDateTime current, LocalDateTime last)
			{
			}
			
			@Override
			public void timeRangeChanged(LocalDateTime _start, LocalDateTime _end)
			{
			}

			@Override
			public void isPlayingChanged(boolean _isPlaying)
			{
				lastFrameChangeTime = System.currentTimeMillis();
				repaint();
			}
		});
		addMouseListener(new MouseAdapter()
				{
					@Override
					public void mousePressed(@Nullable MouseEvent e)
					{
						if (e == null)
							return;
		
						Ray ray = new RayTrace().cast(e.getX(), e.getY(), MainPanel.this);
						for (CameraInteraction cameraInteraction : cameraInteractions)
							cameraInteraction.mousePressed(e, ray);
					}
		
					@Override
					public void mouseReleased(@Nullable MouseEvent e)
					{
						if (e == null)
							return;
		
						Ray ray = new RayTrace().cast(e.getX(), e.getY(), MainPanel.this);
						for (CameraInteraction cameraInteraction : cameraInteractions)
							cameraInteraction.mouseReleased(e, ray);
					}
		
					@Override
					public void mouseExited(@Nullable MouseEvent e)
					{
						for (PanelMouseListener listener : panelMouseListeners)
							listener.mouseExited();
					}
				});
		addMouseMotionListener(new MouseMotionListener()
		{
			@Override
			public void mouseDragged(@Nullable MouseEvent e)
			{
				if (e == null)
					return;

				Ray ray = new RayTrace().cast(e.getX(), e.getY(), MainPanel.this);
				for (CameraInteraction cameraInteraction : cameraInteractions)
					cameraInteraction.mouseDragged(e, ray);
			}

			@Override
			public void mouseMoved(@Nullable MouseEvent e)
			{
				if (e == null)
					return;

				Ray ray = new RayTrace().cast(e.getX(), e.getY(), MainPanel.this);
				for (PanelMouseListener listener : panelMouseListeners)
					listener.mouseMoved(e, ray);
			}
		});
		addGLEventListener(this);
		addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseWheelMoved(@Nullable MouseWheelEvent e)
			{
				if (e == null)
					return;
				
				Ray ray = new RayTrace().cast(e.getX(), e.getY(), MainPanel.this);
				for (CameraInteraction cameraInteraction : cameraInteractions)
					cameraInteraction.mouseWheelMoved(e, ray);
			}
		});

		rotationNow = rotationEnd = Quaternion.createRotation(0.0, new Vector3d(0, 1, 0));
		translationNow = translationEnd = new Vector3d(0, 0, DEFAULT_CAMERA_DISTANCE);

		cameraInteractions = new CameraInteraction[2];
		cameraInteractions[0] = new CameraZoomInteraction(this, this);
		cameraInteractions[1] = new CameraRotationInteraction(this, this);

		visibleAreaOutline = new Vector3d[40];
	}

	public Quaternion getRotationCurrent()
	{
		return rotationNow;
	}

	public Vector3d getTranslationCurrent()
	{
		return translationNow;
	}

	public Quaternion getRotationEnd()
	{
		return rotationEnd;
	}

	public Vector3d getTranslationEnd()
	{
		return translationEnd;
	}

	public void setRotationEnd(Quaternion _rotationEnd)
	{
		rotationEnd = _rotationEnd;
	}

	public void setTranslationEnd(Vector3d _translationEnd)
	{
		translationEnd = _translationEnd;
	}

	public void setRotationCurrent(Quaternion _rotationNow)
	{
		rotationNow = _rotationNow;
		repaint();
		for (CameraListener listener : cameraListeners)
			listener.cameraChanged();
	}

	public void setTranslationCurrent(Vector3d _translationNow)
	{
		if (_translationNow.isApproxEqual(translationNow, 1E-6))
			return;

		translationNow = _translationNow;
		repaint();
		for (CameraListener listener : cameraListeners)
			listener.cameraChanged();
	}

	public Matrix4d getTransformation()
	{
		return rotationNow.toMatrix().translated(translationNow);
	}

	public void activateRotationInteraction()
	{
		cameraInteractions[1] = new CameraRotationInteraction(this, this);
	}

	public void activatePanInteraction()
	{
		cameraInteractions[1] = new CameraPanInteraction(this, this, 1);
	}

	public void activateZoomBoxInteraction()
	{
		cameraInteractions[1] = new CameraZoomBoxInteraction(this, this);
	}

	protected void advanceFrame()
	{
		long now = System.currentTimeMillis();
		long frameDuration = now - lastFrameChangeTime;

		if (TimeLine.SINGLETON.isPlaying())
		{
			if (TimeLine.SINGLETON.processElapsedAnimationTime(frameDuration))
				lastFrameChangeTime = now;

			if (frameDuration > TimeLine.SINGLETON.getMillisecondsPerFrame())
				resolutionDivisor += 1;
			if (frameDuration < TimeLine.SINGLETON.getMillisecondsPerFrame())
				resolutionDivisor -= 0.05;

			resolutionDivisor = Math.min(Math.max(resolutionDivisor, 1), 4);
		}
		else
			lastFrameChangeTime = now;
	}
	
	
	protected void render(GL2 gl, boolean _showLoadingAnimation)
	{
		FramerateStatusPanel.notifyRenderingNewFrame();
		advanceFrame();

		LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		gl.glClearDepth(1);
		gl.glDepthMask(true);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
		gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		gl.glDepthMask(false);

		while (!cameraAnimations.isEmpty() && cameraAnimations.get(0).isFinished())
			cameraAnimations.remove(0);

		if (!cameraAnimations.isEmpty())
		{
			for (CameraAnimation ca : cameraAnimations)
				ca.animate(this);

			// render another new frame right after this one
			repaint();
		}

		if (cameraTrackingEnabled)
			updateTrackRotation();

		double clipNear = Math.max(translationNow.z - 4 * Constants.SUN_RADIUS, CLIP_NEAR);
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		{
			gl.glOrtho(-1, 1, -1, 1, clipNear, translationNow.z + 4 * Constants.SUN_RADIUS);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();

			gl.glTranslated(0, 0, -translationNow.z);
			if (CameraMode.mode == MODE.MODE_2D)
			{
				ImageLayer il = Layers.getActiveImageLayer();
				if (il != null)
				{
					MetaData md = il.getMetaData(currentDateTime);
					if (md != null)
						rotationNow = md.rotation;
				}
			}
			
			ImageLayer.ensureAppropriateTextureCacheSize();
			
			LinkedHashMap<ImageLayer, Future<PreparedImage>> layers = new LinkedHashMap<>();
			for (Layer layer : Layers.getLayers())
				if (layer.isVisible() && layer instanceof ImageLayer)
					layers.put((ImageLayer) layer,
							((ImageLayer) layer).prepareImageData(this, sizeForDecoder));
			
			for (Entry<ImageLayer, Future<PreparedImage>> l : layers.entrySet())
				try
				{
					if (l.getValue().get() != null)
					{
						// RenderResult r =
						l.getKey().renderLayer(gl, this, l.getValue().get());
					}
				}
				catch (ExecutionException | InterruptedException _e)
				{
					Telemetry.trackException(_e);
				}
			
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
		}
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		
		Quaternion rotation = new Quaternion(rotationNow.a, rotationNow.u.negatedY());
		Matrix4d transformation = rotation.toMatrix().translated(-translationNow.x, translationNow.y,-translationNow.z);
		gl.glMultMatrixd(transformation.m, 0);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glScaled(aspect, aspect, 1);

		if (CameraMode.mode == CameraMode.MODE.MODE_3D)
		{
			new GLU().gluPerspective(MainPanel.FOV, this.aspect, clipNear, translationNow.z + 4 * Constants.SUN_RADIUS);
		}
		else
		{
			double width = Math.tan(Math.toRadians(FOV) / 2) * translationNow.z;
			gl.glOrtho(-width, width, -width, width, clipNear, translationNow.z + 4 * Constants.SUN_RADIUS);
			gl.glScalef((float) (1 / aspect), 1, 1);
		}

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		calculateBounds();
		for (CameraInteraction cameraInteraction : cameraInteractions)
			cameraInteraction.renderInteraction(gl);
		
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LESS);
		gl.glDepthMask(false);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		
		//render plugin layers
		for (Layer layer : Layers.getLayers())
			if (layer.isVisible() && layer instanceof PluginLayer)
				((PluginLayer)layer).renderLayer(gl,this);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
		
		gl.glDepthMask(false);
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		double xScale = aspect > 1 ? 1 / aspect : 1;
		double yScale = aspect < 1 ? aspect : 1;
		gl.glScaled(xScale, yScale, 1);
		gl.glMatrixMode(GL2.GL_MODELVIEW);			
		
		if (DownloadManager.areDownloadsActive() && _showLoadingAnimation)
		{
			int xOffset = (int) (getSurfaceWidth() * 0.85);
			int width = (int) (getSurfaceWidth() * 0.15);
			int yOffset = (int) (getSurfaceHeight() * 0.85);
			int height = (int) (getSurfaceHeight() * 0.15);
			
			gl.glViewport(xOffset, yOffset, width, height);
			LoadingScreen.render(gl);
			gl.glViewport(0, 0, getSurfaceWidth(), getSurfaceHeight());
			repaint(1000);
		}

		boolean noImageScreen = _showLoadingAnimation;
		for (Layer layer : Layers.getLayers())
			if (layer instanceof ImageLayer)
				noImageScreen &= !layer.isVisible();

		if (noImageScreen)
		{
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPushMatrix();
			{
				gl.glLoadIdentity();
				
				double size = Math.max(getSurfaceHeight(), getSurfaceWidth()) * 0.15;
				gl.glViewport((int) (getSurfaceWidth() / 2 - size / 2), (int) (getSurfaceHeight() / 2 - size / 2), (int)size, (int)size);
				
				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glPushMatrix();
				{
					gl.glLoadIdentity();
					NoImageScreen.render(gl);
				}
				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glPopMatrix();
			}
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
		}

		for (MainPanel componentView : synchronizedViews)
			componentView.repaint();

		// force immediate repaints of dependent regions
		RepaintManager.currentManager(MainFrame.SINGLETON).paintDirtyRegions();

		if (TimeLine.SINGLETON.isPlaying())
			repaint();
	}

	protected float getDesiredRelativeResolution()
	{
		 if(TimeLine.SINGLETON.isPlaying())
			 return 0.5f; //or "resolutionDivisor"
		 else
			 return 1;
	}

	@Override
	public void display(@Nullable GLAutoDrawable drawable)
	{
		if(drawable==null)
			return;
		
		sizeForDecoder = getCanavasSize();

		sizeForDecoder = new Dimension((int) (sizeForDecoder.width * getDesiredRelativeResolution()),
				(int) (sizeForDecoder.height * getDesiredRelativeResolution()));

		GL2 gl = drawable.getGL().getGL2();

		gl.glViewport(0, 0, this.getSurfaceWidth(), this.getSurfaceHeight());

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glScaled(1, aspect, 1);
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		render(gl, true);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glLoadIdentity();
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		/*
		 * gl.glDisable(GL2.GL_BLEND); gl.glDisable(GL2.GL_TEXTURE_2D);
		 * gl.glUseProgram(0); gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		 * gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		 * gl.glDisable(GL2.GL_DEPTH_TEST);
		 * 
		 * 
		 * gl.glEnable(GL2.GL_TEXTURE_2D); gl.glActiveTexture(GL.GL_TEXTURE0);
		 * gl.glDisable(GL2.GL_BLEND); gl.glDisable(GL2.GL_TEXTURE_2D);
		 * gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		 * gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB); gl.glUseProgram(0);
		 * gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		 * gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		 * 
		 * gl.glEnable(GL2.GL_BLEND); gl.glBlendFunc(GL2.GL_SRC_ALPHA,
		 * GL2.GL_ONE);
		 */
	}

	protected void updateTrackRotation()
	{
		if (lastDate == null)
			lastDate = TimeLine.SINGLETON.getCurrentDateTime();

		if (!lastDate.isEqual(TimeLine.SINGLETON.getCurrentDateTime()))
		{
			long difference = Duration.between(lastDate, TimeLine.SINGLETON.getCurrentDateTime()).getSeconds();
			lastDate = TimeLine.SINGLETON.getCurrentDateTime();
			
			RayTrace rayTrace = new RayTrace();
			Vector3d hitPoint = rayTrace.cast(getWidth() / 2, getHeight() / 2, this).getHitpoint();
			HeliographicCoordinate newCoord = new HeliocentricCartesianCoordinate(hitPoint.x, hitPoint.y, hitPoint.z).toHeliographicCoordinate();
			double angle = DifferentialRotation.calculateRotationInRadians(newCoord.latitude, difference);
			
			Quaternion newRotation = Quaternion.createRotation(angle, new Vector3d(0, 1, 0)).rotate(rotationNow);
			if (CameraMode.mode == MODE.MODE_3D)
			{
				rotationEnd = Quaternion.createRotation(angle, new Vector3d(0, 1, 0)).rotate(rotationEnd);
				rotationNow = newRotation;
			}
			else
			{
				Vector3d newTranslation = newRotation.toMatrix().multiply(hitPoint);
				translationEnd = translationEnd.add(new Vector3d(newTranslation.x-translationNow.x,newTranslation.y-translationNow.y,0));
				translationNow = new Vector3d(newTranslation.x, newTranslation.y, translationNow.z);
			}
			
			for (CameraListener listener : cameraListeners)
				listener.cameraChanged();
		}
	}

	protected void calculateBounds()
	{
		RayTrace rayTrace = new RayTrace();
		double width = getWidth() / 10d;
		double height = getHeight() / 10d;

		for (int i = 0; i < 10; i++)
		{
			visibleAreaOutline[i   ] = rayTrace.cast((int)(i * width), 0, this).getHitpoint();
			visibleAreaOutline[i+10] = rayTrace.cast(getWidth(), (int)(i * height), this).getHitpoint();
			visibleAreaOutline[i+20] = rayTrace.cast((int)((10 - i) * width), getHeight(), this).getHitpoint();
			visibleAreaOutline[i+30] = rayTrace.cast(0, (int)((10 - i) * height), this).getHitpoint();
		}
	}

	@Override
	public void dispose(@Nullable GLAutoDrawable arg0)
	{
	}

	@Override
	public void init(@Nullable GLAutoDrawable drawable)
	{
		if(drawable==null)
			return;
		
		// if (System.getProperty("jhvVersion") == null)
		// drawable.setGL(new DebugGL2(drawable.getGL().getGL2()));
		// GuiState3DWCS.overViewPanel.activate(drawable.getContext());
		aspect = this.getSize().getWidth() / this.getSize().getHeight();
		GL2 gl = drawable.getGL().getGL2();
		// splashScreen = new NoImageScreen(gl);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		gl.glDisable(GL2.GL_TEXTURE_2D);

		frameBufferObject = new int[1];
		gl.glGenFramebuffers(1, frameBufferObject, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		gl.glEnable(GL2.GL_TEXTURE_2D);

		gl.glDisable(GL2.GL_TEXTURE_2D);
	}

	private void generateNewRenderBuffers(GL2 gl, int width, int height)
	{
		if (renderBufferDepth != null)
			gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);

		renderBufferDepth = new int[1];
		gl.glGenRenderbuffers(1, renderBufferDepth, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferDepth[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, width, height);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER,
				renderBufferDepth[0]);

		if (renderBufferColor != null)
			gl.glDeleteRenderbuffers(1, renderBufferColor, 0);

		renderBufferColor = new int[1];
		gl.glGenRenderbuffers(1, renderBufferColor, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferColor[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, width, height);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER,
				renderBufferColor[0]);
	}

	@Override
	public void reshape(@Nullable GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		aspect = this.getSize().getWidth() / this.getSize().getHeight();
		repaint();
	}

	public Dimension getCanavasSize()
	{
		return new Dimension(getSurfaceWidth(), getSurfaceHeight());
	}

	public BufferedImage getBufferedImage(int imageWidth, int imageHeight, boolean textEnabled)
	{
		ArrayList<String> descriptions = null;
		if (textEnabled)
		{
			descriptions = new ArrayList<>();
			for (Layer layer : Layers.getLayers())
				if (layer.isVisible())
				{
					LocalDateTime ldt = layer.getCurrentTime();
					if (ldt != null)
						descriptions.add(layer.getFullName() + " - " + ldt.format(Globals.DATE_TIME_FORMATTER));
				}
		}

		int tileWidth = imageWidth < DEFAULT_TILE_WIDTH ? imageWidth : DEFAULT_TILE_WIDTH;
		int tileHeight = imageHeight < DEFAULT_TILE_HEIGHT ? imageHeight : DEFAULT_TILE_HEIGHT;
		repaint();
		double xTiles = imageWidth / (double) tileWidth;
		double yTiles = imageHeight / (double) tileHeight;
		int countXTiles = imageWidth % tileWidth == 0 ? (int) xTiles : (int) xTiles + 1;
		int countYTiles = imageHeight % tileHeight == 0 ? (int) yTiles : (int) yTiles + 1;

		GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.getDefault());

		GLProfile profile = GLProfile.get(GLProfile.GL2);
		GLCapabilities capabilities = new GLCapabilities(profile);
		capabilities.setDoubleBuffered(false);
		capabilities.setOnscreen(false);
		capabilities.setHardwareAccelerated(true);
		capabilities.setFBO(true);

		GLDrawable offscreenDrawable = factory.createOffscreenDrawable(null, capabilities, null, tileWidth, tileHeight);

		offscreenDrawable.setRealized(true);
		GLContext offscreenContext = this.getContext();
		offscreenDrawable.setRealized(true);
		offscreenContext.makeCurrent();
		GL2 offscreenGL = offscreenContext.getGL().getGL2();

		offscreenGL.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
		generateNewRenderBuffers(offscreenGL, tileWidth, tileHeight);

		BufferedImage screenshot = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
		ByteBuffer.wrap(((DataBufferByte) screenshot.getRaster().getDataBuffer()).getData());

		offscreenGL.glViewport(0, 0, tileWidth, tileHeight);
		offscreenGL.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		double aspect = imageWidth / (double) imageHeight;
		// double top = Math.tan(MainPanel.FOV / 360.0 * Math.PI) *
		// MainPanel.CLIP_NEAR;
		// double right = top * aspect;
		// double left = -right;
		// double bottom = -top;

		TextRenderer textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 24));
		textRenderer.setColor(1f, 1f, 1f, 1f);

		offscreenGL.glViewport(0, 0, tileWidth, tileHeight);
		this.sizeForDecoder = new Dimension(tileWidth, tileHeight);

		offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
		offscreenGL.glLoadIdentity();
		offscreenGL.glScaled(1, aspect, 1);
		offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);

		for (int x = 0; x < countXTiles; x++)
		{
			for (int y = 0; y < countYTiles; y++)
			{
				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glPushMatrix();
				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);

				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glViewport(0, 0, imageWidth, imageHeight);
				offscreenGL.glTranslated(-x, -y, 0);
				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);

				// double factor =
				int destX = tileWidth * x;
				int destY = tileHeight * y;
				render(offscreenGL, false);

				if (descriptions != null && x == 0 && y == 0)
				{
					int counter = 0;
					textRenderer.beginRendering(this.getSurfaceWidth(), this.getSurfaceHeight());
					for (String description : descriptions)
						textRenderer.draw(description, 5, 5 + 40 * counter++);

					textRenderer.endRendering();
				}
				offscreenGL.glPixelStorei(GL2.GL_PACK_ROW_LENGTH, imageWidth);
				offscreenGL.glPixelStorei(GL2.GL_PACK_SKIP_ROWS, destY);
				offscreenGL.glPixelStorei(GL2.GL_PACK_SKIP_PIXELS, destX);
				offscreenGL.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);

				int cutOffX = imageWidth >= (x + 1) * tileWidth ? tileWidth : tileWidth - x * tileWidth;
				int cutOffY = imageHeight >= (y + 1) * tileHeight ? tileHeight : tileHeight - y * tileHeight;

				offscreenGL.glReadPixels(0, 0, cutOffX, cutOffY, GL2.GL_BGR, GL2.GL_UNSIGNED_BYTE,
						ByteBuffer.wrap(((DataBufferByte) screenshot.getRaster().getDataBuffer()).getData()));
				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glPopMatrix();
				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);
			}
		}

		ImageUtil.flipImageVertically(screenshot);
		return screenshot;
	}

	public double getAspect()
	{
		return this.aspect;
	}

	public void addSynchronizedView(MainPanel compenentView)
	{
		synchronizedViews.add(compenentView);
	}

	public Vector3d[] getVisibleAreaOutline()
	{
		return visibleAreaOutline;
	}

	public void addCameraAnimation(CameraAnimation cameraAnimation)
	{
		cameraAnimations.add(cameraAnimation);
		repaint();
	}

	public void switchToFullscreen()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
				GraphicsDevice graphicsDevice = MainFrame.SINGLETON.getGraphicsConfiguration().getDevice();
				if (graphicsDevice == null)
					graphicsDevice = graphicsEnvironment.getDefaultScreenDevice();

				final JFrame fullscreenFrame = new JFrame(graphicsDevice.getDefaultConfiguration());

				fullscreenFrame.getContentPane().setLayout(new BorderLayout());
				final Container lastParent = MainPanel.this.getParent();
				fullscreenFrame.getContentPane().add(MainPanel.this);

				fullscreenFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
				fullscreenFrame.setUndecorated(true);
				fullscreenFrame.setResizable(false);

				if (graphicsDevice.isFullScreenSupported())
					graphicsDevice.setFullScreenWindow(fullscreenFrame);

				fullscreenFrame.setVisible(true);

				final KeyAdapter keyAdapter = new KeyAdapter()
				{
					@Override
					public void keyPressed(@Nullable KeyEvent e)
					{
						if(e==null)
							return;
						
						if (e.getKeyCode() == KeyEvent.VK_ESCAPE || (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_T))
						{
							MainPanel.this.removeKeyListener(this);
							fullscreenFrame.getContentPane().remove(MainPanel.this);
							lastParent.add(MainPanel.this);
							fullscreenFrame.setVisible(false);

							fullscreenFrame.dispose();
						}
					}
				};

				fullscreenFrame.addKeyListener(keyAdapter);
				MainPanel.this.addKeyListener(keyAdapter);
			}
		});
	}

	public void toggleCameraTracking()
	{
		cameraTrackingEnabled = !cameraTrackingEnabled;
		lastDate = TimeLine.SINGLETON.getCurrentDateTime();
	}

	public void addPanelMouseListener(PanelMouseListener _listener)
	{
		panelMouseListeners.add(_listener);
	}

	public void addCameraListener(CameraListener _listener)
	{
		cameraListeners.add(_listener);
	}

	public void stopAllAnimations()
	{
		cameraAnimations.clear();
		setTranslationEnd(getTranslationCurrent());
		setRotationEnd(getRotationCurrent());
	}
}
