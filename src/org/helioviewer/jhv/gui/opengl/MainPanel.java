package org.helioviewer.jhv.gui.opengl;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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

import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.coordinates.HeliographicCoordinate;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.statusLabels.StatusLabelInterfaces.StatusLabelCameraListener;
import org.helioviewer.jhv.gui.statusLabels.StatusLabelInterfaces.StatusLabelMouseListener;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.AbstractLayer.RenderResult;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.LoadingScreen;
import org.helioviewer.jhv.opengl.NoImageScreen;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.RayTrace;
import org.helioviewer.jhv.opengl.RayTrace.Ray;
import org.helioviewer.jhv.opengl.TextureCache;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.opengl.camera.CameraInteraction;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.opengl.camera.CameraMode.MODE;
import org.helioviewer.jhv.opengl.camera.CameraPanInteraction;
import org.helioviewer.jhv.opengl.camera.CameraRotationInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomBoxInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomInteraction;
import org.helioviewer.jhv.opengl.camera.animation.CameraAnimation;
import org.helioviewer.jhv.opengl.camera.animation.CameraTransformationAnimation;
import org.helioviewer.jhv.plugins.plugin.AbstractPlugin.RENDER_MODE;
import org.helioviewer.jhv.plugins.plugin.Plugins;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.TimeLineListener;

import com.jogamp.opengl.GL;
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

public class MainPanel extends GLCanvas implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener, LayerListener, TimeLineListener, Camera
{
	private static final long serialVersionUID = 6714893614985558471L;

	public static final double MAX_DISTANCE = Constants.SUN_MEAN_DISTANCE_TO_EARTH * 1.8;
	public static final double MIN_DISTANCE = Constants.SUN_RADIUS * 1.2;
	private static final double DEFAULT_CAMERA_DISTANCE = 14 * Constants.SUN_RADIUS;

	public static final double CLIP_NEAR = Constants.SUN_RADIUS / 10;
	public static final double CLIP_FAR = Constants.SUN_RADIUS * 1000;
	public static final double FOV = 10;
	private double aspect = 0.0;

	private double[][] visibleAreaOutline;

	protected Quaternion3d rotation;
	protected Vector3d translation;
	private ArrayList<MainPanel> synchronizedViews;

	private ArrayList<StatusLabelMouseListener> statusLabelsMouseListeners;
	private ArrayList<StatusLabelCameraListener> statusLabelCameraListeners;
	private ArrayList<CameraAnimation> cameraAnimations;

	protected CameraInteraction[] cameraInteractions;

	private boolean cameraTrackingEnabled = false;

	private long lastFrameChangeTime = -1;
	private LocalDateTime lastDate;

	private int[] frameBufferObject;

	private int[] renderBufferDepth;

	private int[] renderBufferColor;

	protected Dimension size;
	private float resolutionDivisor=1;

	private static int DEFAULT_TILE_WIDTH = 2048;
	private static int DEFAULT_TILE_HEIGHT = 2048;

	public MainPanel()
	{
		this.cameraAnimations = new ArrayList<CameraAnimation>();
		this.synchronizedViews = new ArrayList<MainPanel>();
		statusLabelsMouseListeners = new ArrayList<StatusLabelMouseListener>();
		statusLabelCameraListeners = new ArrayList<StatusLabelCameraListener>();
		this.setSharedContext(OpenGLHelper.glContext);

		Layers.addNewLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addGLEventListener(this);
		this.addMouseWheelListener(this);

		this.rotation = Quaternion3d.createRotation(0.0, new Vector3d(0, 1, 0));
		this.translation = new Vector3d(0, 0, DEFAULT_CAMERA_DISTANCE);

		cameraInteractions = new CameraInteraction[2];
		cameraInteractions[0] = new CameraZoomInteraction(this, this);
		cameraInteractions[1] = new CameraRotationInteraction(this, this);

		visibleAreaOutline = new double[40][3];
	}

	public Quaternion3d getRotation() {
		return rotation;
	}

	public void setRotation(Quaternion3d rotation)
	{
		this.rotation = rotation;
		repaint();
		for (StatusLabelCameraListener statusLabelCamera : statusLabelCameraListeners)
			statusLabelCamera.cameraChanged();
	}

	public Vector3d getTranslation() {
		return translation;
	}

	public void setTranslation(Vector3d translation)
	{
		if (!translation.isApproxEqual(this.translation, 0)) {
			this.translation = translation;
			repaint();
			for (StatusLabelCameraListener statusLabelCamera : statusLabelCameraListeners)
				statusLabelCamera.cameraChanged();
		}
	}

	public Matrix4d getTransformation()
	{
		return rotation.toMatrix().translated(translation);
	}

	public Matrix4d getTransformation(Quaternion3d _rotation)
	{
		return rotation.rotate(_rotation).toMatrix().translated(translation);
	}

	public void setZTranslation(double z)
	{
		Vector3d translation = new Vector3d(this.translation.x,
				this.translation.y, Math.max(MIN_DISTANCE,
						Math.min(MAX_DISTANCE, z)));
		if (!translation.isApproxEqual(this.translation, 0)) {
			this.translation = translation;
			repaint();
			for (StatusLabelCameraListener statusLabelCamera : statusLabelCameraListeners) {
				statusLabelCamera.cameraChanged();
			}
		}

	}

	public void setTransformation(Quaternion3d rotation, Vector3d translation) {
		this.rotation = rotation;
		this.translation = translation;
		repaint();
		for (StatusLabelCameraListener statusLabelCamera : statusLabelCameraListeners) {
			statusLabelCamera.cameraChanged();
		}
	}

	public void activateRotationInteraction() {
		this.cameraInteractions[1] = new CameraRotationInteraction(this, this);
	}

	public void activatePanInteraction() {
		this.cameraInteractions[1] = new CameraPanInteraction(this, this);
	}

	public void activateZoomBoxInteraction() {
		this.cameraInteractions[1] = new CameraZoomBoxInteraction(this, this);
	}

	protected void advanceFrame()
	{
		long now = System.currentTimeMillis();
		long frameDuration = now-lastFrameChangeTime;

		if (TimeLine.SINGLETON.isPlaying())
		{
			if(TimeLine.SINGLETON.processElapsedAnimationTime(frameDuration))
				lastFrameChangeTime = now;

			if(frameDuration > TimeLine.SINGLETON.getMillisecondsPerFrame())
				resolutionDivisor += 1;
			if(frameDuration < TimeLine.SINGLETON.getMillisecondsPerFrame())
				resolutionDivisor -= 0.05;
			
			resolutionDivisor = Math.min(Math.max(resolutionDivisor, 1), 4);
		}
		else
			lastFrameChangeTime = now;
	}
	
	protected void render(GL2 gl)
	{
		advanceFrame();
		
		LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		gl.glClearDepth(1);
		gl.glDepthMask(true);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
		gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		gl.glDepthMask(false);

		// Calculate Track
		if (cameraTrackingEnabled)
			updateTrackRotation();
		
		if (Layers.getLayerCount() > 0)
		{
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPushMatrix();

			double clipNear = Math.max(this.translation.z - 4 * Constants.SUN_RADIUS, CLIP_NEAR);
			gl.glOrtho(-1, 1, -1, 1, clipNear, this.translation.z + 4 * Constants.SUN_RADIUS);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();

			gl.glTranslated(0, 0, -translation.z);
			if (CameraMode.mode == MODE.MODE_2D)
			{
				try
				{
					this.rotation = Layers.getActiveImageLayer().getMetaData(currentDateTime).getRotation();
				}
				catch (MetaDataException e)
				{
					e.printStackTrace();
				}
			}
			
			//make sure texturePool is initialized
			//FIXME: should not be necessary. ideally TP interaction should all happen from gl thread
			TextureCache.init();

			LinkedHashMap<AbstractLayer, Future<ByteBuffer>> layers=new LinkedHashMap<AbstractLayer, Future<ByteBuffer>>();
			for (AbstractLayer layer : Layers.getLayers())
				if (layer.isVisible() && layer.isImageLayer())
					try
					{
						layers.put(layer,((AbstractImageLayer)layer).prepareImageData(this, size));
					}
					catch(MetaDataException _mde)
					{
						_mde.printStackTrace();
					}

			for(Entry<AbstractLayer, Future<ByteBuffer>> l:layers.entrySet())
				try
				{
					RenderResult r = l.getKey().renderLayer(gl, size, this, l.getValue().get());
				}
				catch(ExecutionException|InterruptedException _e)
				{
					_e.printStackTrace();
				}

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glPushMatrix();
			gl.glMatrixMode(GL2.GL_MODELVIEW);

			Quaternion3d rotation = new Quaternion3d(this.rotation.getAngle(), this.rotation.getRotationAxis().negateY());
			Matrix4d transformation = rotation.toMatrix().translated(-translation.x, translation.y, -translation.z);
			gl.glMultMatrixd(transformation.m, 0);

			GLU glu = new GLU();
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glScaled(aspect, aspect, 1);

			if (CameraMode.mode == CameraMode.MODE.MODE_3D)
			{
 				glu.gluPerspective(MainPanel.FOV, this.aspect, clipNear, this.translation.z + 4 * Constants.SUN_RADIUS);
			}
			else
			{
				double width = Math.tan(Math.toRadians(FOV) / 2) * translation.z;
				gl.glOrtho(-width, width, -width, width, clipNear, this.translation.z + 4 * Constants.SUN_RADIUS);
				gl.glScalef((float) (1 / aspect), 1, 1);
			}

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			calculateBounds();
			for (CameraInteraction cameraInteraction : cameraInteractions)
				cameraInteraction.renderInteraction(gl);

			renderPlugins(gl);
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glMatrixMode(GL2.GL_MODELVIEW);

			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();
			double xScale = aspect > 1 ? 1 / aspect : 1;
			double yScale = aspect < 1 ? aspect : 1;
			gl.glScaled(xScale, yScale, 1);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			boolean loading = UltimateDownloadManager.areDownloadsActive();
			if (loading)
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
		}

		// empty screen
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);

		while (!cameraAnimations.isEmpty() && cameraAnimations.get(0).isFinished())
			cameraAnimations.remove(0);
		if (!cameraAnimations.isEmpty())
			cameraAnimations.get(0).animate(this);
		
		for (MainPanel compenentView : synchronizedViews)
			compenentView.repaint();
		
		RepaintManager.currentManager(MainFrame.SINGLETON).paintDirtyRegions();
		
		if (TimeLine.SINGLETON.isPlaying())
			repaint();
	}

	protected void renderPlugins(GL2 gl) {
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LESS);
		gl.glDepthMask(false);
		Plugins.SINGLETON.renderPlugin(gl,
				RENDER_MODE.MAIN_PANEL);
		gl.glDepthMask(false);
	}

	@Override
	public void display(GLAutoDrawable drawable)
	{
		size = getCanavasSize();
		
		if(TimeLine.SINGLETON.isPlaying())
			size = new Dimension((int)(size.width/resolutionDivisor), (int)(size.height/resolutionDivisor));
		
		GL2 gl = drawable.getGL().getGL2();

		gl.glViewport(0, 0, this.getSurfaceWidth(), this.getSurfaceHeight());

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glScaled(1, aspect, 1);
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		render(gl);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glLoadIdentity();
		// gl.glScaled(1, aspect, 1);
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glUseProgram(0);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glDisable(GL2.GL_DEPTH_TEST);

		boolean noImageScreen = true;
		for (AbstractLayer layer : Layers.getLayers()){
			noImageScreen &= !layer.isVisible();
		}
		if (noImageScreen) {
			double dim = Math.max(getSurfaceHeight(), getSurfaceWidth()) * 0.15;

			int xOffset = (int) (getSurfaceWidth() / 2 - dim / 2);
			int width = (int) (dim);
			int yOffset = (int) (getSurfaceHeight() / 2 - dim / 2);
			int height = (int) (dim);
			gl.glViewport(xOffset, yOffset, width, height);
			NoImageScreen.render(gl);
			gl.glViewport(0, 0, getSurfaceWidth(), getSurfaceHeight());
		}

		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glUseProgram(0);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);

		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);

		/*
		 * if (!JHVGlobals.isReleaseVersion()) { gl.glEnable(GL2.GL_TEXTURE_2D);
		 * gl.glColor4d(1, 1, 1, 1); gl.glEnable(GL2.GL_DEPTH_TEST);
		 * 
		 * int tmp[] = new int[1]; gl.glGenTextures(1, tmp, 0);
		 * gl.glBindTexture(GL2.GL_TEXTURE_2D, tmp[0]);
		 * gl.glCopyTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT24,
		 * 0, 0, this.getSurfaceWidth(), this.getSurfaceHeight(), 0);
		 * 
		 * gl.glDisable(GL2.GL_DEPTH_TEST);
		 * 
		 * gl.glDisable(GL2.GL_BLEND); gl.glTexParameteri(GL2.GL_TEXTURE_2D,
		 * GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		 * gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
		 * GL2.GL_LINEAR);
		 * 
		 * gl.glDepthMask(true);
		 * 
		 * gl.glDisable(GL2.GL_BLEND);
		 * 
		 * 
		 * gl.glMatrixMode(GL2.GL_PROJECTION); gl.glLoadIdentity();
		 * gl.glPushMatrix(); gl.glOrtho(0, 1, 0, 1, 10, -10); gl.glViewport(0,
		 * 0, (int) (this.getSurfaceWidth() * 0.3), (int)
		 * (this.getSurfaceHeight() * 0.3)); gl.glMatrixMode(GL2.GL_MODELVIEW);
		 * gl.glLoadIdentity(); gl.glBegin(GL2.GL_QUADS); gl.glTexCoord2f(0, 0);
		 * gl.glVertex2d(0, 1); gl.glTexCoord2f(1, 0); gl.glVertex2d(1, 1);
		 * gl.glTexCoord2f(1, 1); gl.glVertex2d(1, 0); gl.glTexCoord2f(0, 1);
		 * gl.glVertex2d(0, 0); gl.glEnd(); gl.glBindTexture(GL2.GL_TEXTURE_2D,
		 * 0); gl.glDisable(GL2.GL_TEXTURE_2D);
		 * gl.glMatrixMode(GL2.GL_PROJECTION); gl.glPopMatrix();
		 * gl.glMatrixMode(GL2.GL_MODELVIEW); }
		 */
	}

	protected void updateTrackRotation() {
		if (lastDate == null) lastDate = TimeLine.SINGLETON.getCurrentDateTime();
		if (!lastDate.isEqual(TimeLine.SINGLETON.getCurrentDateTime())) {
			Duration difference = Duration.between(lastDate,
					TimeLine.SINGLETON.getCurrentDateTime());

			long seconds = difference.getSeconds();
			lastDate = TimeLine.SINGLETON.getCurrentDateTime();
			RayTrace rayTrace = new RayTrace();
			Vector3d hitPoint = rayTrace.cast(getWidth() / 2, getHeight() / 2,
					this).getHitpoint();
			HeliocentricCartesianCoordinate cart = new HeliocentricCartesianCoordinate(
					hitPoint.x, hitPoint.y, hitPoint.z);
			HeliographicCoordinate newCoord = cart.toHeliographicCoordinate();
			double angle = DifferentialRotation.calculateRotationInRadians(
					newCoord.latitude, seconds);

			Quaternion3d rotation = Quaternion3d.createRotation(angle,
					new Vector3d(0, 1, 0));

			rotation = rotation.rotate(this.rotation);
			if (CameraMode.mode == MODE.MODE_3D)
				this.rotation = rotation;
			else {
				Vector3d trans = rotation.toMatrix().multiply(hitPoint);
				this.translation = new Vector3d(trans.x, trans.y, translation.z);
			}
		}
	}

	protected void calculateBounds() {
		RayTrace rayTrace = new RayTrace();
		int width = this.getWidth() / 9;
		int height = this.getHeight() / 9;
		for (int i = 0; i < 40; i++) {
			if (i < 10) {
				Vector3d hitpoint = rayTrace.cast(i * width, 0, this)
						.getHitpoint();
				visibleAreaOutline[i][0] = hitpoint.x;
				visibleAreaOutline[i][1] = hitpoint.y;
				visibleAreaOutline[i][2] = hitpoint.z;
			} else if (i < 20) {
				Vector3d hitpoint = rayTrace.cast(this.getWidth(),
						(i - 10) * height, this).getHitpoint();
				visibleAreaOutline[i][0] = hitpoint.x;
				visibleAreaOutline[i][1] = hitpoint.y;
				visibleAreaOutline[i][2] = hitpoint.z;
			} else if (i < 30) {
				Vector3d hitpoint = rayTrace.cast((29 - i) * width,
						getHeight(), this).getHitpoint();
				visibleAreaOutline[i][0] = hitpoint.x;
				visibleAreaOutline[i][1] = hitpoint.y;
				visibleAreaOutline[i][2] = hitpoint.z;
			} else if (i < 40) {
				Vector3d hitpoint = rayTrace.cast(0, (39 - i) * height, this)
						.getHitpoint();
				visibleAreaOutline[i][0] = hitpoint.x;
				visibleAreaOutline[i][1] = hitpoint.y;
				visibleAreaOutline[i][2] = hitpoint.z;
			}
		}
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		

	}

	@Override
	public void init(GLAutoDrawable drawable) {
		//if (System.getProperty("jhvVersion") == null)
		//	drawable.setGL(new DebugGL2(drawable.getGL().getGL2()));
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

		//this.initShaders(gl);

		gl.glDisable(GL2.GL_TEXTURE_2D);

	}

	private void generateNewRenderBuffers(GL2 gl, int width, int height)
	{
		if (renderBufferDepth != null)
		{
			gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
		}
		renderBufferDepth = new int[1];
		gl.glGenRenderbuffers(1, renderBufferDepth, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferDepth[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, width, height);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER,
				renderBufferDepth[0]);

		if (renderBufferColor != null)
		{
			gl.glDeleteRenderbuffers(1, renderBufferColor, 0);
		}
		renderBufferColor = new int[1];
		gl.glGenRenderbuffers(1, renderBufferColor, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferColor[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, width, height);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER,
				renderBufferColor[0]);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		aspect = this.getSize().getWidth() / this.getSize().getHeight();
		repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		for (CameraInteraction cameraInteraction : cameraInteractions)
			cameraInteraction.mouseDragged(e);
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		RayTrace rayTrace = new RayTrace();
		Ray ray = rayTrace.cast(e.getX(), e.getY(), this);
		for (StatusLabelMouseListener statusLabel : statusLabelsMouseListeners)
			statusLabel.mouseMoved(e, ray);
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		for (CameraInteraction cameraInteraction : cameraInteractions)
			cameraInteraction.mousePressed(e);
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		for (CameraInteraction cameraInteraction : cameraInteractions)
			cameraInteraction.mouseReleased(e);
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		RayTrace rayTrace = new RayTrace();
		Ray ray = rayTrace.cast(e.getX(), e.getY(), this);
		for (StatusLabelMouseListener statusLabel : statusLabelsMouseListeners)
			statusLabel.mouseExited(e, ray);
	}

	public Dimension getCanavasSize() {
		return new Dimension(this.getSurfaceWidth(), this.getSurfaceHeight());
	}

	public BufferedImage getBufferedImage(int imageWidth, int imageHeight, boolean textEnabled)
	{
		ArrayList<String> descriptions = null;
		if (textEnabled)
		{
			descriptions = new ArrayList<String>();
			for (AbstractLayer layer : Layers.getLayers())
				if (layer.isVisible())
					descriptions.add(layer.getFullName() + " - " + layer.getTime().format(JHVGlobals.DATE_TIME_FORMATTER));
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
		profile = GLProfile.getDefault();
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
		double top = Math.tan(MainPanel.FOV / 360.0 * Math.PI) * MainPanel.CLIP_NEAR;
		double right = top * aspect;
		double left = -right;
		double bottom = -top;

		TextRenderer textRenderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 24));
		textRenderer.setColor(1f, 1f, 1f, 1f);

		offscreenGL.glViewport(0, 0, tileWidth, tileHeight);
		this.size = new Dimension(tileWidth, tileHeight);

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
				render(offscreenGL);
				
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

				offscreenGL.glReadPixels(0, 0, cutOffX, cutOffY, GL2.GL_BGR,
						GL2.GL_UNSIGNED_BYTE, ByteBuffer
								.wrap(((DataBufferByte) screenshot.getRaster()
										.getDataBuffer()).getData()));
				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glPopMatrix();
				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);
			}
		}

		ImageUtil.flipImageVertically(screenshot);
		return screenshot;
	}

	@Override
	public void newlayerAdded()
	{
		this.repaint();
	}

	@Override
	public void newlayerRemoved(int idx)
	{
		this.repaint();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		for (CameraInteraction cameraInteraction : cameraInteractions)
			cameraInteraction.mouseWheelMoved(e);
	}

	@Override
	public void activeLayerChanged(AbstractLayer layer)
	{
		if (Layers.getActiveImageLayer() != null)
			lastDate = null;
		repaint();
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
		//this.repaint();
	}

	public double getAspect()
	{
		return this.aspect;
	}

	public void addSynchronizedView(MainPanel compenentView)
	{
		this.synchronizedViews.add(compenentView);
	}

	@Override
	public void dateTimesChanged(int framecount)
	{
	}

	public double[][] getVisibleAreaOutline()
	{
		return visibleAreaOutline;
	}

	public void addCameraAnimation(CameraAnimation cameraAnimation)
	{
		this.cameraAnimations.add(cameraAnimation);
		this.repaint();
	}

	public void resetCamera()
	{
		Quaternion3d rotation = Quaternion3d.createRotation(0.0, new Vector3d(0, 1, 0));
		Vector3d translation = new Vector3d(0, 0, DEFAULT_CAMERA_DISTANCE);
		addCameraAnimation(new CameraTransformationAnimation(rotation, translation, this));
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
					public void keyPressed(KeyEvent e) {
						if (e.getKeyCode() == KeyEvent.VK_ESCAPE
								|| (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_T)) {
							MainPanel.this.removeKeyListener(this);
							fullscreenFrame.getContentPane().remove(
									MainPanel.this);
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
		this.cameraTrackingEnabled = !cameraTrackingEnabled;
		this.lastDate = TimeLine.SINGLETON.getCurrentDateTime();
	}

	public void addStatusLabelMouseListener(StatusLabelMouseListener statusLabelMouse)
	{
		statusLabelsMouseListeners.add(statusLabelMouse);
	}

	public void addStatusLabelCameraListener(StatusLabelCameraListener statusLabelCamera)
	{
		statusLabelCameraListeners.add(statusLabelCamera);
	}

	
	public void resetLastFrameChangeTime()
	{
		lastFrameChangeTime = System.currentTimeMillis();
	}
}
