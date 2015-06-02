package org.helioviewer.jhv.viewmodel.view.opengl;

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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.LayerInterface.COLOR_CHANNEL_TYPE;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.NewLayer;
import org.helioviewer.jhv.layers.NewLayerListener;
import org.helioviewer.jhv.layers.filter.LUT;
import org.helioviewer.jhv.opengl.CenterLoadingScreen;
import org.helioviewer.jhv.opengl.NoImageScreen;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.RenderAnimation;
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
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.plugins.plugin.Plugin;
import org.helioviewer.jhv.plugins.plugin.Plugins;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;

import ch.fhnw.i4ds.helio.coordinate.converter.Hcc2HgConverter;
import ch.fhnw.i4ds.helio.coordinate.coord.HeliocentricCartesianCoordinate;
import ch.fhnw.i4ds.helio.coordinate.coord.HeliographicCoordinate;

import com.jogamp.opengl.DebugGL2;
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

public class MainPanel extends GLCanvas implements GLEventListener,
		MouseListener, MouseMotionListener, MouseWheelListener,
		NewLayerListener, TimeLineListener, Camera {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6714893614985558471L;

	public static final double MAX_DISTANCE = Constants.SUN_MEAN_DISTANCE_TO_EARTH * 1.8;
	public static final double MIN_DISTANCE = Constants.SUN_RADIUS * 1.2;
	private static final double DEFAULT_CAMERA_DISTANCE = 14 * Constants.SUN_RADIUS;

	private static final double CLIP_NEAR = Constants.SUN_RADIUS / 10;
	public static final double CLIP_FAR = Constants.SUN_RADIUS * 1000;
	public static final double FOV = 10;
	private double aspect = 0.0;

	private double[][] rectBounds;

	protected Quaternion3d rotation;
	protected Vector3d translation;
	private ArrayList<MainPanel> synchronizedViews;

	private CopyOnWriteArrayList<CameraAnimation> cameraAnimations;

	private Layers layers;
	private int shaderprogram;
	private HashMap<String, Integer> lutMap;
	private int nextAvaibleLut = 0;

	private CopyOnWriteArrayList<RenderAnimation> animations;
	private NoImageScreen splashScreen;
	private CenterLoadingScreen loadingScreen;

	protected CameraInteraction[] cameraInteractions;
	public boolean fullScreenMode;

	private boolean track = false;

	private LocalDateTime lastDate;

	private int[] frameBufferObject;

	private int[] renderBufferDepth;

	private int[] renderBufferColor;

	private static int DEFAULT_TILE_WIDTH = 2048;
	private static int DEFAULT_TILE_HEIGHT = 2048;

	public MainPanel() {
		this.cameraAnimations = new CopyOnWriteArrayList<CameraAnimation>();
		this.synchronizedViews = new ArrayList<MainPanel>();
		this.setSharedContext(OpenGLHelper.glContext);
		Layers.LAYERS.addNewLayerListener(this);
		TimeLine.SINGLETON.addListener(this);
		animations = new CopyOnWriteArrayList<RenderAnimation>();
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addGLEventListener(this);
		this.addMouseWheelListener(this);
		layers = Layers.LAYERS;

		this.rotation = Quaternion3d.createRotation(0.0, new Vector3d(0, 1, 0));
		this.translation = new Vector3d(0, 0, DEFAULT_CAMERA_DISTANCE);

		cameraInteractions = new CameraInteraction[2];
		cameraInteractions[0] = new CameraZoomInteraction(this, this);
		cameraInteractions[1] = new CameraRotationInteraction(this, this);

		rectBounds = new double[40][3];
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

	public void setTransformation(Quaternion3d rotation, Vector3d translation) {
		this.rotation = rotation;
		this.translation = translation;
		repaintViewAndSynchronizedViews();
	}

	public void setRotationInteraction() {
		this.cameraInteractions[1] = new CameraRotationInteraction(this, this);
	}

	public void setPanInteraction() {
		this.cameraInteractions[1] = new CameraPanInteraction(this, this);
	}

	public void setZoomBoxInteraction() {
		this.cameraInteractions[1] = new CameraZoomBoxInteraction(this, this);
	}

	private void initShaders(GL2 gl) {
		int vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		int fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

		String vertexShaderSrc = loadShaderFromFile("/shader/MainVertex.glsl");
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

	public boolean displayLayer(GL2 gl, NewLayer layer) {

		int layerTexture = layer.getTexture(this, false);
		if (layerTexture > 0) {
			Vector2d size = layer.getMetaData().getPhysicalRegion().getSize();
			if (size.x <= 0 || size.y <= 0) {
				return false;
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
			if (!Layers.LAYERS.getCoronaVisibility())
				opacityCorona = 0;
			gl.glEnable(GL2.GL_DEPTH_TEST);
			gl.glDepthFunc(GL2.GL_LEQUAL);
			gl.glDepthMask(true);  
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
			gl.glBindTexture(GL2.GL_TEXTURE_2D, LUT.getTexture());

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
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "sharpen"),
					(float) layer.getSharpen());
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
			gl.glUniform1i(
					gl.glGetUniformLocation(shaderprogram, "cameraMode"),
					CameraMode.getCameraMode());
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "contrast"),
					(float) layer.getContrast());
			
			float clipNear = (float)Math.max(this.translation.z - 4 * Constants.SUN_RADIUS, CLIP_NEAR);
			gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "near"), clipNear);
			float[] transformation = getTransformation().toFloatArray();
			float[] layerTransformation = layer.getMetaData().getRotation()
					.toMatrix().toFloatArray();
			gl.glUniformMatrix4fv(
					gl.glGetUniformLocation(shaderprogram, "transformation"),
					1, true, transformation, 0);

			gl.glUniformMatrix4fv(gl.glGetUniformLocation(shaderprogram,
					"layerTransformation"), 1, true, layerTransformation, 0);
			gl.glUniform2f(
					gl.glGetUniformLocation(shaderprogram, "imageResolution"),
					layer.getLastDecodedImageRegion().textureHeight,
					layer.getLastDecodedImageRegion().textureHeight);
			gl.glBegin(GL2.GL_QUADS);
			
			gl.glTexCoord2f(0.0f, 1.0f);
			gl.glVertex3d(-1, -1, 0);
			gl.glTexCoord2f(1.0f, 1.0f);
			gl.glVertex3d(1, -1, 0);
			gl.glTexCoord2f(1.0f, 0.0f);
			gl.glVertex3d(1, 1, 0);
			gl.glTexCoord2f(0.0f, 0.0f);
			gl.glVertex3d(-1, 1, 0);

			gl.glEnd();
			gl.glUseProgram(0);
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glDisable(GL2.GL_BLEND);
			gl.glDisable(GL2.GL_TEXTURE_2D);
			gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
			gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
			gl.glEnable(GL2.GL_DEPTH_TEST);
			gl.glDepthMask(false);  
			return true;

		} else{
			return false;
		}
	}

	protected void render(GL2 gl) {
		gl.glClearDepth(1);
		gl.glDepthMask(true);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
		gl.glDepthMask(false);
		if (track)
			calculateTrackRotation();
		if (layers != null && layers.getLayerCount() > 0) {
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPushMatrix();

			double clipNear = Math.max(this.translation.z - 4 * Constants.SUN_RADIUS, CLIP_NEAR);
			
			gl.glOrtho(-1, 1, -1, 1, clipNear, this.translation.z + 4 * Constants.SUN_RADIUS);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glPushMatrix();
			
			Matrix4d mat = Matrix4d.identity();
			mat.setTranslation(0, 0, -translation.z);
			gl.glMultMatrixd(mat.m, 0);
			if (CameraMode.mode == MODE.MODE_2D) {
				this.rotation = layers.getActiveLayer().getMetaData()
						.getRotation().copy();
			}
			
			boolean layerLoaded = true;
			boolean notCenterAnimation = false;
			for (LayerInterface layer : layers.getLayers()) {
				if (layer.isVisible()) {
					boolean visibleLayer = this.displayLayer(gl, (NewLayer) layer);
					layerLoaded &= visibleLayer;
					notCenterAnimation |= visibleLayer;
				}
			}
			
			gl.glPopMatrix();

			if (!layerLoaded){
				int xOffset, yOffset, width, height;
				if (notCenterAnimation){
					xOffset = (int)(getSurfaceWidth() * 0.9);
					width = (int)(getSurfaceWidth() * 0.1);
					yOffset = (int)(getSurfaceHeight() * 0.9);
					height = (int)(getSurfaceHeight() * 0.1);
				}
				else {
					xOffset = (int)(getSurfaceWidth() * 0.425);
					width = (int)(getSurfaceWidth() * 0.15);
					yOffset = (int)(getSurfaceHeight() * 0.425);
					height = (int)(getSurfaceHeight() * 0.15);					
				}
				gl.glViewport(xOffset, yOffset, width, height);
				loadingScreen.render(gl);
				gl.glViewport(0, 0, getSurfaceWidth(), getSurfaceHeight());
				repaint(20);
			}
			Quaternion3d rotation = new Quaternion3d(this.rotation.getAngle(), this.rotation.getRotationAxis().negateY());
			Matrix4d transformation = rotation.toMatrix();
			transformation.addTranslation(translation.negate());
			gl.glMultMatrixd(transformation.m, 0);
			

			
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glPushMatrix();
			

			/*gl.glOrtho(-width + this.translation.x, width + this.translation.x,
					width + this.translation.y, -width + this.translation.y,
					CLIP_NEAR, CLIP_FAR);*/
			GLU glu = new GLU();
			
			glu.gluPerspective(MainPanel.FOV, this.aspect, clipNear, this.translation.z + 4 * Constants.SUN_RADIUS);
			/*gl.glFrustum(-width + this.translation.x, width + this.translation.x,
					-width + this.translation.y, width + this.translation.y,
					CLIP_NEAR, CLIP_FAR);*/
			//if (CameraMode.mode == MODE.MODE_2D) {
			//	gl.glOrtho(-1, 1, 1, -1, 10, -10);
			//} else {
				//gl.glFrustum(-1, 1, -1, 1, 10, 20);
				//gl.glTranslated(0, 0, -10);
			//}

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			calculateBounds();
			for (CameraInteraction cameraInteraction : cameraInteractions) {
				cameraInteraction.renderInteraction(gl);
			}
			gl.glEnable(GL2.GL_DEPTH_TEST);
			gl.glDepthFunc(GL2.GL_LESS);
			for (Plugin plugin : Plugins.plugins) {
				if (plugin.getRenderer() != null
						&& plugin.getRenderer().isVisible()) {
					plugin.getRenderer().render(gl);
				}
			}
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glMatrixMode(GL2.GL_MODELVIEW);

		}

		// empty screen
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);

		if (!cameraAnimations.isEmpty() && cameraAnimations.get(0).isFinished())
			cameraAnimations.remove(0);
		if (!cameraAnimations.isEmpty()) {
			cameraAnimations.get(0).animate(this);
		}
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		drawable.getContext().makeCurrent();
		GL2 gl = drawable.getGL().getGL2();
		gl.glViewport(0, 0, this.getSurfaceWidth(), this.getSurfaceHeight());

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glScaled(1, aspect, 1);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		this.render(gl);
		
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glUseProgram(0);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);

		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);

		gl.glEnable(GL2.GL_TEXTURE_2D);
 		gl.glColor3d(1, 1, 1);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		int tmp[] = new int[1];
		gl.glGenTextures(1, tmp, 0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, tmp[0]);
		gl.glCopyTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT24, 0, 0, this.getSurfaceWidth(), this.getSurfaceHeight(), 0);
		gl.glDisable(GL2.GL_DEPTH_TEST);

		gl.glDisable(GL2.GL_BLEND);
		
		//gl.glBindTexture(GL2.GL_TEXTURE_2D, LUT.getTexture());

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_LINEAR);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glPushMatrix();
		gl.glOrtho(0, 1, 0, 1, 10, -10);
		gl.glViewport(0, 0, (int)(this.getSurfaceWidth() * 0.3), (int)(this.getSurfaceHeight() * 0.3));
		gl.glMatrixMode(GL2.GL_MODELVIEW );
		gl.glLoadIdentity();
		gl.glBegin( GL2.GL_QUADS );
		gl.glTexCoord2f(0, 0);
		gl.glVertex2d(0, 1);
		gl.glTexCoord2f(1, 0);
		gl.glVertex2d(1, 1);
		gl.glTexCoord2f(1, 1);
		gl.glVertex2d(1, 0);
		gl.glTexCoord2f(0, 1);
		gl.glVertex2d(0, 0);
	    gl.glEnd();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW );
		
		
		if (Layers.LAYERS.getLayerCount() == 0){
			int xOffset = (int)(getSurfaceWidth() * 0.425);
			int width = (int)(getSurfaceWidth() * 0.15);
			int yOffset = (int)(getSurfaceHeight() * 0.425);
			int height = (int)(getSurfaceHeight() * 0.15);
			gl.glViewport(xOffset, yOffset, width, height);
			splashScreen.render(gl);
			gl.glViewport(0, 0, getSurfaceWidth(), getSurfaceHeight());
		}
	}

	protected void calculateTrackRotation() {
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
			Hcc2HgConverter converter = new Hcc2HgConverter();
			HeliographicCoordinate newCoord = converter.convert(cart);
			double angle = DifferentialRotation.calculateRotationInRadians(
					newCoord.getHgLatitude().radValue(), seconds);

			Quaternion3d rotation = Quaternion3d.createRotation(angle,
					new Vector3d(0, 1, 0));

			rotation.rotate(this.rotation);
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
				rectBounds[i][0] = hitpoint.x;
				rectBounds[i][1] = hitpoint.y;
				rectBounds[i][2] = hitpoint.z;
			} else if (i < 20) {
				Vector3d hitpoint = rayTrace.cast(this.getWidth(),
						(i - 10) * height, this).getHitpoint();
				rectBounds[i][0] = hitpoint.x;
				rectBounds[i][1] = hitpoint.y;
				rectBounds[i][2] = hitpoint.z;
			} else if (i < 30) {
				Vector3d hitpoint = rayTrace.cast((29 - i) * width,
						getHeight(), this).getHitpoint();
				rectBounds[i][0] = hitpoint.x;
				rectBounds[i][1] = hitpoint.y;
				rectBounds[i][2] = hitpoint.z;
			} else if (i < 40) {
				Vector3d hitpoint = rayTrace.cast(0, (39 - i) * height, this)
						.getHitpoint();
				rectBounds[i][0] = hitpoint.x;
				rectBounds[i][1] = hitpoint.y;
				rectBounds[i][2] = hitpoint.z;
			}
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

		this.initShaders(gl);

		splashScreen = new NoImageScreen();
		loadingScreen = new CenterLoadingScreen();
		
		gl.glDisable(GL2.GL_TEXTURE_2D);

	}

	private void generateNewRenderBuffers(GL2 gl, int width, int height) {
		// tileWidth = defaultTileWidth;
		// tileHeight = defaultTileHeight;
		if (renderBufferDepth != null) {
			gl.glDeleteRenderbuffers(1, renderBufferDepth, 0);
		}
		renderBufferDepth = new int[1];
		gl.glGenRenderbuffers(1, renderBufferDepth, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferDepth[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT,
				width, height);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_DEPTH_ATTACHMENT, GL2.GL_RENDERBUFFER,
				renderBufferDepth[0]);

		if (renderBufferColor != null) {
			gl.glDeleteRenderbuffers(1, renderBufferColor, 0);
		}
		renderBufferColor = new int[1];
		gl.glGenRenderbuffers(1, renderBufferColor, 0);
		gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferColor[0]);
		gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_RGBA8, width,
				height);
		gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER,
				GL2.GL_COLOR_ATTACHMENT0, GL2.GL_RENDERBUFFER,
				renderBufferColor[0]);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		aspect = this.getSize().getWidth() / this.getSize().getHeight();
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
		return new Dimension(this.getSurfaceWidth(), this.getSurfaceHeight());
	}

	public BufferedImage getBufferedImage(int imageWidth, int imageHeight,
			ArrayList<String> descriptions) {

		int tileWidth = imageWidth < DEFAULT_TILE_WIDTH ? imageWidth
				: DEFAULT_TILE_WIDTH;
		int tileHeight = imageHeight < DEFAULT_TILE_HEIGHT ? imageHeight
				: DEFAULT_TILE_HEIGHT;

		double xTiles = imageWidth / (double) tileWidth;
		double yTiles = imageHeight / (double) tileHeight;
		int countXTiles = imageWidth % tileWidth == 0 ? (int) xTiles
				: (int) xTiles + 1;
		int countYTiles = imageHeight % tileHeight == 0 ? (int) yTiles
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
		GLContext offscreenContext = this.getContext();
		offscreenDrawable.setRealized(true);
		offscreenContext.makeCurrent();
		GL2 offscreenGL = offscreenContext.getGL().getGL2();

		offscreenGL.glBindFramebuffer(GL2.GL_FRAMEBUFFER, frameBufferObject[0]);
		generateNewRenderBuffers(offscreenGL, tileWidth, tileHeight);

		BufferedImage screenshot = new BufferedImage(imageWidth, imageHeight,
				BufferedImage.TYPE_3BYTE_BGR);
		ByteBuffer.wrap(((DataBufferByte) screenshot.getRaster()
				.getDataBuffer()).getData());

		offscreenGL.glViewport(0, 0, tileWidth, tileHeight);
		offscreenGL.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		double aspect = imageWidth / (double) imageHeight;
		double top = Math.tan(MainPanel.FOV / 360.0 * Math.PI)
				* MainPanel.CLIP_NEAR;
		double right = top * aspect;
		double left = -right;
		double bottom = -top;

		double tileLeft, tileRight, tileBottom, tileTop;
		TextRenderer textRenderer = new TextRenderer(new Font("SansSerif",
				Font.BOLD, 24));
		textRenderer.setColor(1f, 1f, 1f, 1f);

		offscreenGL.glViewport(0, 0, tileWidth, tileHeight);

		offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
		offscreenGL.glLoadIdentity();
		offscreenGL.glScaled(1, aspect, 1);
		offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);

		for (int x = 0; x < countXTiles; x++) {
			for (int y = 0; y < countYTiles; y++) {
				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glPushMatrix();
				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);
				tileLeft = left + (right - left) / xTiles * x;
				tileRight = left + (right - left) / xTiles * (x + 1);
				tileBottom = bottom + (top - bottom) / yTiles * y;
				tileTop = bottom + (top - bottom) / yTiles * (y + 1);

				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glViewport(0, 0, imageWidth, imageHeight);
				offscreenGL.glTranslated(-x, -y, 0);
				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);

				// double factor =
				int destX = tileWidth * x;
				int destY = tileHeight * y;

				render(offscreenGL);

				if (descriptions != null && x == 0 && y == 0) {
					int counter = 0;
					textRenderer.beginRendering(this.getSurfaceWidth(),
							this.getSurfaceHeight());
					for (String description : descriptions) {
						textRenderer.draw(description, 5, 5 + 40 * counter++);
					}
					textRenderer.endRendering();
				}
				offscreenGL.glPixelStorei(GL2.GL_PACK_ROW_LENGTH, imageWidth);
				offscreenGL.glPixelStorei(GL2.GL_PACK_SKIP_ROWS, destY);
				offscreenGL.glPixelStorei(GL2.GL_PACK_SKIP_PIXELS, destX);
				offscreenGL.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);

				int cutOffX = imageWidth >= (x + 1) * tileWidth ? tileWidth
						: tileWidth - x * tileWidth;
				int cutOffY = imageHeight >= (y + 1) * tileHeight ? tileHeight
						: tileHeight - y * tileHeight;

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
	public void newlayerAdded() {
		this.repaint();
	}

	@Override
	public void newlayerRemoved(int idx) {
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
		repaintViewAndSynchronizedViews();
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		this.repaint();
	}

	public double getAspect() {
		return this.aspect;
	}

	public void addSynchronizedView(MainPanel compenentView) {
		this.synchronizedViews.add(compenentView);
	}

	public void repaintViewAndSynchronizedViews() {
		this.repaint();
		for (MainPanel compenentView : synchronizedViews) {
			compenentView.repaint();
		}
	}

	@Override
	public void dateTimesChanged(int framecount) {
	}

	public double[][] getRectBounds() {
		return rectBounds;
	}

	public void addCameraAnimation(CameraAnimation cameraAnimation) {
		this.cameraAnimations.add(cameraAnimation);
		this.repaint();
	}

	public void resetCamera() {
		Quaternion3d rotation = Quaternion3d.createRotation(0.0, new Vector3d(
				0, 1, 0));
		Vector3d translation = new Vector3d(0, 0, DEFAULT_CAMERA_DISTANCE);
		this.addCameraAnimation(new CameraTransformationAnimation(rotation,
				translation, this));
		this.repaintViewAndSynchronizedViews();
	}

	public void toFullscreen() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {

				GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment
						.getLocalGraphicsEnvironment();
				GraphicsDevice graphicsDevice = MainFrame.SINGLETON
						.getGraphicsConfiguration().getDevice();
				if (graphicsDevice == null)
					graphicsDevice = graphicsEnvironment
							.getDefaultScreenDevice();

				final JFrame fullscreenFrame = new JFrame(graphicsDevice
						.getDefaultConfiguration());

				fullscreenFrame.getContentPane().setLayout(new BorderLayout());
				final Container lastParent = MainPanel.this.getParent();
				fullscreenFrame.getContentPane().add(MainPanel.this);

				fullscreenFrame.setExtendedState(Frame.MAXIMIZED_BOTH);
				fullscreenFrame.setUndecorated(true);
				fullscreenFrame.setResizable(false);

				if (graphicsDevice.isFullScreenSupported()) {
					graphicsDevice.setFullScreenWindow(fullscreenFrame);
				}

				fullscreenFrame.setVisible(true);

				final KeyAdapter keyAdapter = new KeyAdapter() {
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

	public void toggleTrack() {
		this.track = !track;
		this.lastDate = TimeLine.SINGLETON.getCurrentDateTime();
	}

	public boolean getTrack() {
		return track;
	}

}
