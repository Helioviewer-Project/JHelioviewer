package org.helioviewer.jhv.viewmodel.view.opengl;

import java.awt.Container;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.controller.Camera;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.LayerInterface.COLOR_CHANNEL_TYPE;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.NewLayer;
import org.helioviewer.jhv.layers.NewLayerListener;
import org.helioviewer.jhv.layers.filter.LUT;
import org.helioviewer.jhv.opengl.NoImageScreen;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.RenderAnimation;
import org.helioviewer.jhv.opengl.camera.CameraInteraction;
import org.helioviewer.jhv.opengl.camera.CameraPanInteraction;
import org.helioviewer.jhv.opengl.camera.CameraRotationInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomInteraction;
import org.helioviewer.jhv.opengl.camera.newCamera.CameraAnimation;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;

import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

public class MainPanel extends GLCanvas implements
		GLEventListener, MouseListener, MouseMotionListener,
		MouseWheelListener, NewLayerListener, TimeLineListener, Camera {

	public static final double MAX_DISTANCE = Constants.SUN_MEAN_DISTANCE_TO_EARTH * 1.8;
	public static final double MIN_DISTANCE = Constants.SUN_RADIUS * 1.2;
	public static final double DEFAULT_CAMERA_DISTANCE = 14 * Constants.SUN_RADIUS;

	public static final double CLIP_NEAR = Constants.SUN_RADIUS / 10;
	public static final double FOV = 10;
	private double aspect = 0.0;

	private double[][] rectBounds;

	protected Quaternion3d rotation;
	protected Vector3d translation;
	protected ArrayList<MainPanel> synchronizedViews;
	
	private CopyOnWriteArrayList<CameraAnimation> cameraAnimations;
	
	private Layers layers;
	private boolean updateTexture;
	private int shaderprogram;
	private HashMap<String, Integer> lutMap;
	private int nextAvaibleLut = 0;
	private long lastTime;
	public boolean exportMovie;

	private CopyOnWriteArrayList<RenderAnimation> animations;
	private NoImageScreen splashScreen;

	protected CameraInteraction[] cameraInteractions;
	public boolean fullScreenMode;

	public MainPanel() {
		this.cameraAnimations = new CopyOnWriteArrayList<CameraAnimation>();
		this.synchronizedViews = new ArrayList<MainPanel>();
		if (GLContext.getCurrent() != null)
			this.setSharedContext(GLContext.getCurrent());
		Layers.LAYERS.addNewLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
		animations = new CopyOnWriteArrayList<RenderAnimation>();
		lutMap = new HashMap<String, Integer>();
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addGLEventListener(this);
		this.addMouseWheelListener(this);
		loadLutFromFile("/UltimateLookupTable.txt");
		layers = Layers.LAYERS;

		this.rotation = Quaternion3d.createRotation(0.0, new Vector3d(0, 1, 0));
		this.translation = new Vector3d(0, 0, DEFAULT_CAMERA_DISTANCE);

		cameraInteractions = new CameraInteraction[2];
		cameraInteractions[0] = new CameraZoomInteraction(this, this);
		cameraInteractions[1] = new CameraRotationInteraction(this, this);

		rectBounds = new double[4][3];
	}

	public Quaternion3d getRotation() {
		return rotation;
	}

	public void setRotation(Quaternion3d rotation) {
		this.rotation = rotation;
		this.repaintViewAndSynchronizedViews();
	}

	public Vector3d getTranslation() {
		return translation;
	}

	public void setTranslation(Vector3d translation) {
		if (!translation.isApproxEqual(this.translation, 0)) {
			this.translation = translation;
			this.repaintViewAndSynchronizedViews();
		}
	}

	public Matrix4d getTransformation() {
		Matrix4d transformation = this.rotation.toMatrix();
		transformation.addTranslation(translation);
		return transformation;
	}

	public Matrix4d getTransformation(Quaternion3d rotation) {
		Quaternion3d newRotation = this.rotation.copy();
		newRotation.rotate(rotation);
		Matrix4d transformation = newRotation.toMatrix();
		transformation.addTranslation(translation);
		return transformation;
	}

	public void setZTranslation(double z) {
		Vector3d translation = new Vector3d(this.translation.x,
				this.translation.y, Math.max(MIN_DISTANCE,
						Math.min(MAX_DISTANCE, z)));
		if (!translation.isApproxEqual(this.translation, 0)) {
			this.translation = translation;
			this.repaintViewAndSynchronizedViews();
		}

	}

	public void setRotationInteraction(){
		this.cameraInteractions[1] = new CameraRotationInteraction(this, this);
	}
	
	public void setPanInteraction(){
		this.cameraInteractions[1] = new CameraPanInteraction(this, this);
	}
	
	private void loadLutFromFile(String lutTxtName) {
		String line = null;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				MainPanel.class.getResourceAsStream(lutTxtName), "UTF-8"))) {
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
		gl.glUseProgram(0);
	}

	private String loadShaderFromFile(String shaderName) {
		StringBuilder shaderCode = new StringBuilder();
		String line = null;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				MainPanel.class.getResourceAsStream(shaderName)))) {
			while ((line = br.readLine()) != null) {
				shaderCode.append(line + "\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return shaderCode.toString();
	}

	public void displayLayer(GL2 gl, NewLayer layer) {

		int layerTexture = layer.getTexture(this, false);
		if (layerTexture >= 0) {
			Vector2d lowerleftCorner = layer.getMetaData().getPhysicalRegion()
					.getLowerLeftCorner();
			Vector2d size = layer.getMetaData().getPhysicalRegion().getSize();
			if (size.x <= 0 || size.y <= 0) {
				return;
			}

			double aspect = this.getSize().getWidth()
					/ this.getSize().getHeight();
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
			MetaData metaData = layer.getMetaData();
			float xSunOffset = (float) ((metaData.getSunPixelPosition().x - metaData
					.getResolution().getX() / 2.0) / (float) metaData
					.getResolution().getX());
			float ySunOffset = -(float) ((metaData.getSunPixelPosition().y - metaData
					.getResolution().getY() / 2.0) / (float) metaData
					.getResolution().getY());

			Vector3d currentPos = getRotation().toMatrix().multiply(
					new Vector3d(0, 0, 1));
			Vector3d startPos = metaData.getRotation().toMatrix()
					.multiply(new Vector3d(0, 0, 1));

			double angle = Math.toDegrees(Math.acos(currentPos.dot(startPos)));
			double maxAngle = 60;
			double minAngle = 30;
			float opacityCorona = (float) ((Math.abs(90 - angle) - minAngle) / (maxAngle - minAngle));
			opacityCorona = opacityCorona > 1 ? 1f : opacityCorona;
			gl.glDisable(GL2.GL_DEPTH_TEST);

			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrtho(-1, 1, -1/aspect, 1/aspect, 10, -10);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glColor3f(1, 1, 1);
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_TEXTURE_2D);
			gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glBindTexture(GL2.GL_TEXTURE_2D, layerTexture);

			gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);
			gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);

			gl.glUseProgram(shaderprogram);

			gl.glActiveTexture(GL.GL_TEXTURE1);
			gl.glBindTexture(GL2.GL_TEXTURE_2D, LUT.getTexture(gl));

			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "texture"), 0);
			gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lut"), 1);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "fov"),
					(float) Math.toRadians(MainPanel.FOV / 2.0));
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "sunRadius"),
					(float) Constants.SUN_RADIUS);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram,
					"physicalImageWidth"), (float) metaData
					.getPhysicalImageWidth());
			gl.glUniform2f(gl.glGetUniformLocation(shaderprogram, "sunOffset"),
					xSunOffset, ySunOffset);
			gl.glUniform4f(
					gl.glGetUniformLocation(shaderprogram, "imageOffset"),
					layer.getLastDecodedImageRegion().getTextureOffsetX(),
					layer.getLastDecodedImageRegion().getTextureOffsetY(),
					layer.getLastDecodedImageRegion().getTextureScaleWidth(),
					layer.getLastDecodedImageRegion().getTextureScaleHeight());
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "opacity"),
					(float) layer.getOpacity());
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "gamma"),
					(float) layer.getGamma());
			System.out.println("sharpen : " + layer.getSharpen());
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "sharpen"), (float) layer.getSharpen());
			gl.glUniform1f(
					gl.glGetUniformLocation(shaderprogram, "lutPosition"),
					layer.getLut().ordinal());
			gl.glUniform1i(
					gl.glGetUniformLocation(shaderprogram, "lutInverted"),
					layer.getLutState());
			gl.glUniform1i(
					gl.glGetUniformLocation(shaderprogram, "redChannel"), layer
							.getColorChannel(COLOR_CHANNEL_TYPE.RED).getState());
			gl.glUniform1i(
					gl.glGetUniformLocation(shaderprogram, "greenChannel"),
					layer.getColorChannel(COLOR_CHANNEL_TYPE.GREEN).getState());
			gl.glUniform1i(
					gl.glGetUniformLocation(shaderprogram, "blueChannel"),
					layer.getColorChannel(COLOR_CHANNEL_TYPE.BLUE).getState());
			gl.glUniform1f(
					gl.glGetUniformLocation(shaderprogram, "opacityCorona"),
					opacityCorona);
			System.out.println("contrast : " + layer.getContrast());
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "contrast"), (float) layer.getContrast());
			float[] transformation = getTransformation().toFloatArray();
			float[] layerTransformation = layer.getMetaData().getRotation()
					.toMatrix().toFloatArray();
			gl.glUniformMatrix4fv(
					gl.glGetUniformLocation(shaderprogram, "transformation"),
					1, true, transformation, 0);

			gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram,
					"layerTransformation"), 1, true, layerTransformation, 0);
			System.out.println(layer.getLastDecodedImageRegion().textureWidth);
			System.out.println(layer.getLastDecodedImageRegion().getImageSize());
			gl.glUniform2f(gl.glGetUniformLocation(shaderprogram, "imageResolution"), layer.getLastDecodedImageRegion().textureHeight, layer.getLastDecodedImageRegion().textureHeight);
			gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0.0f, 1.0f);
			gl.glVertex2d(-1, -1);
			gl.glTexCoord2f(1.0f, 1.0f);
			gl.glVertex2d(1, -1);
			gl.glTexCoord2f(1.0f, 0.0f);
			gl.glVertex2d(1, 1);
			gl.glTexCoord2f(0.0f, 0.0f);
			gl.glVertex2d(-1, 1);

			gl.glEnd();
			gl.glUseProgram(0);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glDisable(GL2.GL_BLEND);
			gl.glDisable(GL2.GL_TEXTURE_2D);
			gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
			gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
			gl.glEnable(GL2.GL_DEPTH_TEST);
		}
		else
			this.repaint(1000);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.getContext().makeCurrent();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glViewport(0, 0, this.getSurfaceWidth(),
				this.getSurfaceHeight());

		if (layers != null && layers.getLayerCount() > 0) {
			gl.glPushMatrix();

				for (LayerInterface layer : layers.getLayers()) {
				if (layer.isVisible()) {
					this.displayLayer(gl, (NewLayer) layer);
				}
			}
			gl.glPopMatrix();
			calculateBounds();
		}		

		// empty screen
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);

		if (animations.size() > 0) {
			for (RenderAnimation animation : animations) {
				animation.render(gl, this.getSurfaceWidth(),
						this.getSurfaceHeight());
				this.repaint(20);
			}
		}

		/*
		 * if (layers.getLayerCount() <= 0 && animations.size() <= 0) {
		 * splashScreen.render(gl, this.getSurfaceWidth(),
		 * this.getSurfaceHeight()); }
		 */
		
		if (!cameraAnimations.isEmpty() && cameraAnimations.get(0).isFinished()) cameraAnimations.remove(0);
		if (!cameraAnimations.isEmpty()){
			System.out.println("animate");
				cameraAnimations.get(0).animate(this);
		}

	}

	private void calculateBounds() {
		RayTrace rayTrace = new RayTrace();
		Vector3d hitpoint = rayTrace.cast(0, 0, this).getHitpoint();
		rectBounds[0][0] = hitpoint.x;
		rectBounds[0][1] = hitpoint.y;
		rectBounds[0][2] = hitpoint.z;
		hitpoint = rayTrace.cast(this.getWidth(), 0, this).getHitpoint();
		rectBounds[1][0] = hitpoint.x;
		rectBounds[1][1] = hitpoint.y;
		rectBounds[1][2] = hitpoint.z;
		hitpoint = rayTrace.cast(this.getWidth(), this.getHeight(), this).getHitpoint();
		rectBounds[2][0] = hitpoint.x;
		rectBounds[2][1] = hitpoint.y;
		rectBounds[2][2] = hitpoint.z;
		hitpoint = rayTrace.cast(0, this.getHeight(), this).getHitpoint();
		rectBounds[3][0] = hitpoint.x;
		rectBounds[3][1] = hitpoint.y;
		rectBounds[3][2] = hitpoint.z;
	}

	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(GLAutoDrawable drawable) {
		System.out.println("init");
		if (System.getProperty("jhvVersion") == null)
			drawable.setGL(new DebugGL2(drawable.getGL().getGL2()));
		// GuiState3DWCS.overViewPanel.activate(drawable.getContext());
		aspect = this.getSize().getWidth() / this.getSize().getHeight();
		GL2 gl = drawable.getGL().getGL2();
		// splashScreen = new NoImageScreen(gl);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		gl.glEnable(GL2.GL_TEXTURE_2D);

		this.initShaders(gl);
		gl.glDisable(GL2.GL_TEXTURE_2D);

	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
		Container parent = this.getParent();
		//parent.removeAll();
		//parent.add(this);
		aspect = this.getSize().getWidth() / this.getSize().getHeight();
		System.out.println("width : " + width);
		System.out.println("height: " + height);
		//this.getParent().setPreferredSize(new Dimension(width, height));
		//gl.glViewport(0, 0, width, height);
		this.repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		for (CameraInteraction cameraInteraction : cameraInteractions) {
				cameraInteraction.mouseDragged(e);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		for (CameraInteraction cameraInteraction : cameraInteractions) {
				cameraInteraction.mousePressed(e);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		for (CameraInteraction cameraInteraction : cameraInteractions) {
				cameraInteraction.mouseReleased(e);
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	public Dimension getCanavasSize() {
		return new Dimension(this.getSurfaceWidth(),
				this.getSurfaceHeight());
	}

	public void regristryAnimation(long duration) {
		// TODO Auto-generated method stub

	}

	public void saveScreenshot(String defaultExtension, File selectedFile,
			int imageWidth, int imageHeight, ArrayList<String> descriptions) {
		// TODO Auto-generated method stub

	}

	public BufferedImage getBufferedImage(int imageWidth, int imageHeight,
			ArrayList<String> descriptions) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateMainImagePanelSize(Vector2i vector2i) {
		// TODO Auto-generated method stub

	}

	@Override
	public void newlayerAdded() {
		System.out.println("newLayerAdded");
		this.updateTexture = true;
		this.repaint();
	}

	@Override
	public void newlayerRemoved(int idx) {
		this.updateTexture = true;
		this.repaint();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		for (CameraInteraction cameraInteraction : cameraInteractions) {
				cameraInteraction.mouseWheelMoved(e);
		}
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		// TODO Auto-generated method stub

	}

	public void addRenderAnimation(RenderAnimation renderAnimation) {
		this.animations.add(renderAnimation);
		this.repaint();
	}

	public void removeRenderAnimation(RenderAnimation renderAnimation) {
		this.animations.remove(renderAnimation);
		this.repaint();
	}

	@Override
	public void timeStampChanged(LocalDateTime localDateTime) {
		this.repaint();
	}

	public double getAspect() {
		return this.getSize().getHeight() / this.getSize().getWidth();
	}

	public void setSynchronizedView(MainPanel compenentView) {
		this.synchronizedViews.add(compenentView);
	}

	public void repaintViewAndSynchronizedViews() {
		for (MainPanel compenentView : synchronizedViews) {
			compenentView.repaint();
		}
		this.repaint();
	}

	@Override
	public void dateTimesChanged(int framecount) {
	}
	
	public double[][] getRectBounds(){
		return rectBounds;
	}
	
	public double getAspectRatio(){
		return aspect;
	}
	
	public void addCameraAnimation(CameraAnimation cameraAnimation){
		this.cameraAnimations.add(cameraAnimation);
		this.repaint();
	}

	public void resetCamera() {
		this.rotation = Quaternion3d.createRotation(0.0, new Vector3d(0, 1, 0));
		this.translation = new Vector3d(0, 0, DEFAULT_CAMERA_DISTANCE);
		this.repaintViewAndSynchronizedViews();
	}

	public void toFullscreen() {
		// TODO Auto-generated method stub
		
	}

	public void escapeFullscreen() {
		// TODO Auto-generated method stub
		
	}

}
