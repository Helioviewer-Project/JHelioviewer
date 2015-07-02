package org.helioviewer.jhv.viewmodel.view.opengl;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.GL3DKeyController;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.View2DAction;
import org.helioviewer.jhv.gui.controller.Camera;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DCameraListener;
import org.helioviewer.jhv.opengl.camera.GL3DTrackballCamera;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.jhv.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.jhv.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.jhv.viewmodel.view.AbstractBasicView;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderHelper;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderView;
import org.helioviewer.jhv.viewmodel.viewport.StaticViewport;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;

import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * The top-most View in the 3D View Chain. Let's the viewchain render to its
 * {@link GLCanvas}.
 * 
 * 
 * @author Simon Sp���������rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DComponentView extends AbstractBasicView implements
		GLEventListener, LayersListener, GL3DCameraListener {

	private Timer postRenderTimer = new Timer(100, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			canvas.repaint();
		}
	});
	private Timer animationTimer = new Timer(0, new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			canvas.repaint();
		}
	});

	private long animationTime = 0;
	private long startedTime = 0;
	private CopyOnWriteArrayList<ScreenRenderer> postRenderers = new CopyOnWriteArrayList<ScreenRenderer>();

	private GLCanvas canvas;
	private GLCanvas inactiveCanvas;

	private Color backgroundColor = Color.BLACK;

	private boolean backGroundColorHasChanged = false;

	private boolean rebuildShadersRequest = false;

	private GLTextureHelper textureHelper = new GLTextureHelper();
	private GLShaderHelper shaderHelper = new GLShaderHelper();

	private ViewportView viewportView;

	private ReentrantLock animationLock = new ReentrantLock();

	private Vector2i viewportSize;
	private int[] frameBufferObject;
	private int[] renderBufferDepth;
	private int[] renderBufferColor;
	private static int DEFAULT_TILE_WIDTH = 2048;
	private static int DEFAULT_TILE_HEIGHT = 2048;
	private int tileWidth = 512;
	private int tileHeight = 512;

	private Viewport defaultViewport;

	private double clipNear = Constants.SUN_RADIUS / 10;
	private double clipFar = Constants.SUN_RADIUS * 1000;
	private double fov = 10;
	private double aspect = 0.0;
	private double width = 0.0;
	private double height = 0.0;
	public boolean exportMovie = false;
	private JFrame fullScreenFrame;
	public boolean fullScreenMode = false;

	public GL3DComponentView() {
		GLCapabilities cap = new GLCapabilities(GLProfile.getDefault());
		try {
			cap.setDepthBits(24);
			this.canvas = new GLCanvas(cap);
		} catch (Exception e) {
			try {
				System.err
						.println("Unable to load 24-bit z-buffer, try 32-bit");
				cap.setDepthBits(32);
				this.canvas = new GLCanvas(cap);
			} catch (Exception e2) {
				System.err
						.println("Unable to load 32-bit z-buffer, try 16-bit");
				try {
					cap.setDepthBits(16);
					this.canvas = new GLCanvas(cap);
				} catch (Exception e3) {
					System.err
							.println("Unable to load 16-bit z-buffer, use default");
					this.canvas = new GLCanvas();
				}
			}
		}
		this.canvas.setSharedContext(OpenGLHelper.glContext);
		this.canvas.addGLEventListener(this);
		this.canvas.addKeyListener(GL3DKeyController.getInstance());
		this.canvas.requestFocus();

		LayersModel.getSingletonInstance().addLayersListener(this);
		Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

			@Override
			public void eventDispatched(AWTEvent event) {
				if (event instanceof KeyEvent) {
					KeyEvent k = (KeyEvent) event;
						Window activeWindow = null;
						for (Window window : Window.getWindows()){
							if (window.isActive()){
								activeWindow = window;
							}
						}
						if (activeWindow instanceof JFrame){
								
					if (k.getID() == KeyEvent.KEY_PRESSED) {
						JHVJPXView activeView;
						switch (k.getKeyCode()) {
						case KeyEvent.VK_SPACE:
							if (LinkedMovieManager.getActiveInstance().getMasterMovie() != null){
							if (LinkedMovieManager.getActiveInstance()
									.getMasterMovie().isMoviePlaying())
								LinkedMovieManager.getActiveInstance()
										.getMasterMovie().pauseMovie();
							else
								LinkedMovieManager.getActiveInstance()
										.getMasterMovie().playMovie();
							}
							break;
						case KeyEvent.VK_RIGHT:
							if (LayersModel.getSingletonInstance().getActiveView() != null){
							activeView = (JHVJPXView) LayersModel
									.getSingletonInstance().getActiveView()
									.getAdapter(JHVJPXView.class);
							activeView.setCurrentFrame(
									activeView.getCurrentFrameNumber() + 1,
									new ChangeEvent());
							}
							break;
						case KeyEvent.VK_LEFT:
							if (LayersModel.getSingletonInstance().getActiveView() != null){
								activeView = (JHVJPXView) LayersModel
									.getSingletonInstance().getActiveView()
									.getAdapter(JHVJPXView.class);
							activeView.setCurrentFrame(
									activeView.getCurrentFrameNumber() - 1,
									new ChangeEvent());
							}
							break;
						default:
							break;
						}

					}
				}}
			}
		}, AWTEvent.KEY_EVENT_MASK);
	}

	public GLCanvas getComponent() {
		return this.canvas;
	}

	public void init(GLAutoDrawable glAD) {
		System.out.println("V : " + glAD.getContext().getGLVersion());
		System.out.println("V : " + glAD.getContext().getGLSLVersionString());
		GuiState3DWCS.overViewPanel.activate();
		System.out.println("GL3DComponentView.Init");
		GL2 gl = glAD.getGL().getGL2();
		GL3DState.create(gl);
		Viewport newViewport = StaticViewport.createAdaptedViewport(
				canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
		this.getAdapter(ViewportView.class).setViewport(newViewport,
				new ChangeEvent());

		try {
			GLShaderHelper.initHelper(gl.getGL2());
		} catch (IOException _ioe) {
			throw new RuntimeException(_ioe);
		}
		GLShaderBuilder.initShaderBuilder(gl);

		this.getAdapter(GL3DCameraView.class).getCurrentCamera()
				.addCameraListener(this);

		frameBufferObject = new int[1];
		gl.glGenFramebuffers(1, frameBufferObject, 0);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
		generateNewRenderBuffers(gl);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

		textureHelper.delAllTextures(gl);
		GLTextureHelper.initHelper(gl);

		shaderHelper.delAllShaderIDs(gl);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
		gl.glShadeModel(GL2.GL_SMOOTH);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE,
				GL2.GL_MODULATE);
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_POINT_SMOOTH);
		gl.glEnable(GL2.GL_COLOR_MATERIAL);

		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_NORMALIZE);
		gl.glCullFace(GL2.GL_BACK);
		gl.glFrontFace(GL2.GL_CCW);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);

		gl.glEnable(GL2.GL_LIGHT0);

		viewportSize = new Vector2i(0, 0);
		this.rebuildShadersRequest = true;
		
		if(Settings.getProperty("startup.cameramode").equals("2D"))
    		SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    new View2DAction().actionPerformed(null);
                }
            });
	}

	public void reshape(GLAutoDrawable glAD, int x, int y, int width, int height) {
		viewportSize = new Vector2i(canvas.getSurfaceWidth(),
				canvas.getSurfaceHeight());
		Viewport newViewport = StaticViewport.createAdaptedViewport(
				canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
		this.getAdapter(ViewportView.class).setViewport(newViewport,
				new ChangeEvent());
		// Log.debug("GL3DComponentView.Reshape");
		GL gl = glAD.getGL();

		gl.setSwapInterval(1);

		GuiState3DWCS.mainComponentView.getComponent().repaint();
		GuiState3DWCS.overViewPanel.repaint();
	}

	public void display(GLAutoDrawable glAD) {
		long time = System.currentTimeMillis();
		if (animationTimer.isRunning()
				&& animationTime - (time - startedTime) <= 0
				&& LinkedMovieManager.getActiveInstance() != null
				&& !LinkedMovieManager.getActiveInstance().isPlaying()) {
			animationTimer.stop();
		} else {
			animationTime -= (time - startedTime);
			this.startedTime = time;
		}
		GL2 gl = glAD.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); // clear
																		// color
																		// and
																		// depth
																		// buffers
		gl.glLoadIdentity(); // reset the model-view matrix

		if (defaultViewport != null) {
		}

		int width = this.viewportSize.getX();
		int height = this.viewportSize.getY();
		GL3DState.getUpdated(gl, width, height);

		if (backGroundColorHasChanged) {
			gl.glClearColor(backgroundColor.getRed() / 255.0f,
					backgroundColor.getGreen() / 255.0f,
					backgroundColor.getBlue() / 255.0f,
					backgroundColor.getAlpha() / 255.0f);

			backGroundColorHasChanged = false;
		}

		// Rebuild all shaders, if necessary
		if (rebuildShadersRequest) {
			rebuildShaders(gl);
		}

		GL3DState.get().checkGLErrors("GL3DComponentView.afterRebuildShader");

		// Save Screenshot, if requested

		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);

		gl.glClearColor(backgroundColor.getRed() / 255.0f,
				backgroundColor.getGreen() / 255.0f,
				backgroundColor.getBlue() / 255.0f,
				backgroundColor.getAlpha() / 255.0f);

		Viewport v = this.getAdapter(ViewportView.class).getViewport();

		this.width = v.getWidth();
		this.height = v.getHeight();

		this.aspect = this.width / this.height;
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();

		double fH = Math.tan(this.fov / 360.0 * Math.PI) * clipNear;
		double fW = fH * aspect;
		gl.glViewport(0, 0, canvas.getSurfaceWidth(), canvas.getSurfaceHeight());

		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
			gl.glFrustum(-fW, fW, -fH, fH, clipNear, clipFar);

		else {
			PhysicalRegion region = this.getAdapter(RegionView.class)
					.getLastDecodedRegion();
			if (region != null) {
				MetaData metaData = null;
				double distance = GL3DTrackballCamera.DEFAULT_CAMERA_DISTANCE;
				GL3DCamera camera = this.getAdapter(GL3DCameraView.class)
						.getCurrentCamera();
				if (LayersModel.getSingletonInstance().getActiveView() != null
						&& camera != null) {
					metaData = LayersModel.getSingletonInstance()
							.getActiveView().getAdapter(MetaDataView.class)
							.getMetaData();
					region = metaData.getPhysicalRegion();

					double halfWidth = region.getWidth() / 2;
					double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
					distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad)
							/ Math.sin(halfFOVRad);

					double scaleFactor = -camera.getZTranslation() / distance;
					double top = region.getCornerX();
					double bottom = region.getCornerX() + region.getWidth();
					gl.glOrtho(top * aspect * scaleFactor, bottom * aspect
							* scaleFactor, top * scaleFactor, bottom
							* scaleFactor, clipNear, clipFar);
				}
			}
		}
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		displayBody(gl);

	}

	private void displayBody(GL2 gl) {

		this.viewportSize.getX();
		int height = this.viewportSize.getY();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glColor4f(1, 1, 1, 1);
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL2.GL_DEPTH_TEST);

		gl.glLoadIdentity();

		gl.glPushMatrix();
		if (this.getView() instanceof GLView) {
			((GLView) this.getView()).renderGL(gl, true);
		}

		GL3DState.get().checkGLErrors("GL3DComponentView.afterRenderGL");

		gl.glPopMatrix();

		gl.glPushMatrix();
		if (!this.postRenderers.isEmpty()) {

			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();

			gl.glOrtho(0, canvas.getSurfaceWidth(), 0,
					canvas.getSurfaceHeight(), -1, 10000);

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glTranslatef(0.0f, height, 0.0f);
			gl.glScalef(1.0f, -1.0f, 1.0f);
			gl.glDisable(GL2.GL_LIGHTING);
			gl.glColor4f(1, 1, 1, 0);
			gl.glDisable(GL2.GL_DEPTH_TEST);
			gl.glEnable(GL2.GL_TEXTURE_2D);
			GLScreenRenderGraphics glRenderer = new GLScreenRenderGraphics(gl);

			// Iterator<> postRenderer = postRenderers
			for (ScreenRenderer r : postRenderers) {
				r.setContainerSize(canvas.getSurfaceWidth(),
						canvas.getSurfaceHeight());
				r.render(glRenderer);
			}

			gl.glDisable(GL2.GL_TEXTURE_2D);

		}
		gl.glPopMatrix();
		GL3DState.get().checkGLErrors("GL3DComponentView.afterPostRenderers");
	}

	private void generateNewRenderBuffers(GL gl) {
		// tileWidth = defaultTileWidth;
		// tileHeight = defaultTileHeight;
		if (renderBufferDepth != null) {
			gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
		}
		renderBufferDepth = new int[1];
		gl.glGenRenderbuffers(1, renderBufferDepth, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferDepth[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT,
				tileWidth, tileHeight);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER,
				renderBufferDepth[0]);

		if (renderBufferColor != null) {
			gl.glDeleteRenderbuffers(1, renderBufferColor, 0);
		}
		renderBufferColor = new int[1];
		gl.glGenRenderbuffers(1, renderBufferColor, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferColor[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, tileWidth,
				tileHeight);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER,
				renderBufferColor[0]);
	}

	public void saveScreenshot(String imageFormat, File outputFile, int width,
			int height, ArrayList<String> descriptions) {
		try {
			ImageIO.write(this.getBufferedImage(width, height, descriptions),
					imageFormat, outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setBackgroundColor(Color background) {
		backgroundColor = background;
		backGroundColorHasChanged = true;
	}

	public BufferedImage getBufferedImage(int width, int height,
			ArrayList<String> descriptions) {
		this.canvas.repaint();
		this.exportMovie = true;
		defaultViewport = this.getAdapter(ViewportView.class).getViewport();

		tileWidth = width < DEFAULT_TILE_WIDTH ? width : DEFAULT_TILE_WIDTH;
		tileHeight = height < DEFAULT_TILE_HEIGHT ? height
				: DEFAULT_TILE_HEIGHT;

		Viewport viewport = StaticViewport.createAdaptedViewport(tileWidth,
				tileHeight);
		this.getAdapter(ViewportView.class).setViewport(viewport,
				new ChangeEvent());
		this.canvas.repaint();

		System.out
				.println(">> GLComponentView.display() > Start taking screenshot");
		double xTiles = width / (double) tileWidth;
		double yTiles = height / (double) tileHeight;
		int countXTiles = width % tileWidth == 0 ? (int) xTiles
				: (int) xTiles + 1;
		int countYTiles = height % tileHeight == 0 ? (int) yTiles
				: (int) yTiles + 1;

		GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile
				.getDefault());
		GLProfile profile = GLProfile.get(GLProfile.GL2);
		profile = GLProfile.getDefault();
		GLCapabilities capabilities = new GLCapabilities(profile);
		capabilities.setDoubleBuffered(false);
		capabilities.setOnscreen(false);
		capabilities.setHardwareAccelerated(true);
		capabilities.setFBO(true);

		GLDrawable offscreenDrawable = factory.createOffscreenDrawable(null,
				capabilities, null, tileWidth, tileHeight);

		offscreenDrawable.setRealized(true);
		GLContext offscreenContext = canvas.getContext();
		offscreenDrawable.setRealized(true);
		offscreenContext.makeCurrent();
		GL2 offscreenGL = offscreenContext.getGL().getGL2();

		offscreenGL.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
		generateNewRenderBuffers(offscreenGL);

		BufferedImage screenshot = new BufferedImage(width, height,
				BufferedImage.TYPE_3BYTE_BGR);
		ByteBuffer.wrap(((DataBufferByte) screenshot.getRaster()
				.getDataBuffer()).getData());

		offscreenGL.glViewport(0, 0, tileWidth, tileHeight);

		double aspect = width / (double) height;
		double top = Math.tan(this.fov / 360.0 * Math.PI) * clipNear;
		double right = top * aspect;
		double left = -right;
		double bottom = -top;

		offscreenGL.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		double tileLeft, tileRight, tileBottom, tileTop;
		TextRenderer textRenderer = new TextRenderer(new Font("SansSerif",
				Font.BOLD, 24));
		textRenderer.setColor(1f, 1f, 1f, 1f);
		for (int x = 0; x < countXTiles; x++) {
			for (int y = 0; y < countYTiles; y++) {
				tileLeft = left + (right - left) / xTiles * x;
				tileRight = left + (right - left) / xTiles * (x + 1);
				tileBottom = bottom + (top - bottom) / yTiles * y;
				tileTop = bottom + (top - bottom) / yTiles * (y + 1);

				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glViewport(0, 0, tileWidth, tileHeight);
				offscreenGL.glLoadIdentity();
				offscreenGL.glPushMatrix();

				offscreenGL.glViewport(0, 0, tileWidth, tileHeight);
				offscreenGL.glFrustum(tileLeft, tileRight, tileBottom, tileTop,
						clipNear, clipFar);

				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);
				offscreenGL.glLoadIdentity();

				// double factor =
				int destX = tileWidth * x;
				int destY = tileHeight * y;

				GL3DState.get().checkGLErrors(
						"GL3DComponentView.beforeTileRenderer");
				GL3DCameraSelectorModel.getInstance().getCurrentCamera().updateCameraTransformation();
				this.canvas.repaint();
				displayBody(offscreenGL);

				if (descriptions != null && x == 0 && y == 0) {
					int counter = 0;
					textRenderer.beginRendering(canvas.getSurfaceWidth(),
							canvas.getSurfaceHeight());
					for (String description : descriptions) {
						textRenderer.draw(description, 5, 5 + 40 * counter++);
					}
					textRenderer.endRendering();
				}
				offscreenGL.glPixelStorei(GL2.GL_PACK_ROW_LENGTH, width);
				offscreenGL.glPixelStorei(GL2.GL_PACK_SKIP_ROWS, destY);
				offscreenGL.glPixelStorei(GL2.GL_PACK_SKIP_PIXELS, destX);
				offscreenGL.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);

				int cutOffX = width >= (x + 1) * tileWidth ? tileWidth
						: viewport.getWidth() - x * tileWidth;
				int cutOffY = height >= (y + 1) * tileHeight ? tileHeight
						: viewport.getHeight() - y * tileHeight;

				offscreenGL.glReadPixels(0, 0, cutOffX, cutOffY, GL2.GL_BGR,
						GL2.GL_UNSIGNED_BYTE, ByteBuffer
								.wrap(((DataBufferByte) screenshot.getRaster()
										.getDataBuffer()).getData()));

				GL3DState.get().checkGLErrors(
						"GL3DComponentView.afterTileRenderer");

			}
		}

		ImageUtil.flipImageVertically(screenshot);
		exportMovie = false;
		Viewport newViewport = StaticViewport.createAdaptedViewport(
				canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
		this.getAdapter(ViewportView.class).setViewport(newViewport,
				new ChangeEvent());
		this.canvas.repaint();
		viewport = null;
		return screenshot;
	}

	public void updateMainImagePanelSize(Vector2i size) {
		this.viewportSize = size;

		if (this.viewportView != null) {
			Viewport viewport = StaticViewport.createAdaptedViewport(
					Math.max(1, size.getX()), Math.max(1, size.getY()));
			this.viewportView.setViewport(viewport, null);
		}
	}

	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		this.viewportView = getAdapter(ViewportView.class);
	}

	public void viewChanged(View sender, ChangeEvent aEvent) {
		// this.saveBufferedImage = true;

		if (this.animationLock.isLocked()) {
			return;
		}

		// rebuild shaders, if necessary
		if (aEvent.reasonOccurred(ViewChainChangedReason.class)
				|| (aEvent.reasonOccurred(LayerChangedReason.class) && aEvent
						.getLastChangedReasonByType(LayerChangedReason.class)
						.getLayerChangeType() == LayerChangeType.LAYER_ADDED)) {
			rebuildShadersRequest = true;
		}
		notifyViewListeners(aEvent);
	}

	private void rebuildShaders(GL2 gl) {
		rebuildShadersRequest = false;
		shaderHelper.delAllShaderIDs(gl);

		GLFragmentShaderView fragmentView = view
				.getAdapter(GLFragmentShaderView.class);
		if (fragmentView != null) {
			// create new shader builder
			GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl.getGL2(),
					GL2.GL_FRAGMENT_PROGRAM_ARB);

			// fill with standard values
			GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
			minimalProgram.build(newShaderBuilder);

			// fill with other filters and compile
			fragmentView.buildFragmentShader(newShaderBuilder).compile();
		}

		GLVertexShaderView vertexView = view
				.getAdapter(GLVertexShaderView.class);
		if (vertexView != null) {
			// create new shader builder
			GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl.getGL2(),
					GL2.GL_VERTEX_PROGRAM_ARB);

			// fill with standard values
			GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
			minimalProgram.build(newShaderBuilder);

			// fill with other filters and compile
			vertexView.buildVertexShader(newShaderBuilder).compile();
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();
		gl.glDeleteFramebuffers(1, frameBufferObject, 0);
		gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
		gl.glDeleteRenderbuffers(1, renderBufferColor, 0);

	}

	public Dimension getCanavasSize() {
		return new Dimension(canvas.getSurfaceWidth(),
				canvas.getSurfaceHeight());
	}

	@Override
	public void layerAdded(int idx) {
		this.canvas.repaint(15);
	}

	@Override
	public void layerRemoved(View oldView, int oldIdx) {
		this.canvas.repaint(15);
	}

	@Override
	public void layerChanged(int idx) {
		this.canvas.repaint(1500);
	}

	@Override
	public void activeLayerChanged(int idx) {
		this.canvas.repaint(15);
	}

	@Override
	public void viewportGeometryChanged() {
		// this.canvas.repaint();
	}

	@Override
	public void timestampChanged(int idx) {
		this.canvas.repaint(50);
	}

	@Override
	public void subImageDataChanged(int idx) {
		// this.canvas.repaint();
	}

	@Override
	public void layerDownloaded(int idx) {
		this.canvas.repaint(15);
	}

	@Override
	public void cameraMoved(GL3DCamera camera) {
		this.canvas.repaint(15);
	}

	@Override
	public void cameraMoving(GL3DCamera camera) {
		this.canvas.repaint(15);
	}

	/**
	 * {@inheritDoc}
	 */
	public void addPostRenderer(ScreenRenderer postRenderer) {
		if (postRenderer != null) {
			if (!containsPostRenderer(postRenderer)) {
				postRenderers.add(postRenderer);
				postRenderTimer.start();
				if (postRenderer instanceof ViewListener) {
					addViewListener((ViewListener) postRenderer);
				}
			}
		}
	}

	private boolean containsPostRenderer(ScreenRenderer postrenderer) {
		return postRenderers.contains(postrenderer);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removePostRenderer(ScreenRenderer postRenderer) {
		if (postRenderer != null) {
			postRenderers.remove(postRenderer);
			postRenderTimer.stop();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public CopyOnWriteArrayList<ScreenRenderer> getAllPostRenderer() {
		return postRenderers;
	}

	public void regristryAnimation(long time) {
		if (time < 0) {
			System.err.println("Not correct time : " + time
					+ ", must be higher then 0");
		} else if (!animationTimer.isRunning()) {
			if (!LinkedMovieManager.getActiveInstance().isPlaying()) {
				this.animationTimer.setDelay(0);
			}
			animationTime = time;
			startedTime = System.currentTimeMillis();
			animationTimer.start();
		} else if (animationTime < time) {
			animationTime = time;
		}
	}

	public void toFullscreen() {
		this.removeListeners(this.canvas);
		inactiveCanvas = this.canvas;
		GLCapabilities cap = new GLCapabilities(GLProfile.getDefault());
		this.fullScreenFrame = new JFrame();
		this.canvas = new GLCanvas(cap);
		canvas.setSharedContext(OpenGLHelper.glContext);
		fullScreenFrame.add(canvas);
		fullScreenFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
		fullScreenFrame.setUndecorated(true);
		fullScreenFrame.setResizable(false);

		fullScreenFrame.setVisible(true);
		fullScreenMode = true;
		ImageViewerGui.getMainFrame().setVisible(false);
		this.canvas.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e))
					escapeFullscreen();
			}
		});
		this.canvas.addKeyListener(new KeyAdapter() {

			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE
						|| (e.isAltDown() && e.getKeyCode() == KeyEvent.VK_T))
					escapeFullscreen();
			}
		});

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				addListeners(canvas);
				canvas.repaint();
			}
		});
	}

	public void escapeFullscreen() {
		if (inactiveCanvas != null) {
			removeListeners(canvas);
			fullScreenFrame.dispose();
			canvas = inactiveCanvas;
			inactiveCanvas = null;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					addListeners(canvas);
					canvas.repaint();
					ImageViewerGui.getMainFrame().setVisible(true);
				}
			});
			fullScreenMode = false;
		}
	}

	public void removeListeners(Component canvas) {
		this.canvas.removeGLEventListener(this);
		this.canvas.removeMouseListener(ImageViewerGui.getSingletonInstance()
				.getMainImagePanel().getInputController());
		this.canvas.removeMouseMotionListener(ImageViewerGui
				.getSingletonInstance().getMainImagePanel()
				.getInputController());
		this.canvas.removeMouseWheelListener(ImageViewerGui
				.getSingletonInstance().getMainImagePanel()
				.getInputController());
		this.canvas.removeKeyListener(GL3DKeyController.getInstance());
	}

	public void addListeners(Component canvas) {
		this.canvas.addGLEventListener(this);
		this.canvas.addMouseListener(ImageViewerGui.getSingletonInstance()
				.getMainImagePanel().getInputController());
		this.canvas.addMouseMotionListener(ImageViewerGui
				.getSingletonInstance().getMainImagePanel()
				.getInputController());
		this.canvas.addMouseWheelListener(ImageViewerGui.getSingletonInstance()
				.getMainImagePanel().getInputController());
		this.canvas.addKeyListener(GL3DKeyController.getInstance());
		this.canvas.requestFocus();
	}

	public void switchHighDPIMode() {
		viewportSize = new Vector2i(canvas.getSurfaceWidth(),
				canvas.getSurfaceHeight());
		Viewport newViewport = StaticViewport.createAdaptedViewport(
				canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
		this.getAdapter(ViewportView.class).setViewport(newViewport,
				new ChangeEvent());

		GuiState3DWCS.mainComponentView.getComponent().repaint();
		GuiState3DWCS.overViewPanel.repaint();
		LayersModel.getSingletonInstance().fireSubImageDataChanged(0);
	}
	
}
