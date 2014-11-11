package org.helioviewer.jhv.viewmodel.view.opengl;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
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
import javax.swing.Timer;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Vector2dInt;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
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
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.renderer.screen.GLScreenRenderGraphics;
import org.helioviewer.jhv.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.jhv.viewmodel.view.AbstractBasicView;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderHelper;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderView;
import org.helioviewer.jhv.viewmodel.viewport.StaticViewport;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;

import com.jogamp.opengl.util.awt.ImageUtil;

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

    private Timer postRenderTimer = new Timer(100, new ActionListener(){
	  @Override
	  public void actionPerformed(ActionEvent e)
	  {
	    canvas.repaint();
	  }
	});
	private Timer animationTimer = new Timer(0, new ActionListener()
	{
	  @Override
	  public void actionPerformed(ActionEvent e)
	  {
	    canvas.repaint();
	  }
	});

	private long animationTime = 0;
	private long startedTime = 0;
	private CopyOnWriteArrayList<ScreenRenderer> postRenderers = new CopyOnWriteArrayList<ScreenRenderer>();

	private GLCanvas canvas;

	private Color backgroundColor = Color.BLACK;

	private boolean backGroundColorHasChanged = false;

	private boolean rebuildShadersRequest = false;

	private GLTextureHelper textureHelper = new GLTextureHelper();
	private GLShaderHelper shaderHelper = new GLShaderHelper();

	private ViewportView viewportView;

	private ReentrantLock animationLock = new ReentrantLock();

	private Vector2dInt viewportSize;
	private int[] frameBufferObject;
	private int[] renderBufferDepth;
	private int[] renderBufferColor;
	private static int defaultTileWidth = 2048;
	private static int defaultTileHeight = 2048;
	private int tileWidth = 512;
	private int tileHeight = 512;

	private Viewport defaultViewport;

	private double clipNear = Constants.SUN_RADIUS / 10;
	private double clipFar = Constants.SUN_RADIUS * 1000;
	private double fov = 10;
	private double aspect = 0.0;
	private double width = 0.0;
	private double height = 0.0;

	public GL3DComponentView() {
		GLCapabilities cap = new GLCapabilities(GLProfile.getDefault());
		
		try {
			cap.setDepthBits(24);
			this.canvas = new GLCanvas(cap);
		} catch (Exception e) {
			try {
				Log.error("Unable to load 24-bit z-buffer, try 32-bit");
				cap.setDepthBits(32);
				this.canvas = new GLCanvas(cap);
			} catch (Exception e2) {
				Log.error("Unable to load 32-bit z-buffer, try 16-bit");
				try {
					cap.setDepthBits(16);
					this.canvas = new GLCanvas(cap);
				} catch (Exception e3) {
					Log.error("Unable to load 16-bit z-buffer, use default");
					this.canvas = new GLCanvas();
				}
			}
		}
		
		this.canvas.addGLEventListener(this);
		
		LayersModel.getSingletonInstance().addLayersListener(this);
	}

	public GLCanvas getComponent() {
		return this.canvas;
	}

	public void init(GLAutoDrawable glAD) {
		
		Log.debug("GL3DComponentView.Init");
		GL2 gl = glAD.getGL().getGL2();
		GL3DState.create(gl);

		GLShaderHelper.initHelper(gl.getGL2(), JHVDirectory.TEMP.getPath());
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
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_BLEND);
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

		viewportSize = new Vector2dInt(0, 0);
		this.rebuildShadersRequest = true;
	}

	public void reshape(GLAutoDrawable glAD, int x, int y, int width, int height) {
		viewportSize = new Vector2dInt(canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
		// Log.debug("GL3DComponentView.Reshape");
		GL gl = glAD.getGL();

		gl.setSwapInterval(1);
	}

	public void display(GLAutoDrawable glAD) {
		long time = System.currentTimeMillis();
		if (animationTimer.isRunning() && animationTime-(time-startedTime) <= 0 && LinkedMovieManager.getActiveInstance() != null && !LinkedMovieManager.getActiveInstance().isPlaying()){
			animationTimer.stop();
		}
		else {
			animationTime -= (time - startedTime);
			this.startedTime = time;
		}
		GL2 gl = glAD.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); // clear color and depth buffers
	    gl.glLoadIdentity();  // reset the model-view matrix
		
		Viewport newViewport = StaticViewport.createAdaptedViewport(canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
		this.getAdapter(ViewportView.class).setViewport(newViewport,
				new ChangeEvent());
		
		if (defaultViewport != null){
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
			Region region = this.getAdapter(RegionView.class).getRegion();
			if (region != null){
			MetaData metaData = null;
			double distance = GL3DTrackballCamera.DEFAULT_CAMERA_DISTANCE;
			GL3DCamera camera = this.getAdapter(GL3DCameraView.class).getCurrentCamera();
			if (LayersModel.getSingletonInstance().getActiveView() != null && camera != null){
				metaData = LayersModel.getSingletonInstance().getActiveView().getAdapter(MetaDataView.class).getMetaData();
				region = metaData.getPhysicalRegion();

				double halfWidth = region.getWidth() / 2;
                double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
                distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);

			double scaleFactor = -camera.getZTranslation() / distance;
			double top = region.getCornerX();
			double bottom = region.getCornerX()+region.getWidth();
	        gl.glOrtho(top*aspect*scaleFactor, bottom*aspect*scaleFactor, top*scaleFactor, bottom*scaleFactor, clipNear, clipFar);
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

			gl.glOrtho(0, canvas.getSurfaceWidth(), 0, canvas.getSurfaceHeight(), -1, 10000);

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
				r.setContainerSize(canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
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

	public void saveScreenshot(String imageFormat, File outputFile)
			throws IOException {
		ImageIO.write(this.getBufferedImage(), imageFormat, outputFile);
	}

	public void saveScreenshot(String imageFormat, File outputFile, int width,
			int height) {
		try {
			ImageIO.write(this.getBufferedImage(width, height), imageFormat,
					outputFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setBackgroundColor(Color background) {
		backgroundColor = background;
		backGroundColorHasChanged = true;
	}

	public BufferedImage getBufferedImage() {
		return getBufferedImage(this.canvas.getWidth(), this.canvas.getHeight());
	}

	public BufferedImage getBufferedImage(int width, int height) {
		defaultViewport = this.getAdapter(ViewportView.class).getViewport();
		Viewport viewport = StaticViewport.createAdaptedViewport(width, height);
		this.getAdapter(ViewportView.class).setViewport(viewport,
				new ChangeEvent());

		tileWidth = width < defaultTileWidth ? width : defaultTileWidth;
		tileHeight = height < defaultTileHeight ? height : defaultTileHeight;

		Log.trace(">> GLComponentView.display() > Start taking screenshot");
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
		// GLContext offscreenContext =
		// offscreenDrawable.createContext(this.canvas.getContext());
		offscreenDrawable.setRealized(true);
		offscreenContext.makeCurrent();
		GL2 offscreenGL = offscreenContext.getGL().getGL2();
		// GL2 offscreenGL = canvas.getContext().getGL().getGL2();

		offscreenGL.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
		generateNewRenderBuffers(offscreenGL);

		BufferedImage screenshot = new BufferedImage(viewport.getWidth(),
				viewport.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		ByteBuffer.wrap(((DataBufferByte) screenshot
				.getRaster().getDataBuffer()).getData());

		offscreenGL.glViewport(0, 0, tileWidth, tileHeight);

		double aspect = width / (double) height;
		double top = Math.tan(this.fov / 360.0 * Math.PI) * clipNear;
		double right = top * aspect;
		double left = -right;
		double bottom = -top;

		offscreenGL.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		double tileLeft, tileRight, tileBottom, tileTop;

		for (int x = 0; x < countXTiles; x++) {
			for (int y = 0; y < countYTiles; y++) {
				tileLeft = left + (right - left) / xTiles * x;
				tileRight = left + (right - left) / xTiles * (x + 1);
				tileBottom = bottom + (top - bottom) / yTiles * y;
				tileTop = bottom + (top - bottom) / yTiles * (y + 1);
				// offscreenGL.glFlush();

				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glLoadIdentity();
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

				displayBody(offscreenGL);

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

		this.canvas.repaint();
		viewport = null;
		return screenshot;
	}

	public void updateMainImagePanelSize(Vector2dInt size) {
		this.viewportSize = size;

		if (this.viewportView != null) {
			Viewport viewport = StaticViewport.createAdaptedViewport(
					Math.max(1, size.getX()), Math.max(1, size.getY()));
			this.viewportView.setViewport(viewport, null);
		}
	}

	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		// this.orthoView = getAdapter(GL3DOrthoView.class);
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
		return new Dimension(canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
	}

	public void stop() {
		defaultViewport = this.getAdapter(ViewportView.class).getViewport();

	}

	public void start() {
		this.getAdapter(ViewportView.class).setViewport(defaultViewport,
				new ChangeEvent());
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
    this.canvas.repaint(15);
	}

	@Override
	public void activeLayerChanged(int idx) {
    this.canvas.repaint(15);
	}

	@Override
	public void viewportGeometryChanged() {
		//this.canvas.repaint();
	}

	@Override
	public void timestampChanged(int idx) {
		this.canvas.repaint(15);
	}

	@Override
	public void subImageDataChanged() {
	  //this.canvas.repaint();
	}

	@Override
	public void layerDownloaded(int idx) {
		this.canvas.repaint(15);
	}

	@Override
	public void cameraMoved(GL3DCamera camera) {
		// this.canvas.repaint();
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
			Log.error("Not correct time : " + time + ", must be higher then 0");
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

}
