package org.helioviewer.jhv.viewmodel.view.opengl;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.layers.NewLayerListener;
import org.helioviewer.jhv.layers.filter.LUT;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.RenderAnimation;
import org.helioviewer.jhv.opengl.SplashScreen;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.newCamera.CameraListener;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.opengl.raytrace.RayTrace.Ray;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.renderer.screen.ScreenRenderer;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.View;

public class CompenentView extends GL3DComponentView implements
		GLEventListener, LayersListener, MouseListener, MouseMotionListener,
		MouseWheelListener, NewLayerListener, CameraListener {

	GLCanvas canvas = new GLCanvas();
	GLTextureHelper textureHelper = new GLTextureHelper();
	private Layers layers;
	private boolean updateTexture;
	private GL3DCamera camera;
	private int shaderprogram;
	private HashMap<String, Integer> lutMap;
	private int nextAvaibleLut = 0;
	private long lastTime;
	private int invertedLut = 0;
	public boolean exportMovie;
	private float z = 1;
	private Camera cameraNEW;

	private CopyOnWriteArrayList<RenderAnimation> animations;
	private SplashScreen splashScreen;

	public CompenentView() {
		GuiState3DWCS.layers.addNewLayerListener(this);
		lutMap = new HashMap<String, Integer>();
		this.canvas.setSharedContext(OpenGLHelper.glContext);
		this.canvas.addMouseListener(this);
		this.canvas.addMouseMotionListener(this);
		this.canvas.addGLEventListener(this);
		this.canvas.addMouseWheelListener(this);
		loadLutFromFile("/UltimateLookupTable.txt");
		this.cameraNEW = new Camera();
		this.cameraNEW.init(this.canvas);
		this.cameraNEW.addCameraListener(this);
		layers = GuiState3DWCS.layers;
	}

	private void loadLutFromFile(String lutTxtName) {
		String line = null;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				CompenentView.class.getResourceAsStream(lutTxtName), "UTF-8"))) {
			while ((line = br.readLine()) != null) {
				lutMap.put(line, this.nextAvaibleLut++);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int nextPowerOfTwo(int input) {
		int output = 1;
		while (output < input) {
			output <<= 1;
		}
		return output;
	}

	private void renderRect(GL2 gl) {
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_2D
				|| calculateAngleToActiveLayer() < 0.01) {
			gl.glDisable(GL2.GL_TEXTURE_2D);
			gl.glDisable(GL2.GL_BLEND);
			gl.glShadeModel(GL2.GL_FLAT);
			gl.glDisable(GL2.GL_TEXTURE_2D);
			gl.glColor3d(1, 1, 136.0 / 255.0);

			// lastLayer.getMetaData().getMaskRotation

			Vector3d translation = camera.getTranslation();
			double height = Math.tan(Math.toRadians(camera.getFOV()))
					* translation.z;
			double width = height * camera.getAspect();
			double x1 = width / 2.0;
			double y1 = height / 2.0;
			double x0 = -x1;
			double y0 = -y1;
			x0 -= translation.x;
			x1 -= translation.x;
			y0 -= translation.y;
			y1 -= translation.y;

			// Draw box
			gl.glBegin(GL2.GL_LINE_STRIP);
			gl.glVertex2d(x0, y0);
			gl.glVertex2d(x0, y1);
			gl.glVertex2d(x1, y1);
			gl.glVertex2d(x1, y0);
			gl.glVertex2d(x0, y0);
			gl.glEnd();

			double r = x1 - x0 > y1 - y0 ? (x1 - x0) * 0.02 : (y1 - y0) * 0.02;
			// Draw circle
			gl.glBegin(GL2.GL_LINE_LOOP);
			for (int i = 0; i < 360; i++) {
				double x = Math.sin(i * Math.PI * 2 / 360.) * r - translation.x;
				double y = Math.cos(i * Math.PI * 2 / 360.) * r - translation.y;
				gl.glVertex2d(x, y);
			}
			gl.glEnd();
		}

	}

	private double calculateAngleToActiveLayer() {
		Matrix4d camTrans = camera.getRotation().toMatrix().inverse();
		Vector3d camDirection = new Vector3d(0, 0, 1);

		View view = LayersModel.getSingletonInstance().getActiveView();
		if (view == null)
			return 0;

		GL3DCoordinateSystemView layer = view
				.getAdapter(GL3DCoordinateSystemView.class);
		GL3DState state = GL3DState.get();
		CoordinateVector orientationVector = layer.getOrientation();
		CoordinateConversion toViewSpace = layer.getCoordinateSystem()
				.getConversion(
						state.activeCamera.getViewSpaceCoordinateSystem());
		Vector3d orientation = toViewSpace.convert(orientationVector)
				.toVector3d().normalize();

		camDirection = camTrans.multiply(camDirection).normalize();

		double angle = (Math.acos(camDirection.dot(orientation)) / Math.PI * 180.0);
		return angle;
	}

	private void initShaders(GL2 gl) {
		int vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		int fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

		String vertexShaderSrc = loadShaderFromFile("/shader/OverViewVertex.glsl");
		String fragmentShaderSrc = loadShaderFromFile("/shader/MainFragment.glsl");

		gl.glShaderSource(vertexShader, 1, new String[] { vertexShaderSrc },
				(int[]) null, 0);
		gl.glCompileShader(vertexShader);

		gl.glShaderSource(fragmentShader, 1,
				new String[] { fragmentShaderSrc }, (int[]) null, 0);
		gl.glCompileShader(fragmentShader);

		this.shaderprogram = gl.glCreateProgram();
		gl.glAttachShader(shaderprogram, vertexShader);
		gl.glAttachShader(shaderprogram, fragmentShader);
		gl.glLinkProgram(shaderprogram);
		gl.glValidateProgram(shaderprogram);
		textureHelper.checkGLErrors(gl, this + ".afterValidateProgram");

		IntBuffer intBuffer = IntBuffer.allocate(1);
		gl.glGetProgramiv(shaderprogram, GL2.GL_LINK_STATUS, intBuffer);
		if (intBuffer.get(0) != 1) {
			gl.glGetProgramiv(shaderprogram, GL2.GL_INFO_LOG_LENGTH, intBuffer);
			int size = intBuffer.get(0);
			System.err.println("Program link error: ");
			if (size > 0) {
				ByteBuffer byteBuffer = ByteBuffer.allocate(size);
				gl.glGetProgramInfoLog(shaderprogram, size, intBuffer,
						byteBuffer);
				for (byte b : byteBuffer.array()) {
					System.err.print((char) b);
				}
			} else {
				System.out.println("Unknown");
			}
			System.exit(1);
		}
		gl.glUseProgram(shaderprogram);
	}

	private String loadShaderFromFile(String shaderName) {
		StringBuilder shaderCode = new StringBuilder();
		String line = null;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				CompenentView.class.getResourceAsStream(shaderName)))) {
			while ((line = br.readLine()) != null) {
				shaderCode.append(line + "\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return shaderCode.toString();
	}

	private ByteBuffer readPixels(BufferedImage image, boolean storeAlphaChannel) {
		int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0,
				image.getWidth());

		ByteBuffer buffer = ByteBuffer.allocate(image.getWidth()
				* image.getHeight() * 3); // 4 for RGBA, 3 for RGB

		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int pixel = pixels[y * image.getWidth() + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF));
				buffer.put((byte) ((pixel >> 8) & 0xFF));
				buffer.put((byte) (pixel & 0xFF));
			}
		}

		buffer.flip();

		return buffer;
	}

	@Override
	public void viewChanged(View sender, ChangeEvent aEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cameraMoved(GL3DCamera camera) {
		this.canvas.repaint();
	}

	@Override
	public void cameraMoving(GL3DCamera camera) {
		this.canvas.repaint();
	}

	@Override
	public void layerAdded(int idx) {
		System.out.println("layer added and repaint");
		this.canvas.repaint();
	}

	@Override
	public void layerRemoved(View oldView, int oldIdx) {
		this.canvas.repaint();
	}

	@Override
	public void layerChanged(int idx) {
		this.canvas.repaint();
	}

	@Override
	public void activeLayerChanged(int idx) {
		/*
		 * System.out.println("changed"); if (idx >= 0 && idx <
		 * LayersModel.getSingletonInstance().getNumLayers() && layers.size() >
		 * 0){ if (LinkedMovieManager.getActiveInstance().getMasterMovie() !=
		 * null) this.lastTime =
		 * LinkedMovieManager.getActiveInstance().getMasterMovie
		 * ().getCurrentFrameDateTime().getMillis(); idx =
		 * LayersModel.getSingletonInstance().getNumLayers() - idx -1; lastLayer
		 * = layers.get(idx); View lastView =
		 * LayersModel.getSingletonInstance().getActiveView(); // Just a hack
		 * GLFilterView opacityFilterView =
		 * lastView.getAdapter(GLFilterView.class
		 * ).getAdapter(GLFilterView.class); GLFilterView lutView =
		 * (GLFilterView)opacityFilterView.getView(); SOHOLUTFilter lutFilter =
		 * (SOHOLUTFilter)lutView.getFilter();
		 * this.setCurrentLutByName(lutFilter.getLUT(), true); this.invertedLut
		 * = lutFilter.isInverted(); if (lastLayer.getImageData() != null){
		 * this.updateTexture = true; } } else { lastLayer = null; }
		 * this.canvas.repaint();
		 */
	}

	/*
	 * private void createTexture(GL2 gl, Layer layer){ ImageData imageData =
	 * layer.getJhvjpxView().getImageData(); int bitsPerPixel =
	 * imageData.getImageTransport().getNumBitsPerPixel(); Buffer buffer;
	 * 
	 * switch (bitsPerPixel) { case 8: buffer =
	 * ByteBuffer.wrap(((Byte8ImageTransport) imageData
	 * .getImageTransport()).getByte8PixelData()); break; case 16: buffer =
	 * ShortBuffer.wrap(((Short16ImageTransport) imageData
	 * .getImageTransport()).getShort16PixelData()); break; case 32: buffer =
	 * IntBuffer.wrap(((Int32ImageTransport) imageData
	 * .getImageTransport()).getInt32PixelData()); break; default: buffer =
	 * null; }
	 * 
	 * gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
	 * gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
	 * gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
	 * gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, bitsPerPixel >> 3);
	 * 
	 * textureHelper.checkGLErrors(gl, this + ".afterPixelStore"); ImageFormat
	 * imageFormat = imageData.getImageFormat(); int internalFormat =
	 * GLTextureHelper.mapImageFormatToInternalGLFormat(imageFormat); int
	 * inputFormat = GLTextureHelper.mapImageFormatToInputGLFormat(imageFormat);
	 * int width = imageData.getWidth(); int height = imageData.getHeight(); int
	 * inputType = GLTextureHelper.mapBitsPerPixelToGLType(bitsPerPixel);
	 * 
	 * gl.glBindTexture(GL2.GL_TEXTURE_2D, layer.texture);
	 * 
	 * int width2 = nextPowerOfTwo(width); int height2 = nextPowerOfTwo(height);
	 * 
	 * int bpp = 3; switch (inputFormat) { case GL2.GL_LUMINANCE: case
	 * GL2.GL_ALPHA: bpp = 1; break; case GL2.GL_LUMINANCE_ALPHA: bpp = 2;
	 * break; case GL2.GL_RGB: bpp = 3; break; case GL2.GL_RGBA: case
	 * GL2.GL_BGRA: bpp = 4; break;
	 * 
	 * default: throw new RuntimeException("" + inputFormat); }
	 * 
	 * switch (inputType) { case GL2.GL_UNSIGNED_BYTE: bpp *= 1; break; case
	 * GL2.GL_UNSIGNED_SHORT: case GL2.GL_UNSIGNED_SHORT_5_6_5: case
	 * GL2.GL_UNSIGNED_SHORT_4_4_4_4: case GL2.GL_UNSIGNED_SHORT_5_5_5_1: bpp *=
	 * 2; break; case GL2.GL_UNSIGNED_INT_8_8_8_8_REV: bpp *= 4; break; default:
	 * throw new RuntimeException("" + inputType); }
	 * 
	 * ByteBuffer b = ByteBuffer.allocate(width2 * height2 * bpp);
	 * b.limit(width2 * height2 * bpp);
	 * 
	 * gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width2, height2, 0,
	 * inputFormat, inputType, b);
	 * 
	 * textureHelper.checkGLErrors(gl, this + ".glTexImage2d"); if (buffer !=
	 * null) { gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, width, height,
	 * inputFormat, inputType, buffer); } textureHelper.checkGLErrors(gl, this +
	 * ".glTexSubImage2d");
	 * 
	 * gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
	 * GL2.GL_LINEAR); gl.glTexParameteri(GL2.GL_TEXTURE_2D,
	 * GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
	 * gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
	 * GL2.GL_CLAMP_TO_BORDER); gl.glTexParameteri(GL2.GL_TEXTURE_2D,
	 * GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER); }
	 */
	@Override
	public void viewportGeometryChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public void timestampChanged(int idx) {
		if (LinkedMovieManager.getActiveInstance().getMasterMovie() != null)
			if (this.lastTime != LinkedMovieManager.getActiveInstance()
					.getMasterMovie().getCurrentFrameDateTime().getMillis()) {
				this.lastTime = LinkedMovieManager.getActiveInstance()
						.getMasterMovie().getCurrentFrameDateTime().getMillis();
				this.updateTexture = true;
				this.canvas.repaint();
			}

	}

	@Override
	public void subImageDataChanged() {
		this.updateTexture = true;
		this.canvas.repaint();
	}

	@Override
	public void layerDownloaded(int idx) {
		// TODO Auto-generated method stub

	}

	public void displayLayer(GL2 gl, Layer layer) {
		Vector2d lowerleftCorner = layer.getJhvjpxView().getMetaData()
				.getPhysicalRegion().getLowerLeftCorner();
		Vector2d size = layer.getJhvjpxView().getMetaData().getPhysicalRegion()
				.getSize();
		if (size.x <= 0 || size.y <= 0) {
			return;
		}

		double aspect = this.canvas.getSize().getWidth()
				/ this.canvas.getSize().getHeight();
		float x0 = (float) lowerleftCorner.x;
		float y0 = (float) lowerleftCorner.y;
		float x1 = x0 + (float) size.x;
		float y1 = y0 + (float) size.y;
		float tmpX0 = x0;
		float tmpX1 = x1;
		float tmpY0 = y0;
		float tmpY1 = y1;
		// if height is bigger then width
		if (aspect < 1) {
			tmpX0 = (float) (x0 * aspect);
			tmpX1 = (float) (x1 * aspect);
		} else {
			tmpY0 = (float) (y0 / aspect);
			tmpY1 = (float) (y1 / aspect);
		}
		System.out.println("layer : " + layer.getJhvjpxView().getMetaData().getSunPixelPosition());
		System.out.println("layer : " + layer.getJhvjpxView().getMetaData().getSunPixelRadius());
		System.out.println(" : " + layer.getJhvjpxView().getMetaData().getSunPixelPosition());
		System.out.println(" : " + layer.getJhvjpxView().getMetaData().getUnitsPerPixel());
		gl.glDisable(GL2.GL_DEPTH_TEST);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(tmpX0, tmpX1, tmpY0, tmpY1, 10, -10);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glColor3f(1, 1, 1);
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, layer.getTexture());

		gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);

		gl.glUseProgram(shaderprogram);

		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, LUT.getLut().getTexture(gl));

		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "texture"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lut"), 1);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "sunRadius"),
				(float) Constants.SUN_RADIUS);
		System.out.println("opacity : " + layer.opacity);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "opacity"),
				(float) layer.opacity);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "gamma"),
				(float) layer.gamma);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "lutPosition"),
				layer.lut.idx);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lutInverted"),
				layer.lut.getState());
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "redChannel"),
				layer.redChannel.getState());
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "greenChannel"),
				layer.greenChannel.getState());
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "blueChannel"),
				layer.blueChannel.getState());
		float[] transformation = cameraNEW.getTransformation().toFloatArray();
		System.out.println("TRANSFORM : ");
		float[] layerTransformation = layer.getJhvjpxView().getMetaData()
				.getRotation().toMatrix().toFloatArray();
		gl.glUniformMatrix4fv(
				gl.glGetUniformLocation(shaderprogram, "transformation"), 1,
				true, transformation, 0);

		gl.glUniformMatrix4fv(
				gl.glGetUniformLocation(shaderprogram, "layerTransformation"), 1,
				true, layerTransformation, 0);

		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0.0f, 1.0f);
		gl.glVertex2d(x0, y0);
		gl.glTexCoord2f(1.0f, 1.0f);
		gl.glVertex2d(x1, y0);
		gl.glTexCoord2f(1.0f, 0.0f);
		gl.glVertex2d(x1, y1);
		gl.glTexCoord2f(0.0f, 0.0f);
		gl.glVertex2d(x0, y1);

		gl.glEnd();
		gl.glUseProgram(0);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glEnable(GL2.GL_DEPTH_TEST);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glViewport(0, 0, this.canvas.getSurfaceWidth(),
				this.canvas.getSurfaceHeight());
		
		
		if (layers != null && layers.getLayerCount() > 0) {
			gl.glPushMatrix();
			
			if (this.updateTexture) {
				textureHelper.checkGLErrors(gl, this + ".beforeCreateTexture");
				for (Layer layer : layers.getLayers()) {
					layer.updateTexture(gl);
				}
				updateTexture = false;
			}
			
			for (Layer layer : layers.getLayers()) {
				if (layer.isVisible()) {
					this.displayLayer(gl, layer);
				}
			}
			gl.glPopMatrix();
			// renderRect(gl);
		}
		
		// empty screen
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		System.out.println("layercount : " + layers.getLayerCount());
		if (layers.getLayerCount() <= 0) {
			splashScreen.render(gl, canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
		}

	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(GLAutoDrawable drawable) {
		if (System.getProperty("jhvVersion") == null)
			drawable.setGL(new DebugGL2(drawable.getGL().getGL2()));
		// GuiState3DWCS.overViewPanel.activate(drawable.getContext());
		GL2 gl = drawable.getGL().getGL2();
		splashScreen = new SplashScreen(gl);
		GL3DState.create(gl);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		gl.glEnable(GL2.GL_TEXTURE_2D);

		textureHelper.checkGLErrors(gl, this + ".beforeInitShader");
		this.initShaders(gl);
		textureHelper.checkGLErrors(gl, this + ".afterInitShader");

		this.canvas.repaint();
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
			int arg4) {
		this.canvas.repaint();
	}

	@Override
	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		this.canvas.repaint();

	}

	@Override
	public void mousePressed(MouseEvent e) {
		RayTrace rayTrace = new RayTrace(cameraNEW);
		this.updateTexture = true;
		Ray ray = rayTrace.cast(e.getX(), e.getY());
		System.out.println("rayTrace --> new : " + ray.getHitpoint());
		System.out.println("Sphere-type : "
				+ (ray.hitpointType == RayTrace.HITPOINT_TYPE.SPHERE));
		System.out.println("Plane -type : "
				+ (ray.hitpointType == RayTrace.HITPOINT_TYPE.PLANE));
		this.canvas.repaint();

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public GLCanvas getComponent() {
		return canvas;
	}

	public Dimension getCanavasSize() {
		return new Dimension(canvas.getSurfaceWidth(),
				canvas.getSurfaceHeight());
	}

	public void regristryAnimation(long duration) {
		// TODO Auto-generated method stub

	}

	public void saveScreenshot(String defaultExtension, File selectedFile,
			int imageWidth, int imageHeight, ArrayList<String> descriptions) {
		// TODO Auto-generated method stub

	}

	public void addPostRenderer(ScreenRenderer postRenderer) {
		// TODO Auto-generated method stub

	}

	public BufferedImage getBufferedImage(int imageWidth, int imageHeight,
			ArrayList<String> descriptions) {
		// TODO Auto-generated method stub
		return null;
	}

	public void removePostRenderer(ScreenRenderer postRenderer) {
		// TODO Auto-generated method stub

	}

	public void updateMainImagePanelSize(Vector2i vector2i) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newlayerAdded() {
		System.out.println("newLayerAdded");
		this.updateTexture = true;
		this.canvas.repaint();
	}

	@Override
	public void newlayerRemoved(int idx) {
		this.updateTexture = true;
		this.canvas.repaint();
	}

	@Override
	public void newtimestampChanged() {
		this.updateTexture = true;
		this.canvas.repaint();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

	}

	@Override
	public void cameraMoved() {
		this.canvas.repaint(20);
	}

	@Override
	public void cameraMoving() {
		this.canvas.repaint(20);
	}

	@Override
	public void activeLayerChanged(Layer layer) {
		// TODO Auto-generated method stub

	}

}
