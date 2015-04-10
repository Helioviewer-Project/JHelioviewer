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

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.NewLayerListener;
import org.helioviewer.jhv.layers.filter.LUT;
import org.helioviewer.jhv.opengl.NoImageScreen;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.RenderAnimation;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.newCamera.CameraListener;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.opengl.raytrace.RayTrace.Ray;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
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
	private int shaderprogram;
	private HashMap<String, Integer> lutMap;
	private int nextAvaibleLut = 0;
	private long lastTime;
	public boolean exportMovie;
	private Camera cameraNEW;

	private CopyOnWriteArrayList<RenderAnimation> animations;
	private NoImageScreen splashScreen;

	public CompenentView() {
		this.canvas.setSharedContext(OpenGLHelper.glContext);
		GuiState3DWCS.layers.addNewLayerListener(this);
		animations = new CopyOnWriteArrayList<RenderAnimation>();
		lutMap = new HashMap<String, Integer>();
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
	}


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
	public void subImageDataChanged(int idx) {
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
		MetaData metaData = layer.getJhvjpxView().getMetaData();
		float xSunOffset = (float)((metaData.getSunPixelPosition().x - metaData.getResolution().getX()/2.0)/(float)metaData.getResolution().getX());
		float ySunOffset = -(float)((metaData.getSunPixelPosition().y - metaData.getResolution().getY()/2.0)/(float)metaData.getResolution().getY());
		
		Vector3d currentPos = cameraNEW.getRotation().toMatrix().multiply(new Vector3d(0, 0, 1));
		Vector3d startPos = metaData.getRotation().toMatrix().multiply(new Vector3d(0, 0, 1));

		double angle = Math.toDegrees(Math.acos(currentPos.dot(startPos)));
		double maxAngle = 60;
		double minAngle = 30;
		float opacityCorona = (float) ((Math.abs(90 - angle) - minAngle) / (maxAngle - minAngle));
		opacityCorona = opacityCorona > 1 ? 1f : opacityCorona;
		System.out.println("opacityCorona : " + opacityCorona);
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
		gl.glBindTexture(GL2.GL_TEXTURE_2D, LUT.getTexture(gl));

		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "texture"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lut"), 1);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "sunRadius"),
				(float) Constants.SUN_RADIUS);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "physicalImageWidth"), (float)metaData.getPhysicalImageWidth());
		gl.glUniform2f(gl.glGetUniformLocation(shaderprogram, "sunOffset"), xSunOffset, ySunOffset);
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
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "opacityCorona"), opacityCorona);
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
		gl.getContext().makeCurrent();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glViewport(0, 0, this.canvas.getSurfaceWidth(),
				this.canvas.getSurfaceHeight());
		
		
		if (layers != null && layers.getLayerCount() > 0) {
			gl.glPushMatrix();
			
			if (this.updateTexture) {
				textureHelper.checkGLErrors(gl, this + ".beforeCreateTexture");
				for (LayerInterface layer : layers.getLayers()) {
					if (layer instanceof Layer)
					((Layer)layer).updateTexture(gl);
				}
				updateTexture = false;
			}
			
			for (LayerInterface layer : layers.getLayers()) {
				System.out.println("renderLayer");
				if (layer.isVisible()) {
					if (layer instanceof Layer)
					this.displayLayer(gl, (Layer)layer);
				}
			}
			gl.glPopMatrix();
			// renderRect(gl);
		}
		
		// empty screen
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		
		if (animations.size() > 0){
			for (RenderAnimation animation : animations){
				animation.render(gl, canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
				this.canvas.repaint(20);
			}
		}
		
		/*if (layers.getLayerCount() <= 0 && animations.size() <= 0) {
			splashScreen.render(gl, canvas.getSurfaceWidth(), canvas.getSurfaceHeight());
		}*/

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
		//splashScreen = new NoImageScreen(gl);
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
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
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
	public void activeLayerChanged(LayerInterface layer) {
		// TODO Auto-generated method stub

	}
	
	public void addRenderAnimation(RenderAnimation renderAnimation){
		this.animations.add(renderAnimation);
		this.canvas.repaint();
	}
	
	public void removeRenderAnimation(RenderAnimation renderAnimation){
		this.animations.remove(renderAnimation);
		this.canvas.repaint();
	}
}
