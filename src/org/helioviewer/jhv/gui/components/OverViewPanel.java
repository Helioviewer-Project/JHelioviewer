package org.helioviewer.jhv.gui.components;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JPanel;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.SOHOLUTFilter;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DCameraListener;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imageformat.ImageFormat;
import org.helioviewer.jhv.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.jhv.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.jhv.viewmodel.imagetransport.Short16ImageTransport;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DCoordinateSystemView;
import org.helioviewer.jhv.viewmodel.view.opengl.GLFilterView;
import org.helioviewer.jhv.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.jhv.viewmodel.viewport.StaticViewport;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;

public class OverViewPanel extends JPanel implements LayersListener, GLEventListener, GL3DCameraListener, MouseListener, MouseMotionListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6052916320152982825L;
	GLJPanel canvas = new GLJPanel();
	JHVJPXView lastLayer;
	GLTextureHelper textureHelper = new GLTextureHelper();
	private ArrayList<JHVJPXView> layers;
	private int texID;
	private int lutTexID;
	private boolean updateTexture;
	private GL3DCamera camera;
	private int shaderprogram;
	private HashMap<String, Integer> lutMap;
	private int nextAvaibleLut = 0;
	private int currentLut = 0;
	private long lastTime;
	private int invertedLut = 0;
	
	public enum MOVE_MODE{
		UP, DOWN
	}
	
	public OverViewPanel() {
		this.setPreferredSize(new Dimension(200, 200));
		layers = new ArrayList<JHVJPXView>();
		lutMap = new HashMap<String, Integer>();
    	this.add(canvas);
    	this.canvas.setPreferredSize(new Dimension(200, 200));
		this.canvas.addMouseListener(this);
		this.canvas.addMouseMotionListener(this);
		loadLutFromFile("/UltimateLookupTable.txt");
        
	}

	@Override
	public void layerAdded(int idx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void layerRemoved(View oldView, int oldIdx) {
		/*if (oldIdx >= 0 && oldIdx < layers.size() && layers.size() > 0 && lastViewToDelete != oldView){
			oldIdx = layers.size() - oldIdx - 1;
			layers.remove(oldIdx);
			this.lastViewToDelete = oldView;
		}*/
	}

	@Override
	public void layerChanged(int idx) {
	}

	@Override
	public void activeLayerChanged(int idx) {
		if (idx >= 0 && idx < LayersModel.getSingletonInstance().getNumLayers() && layers.size() > 0){
			if (LinkedMovieManager.getActiveInstance().getMasterMovie() != null)
				this.lastTime = LinkedMovieManager.getActiveInstance().getMasterMovie().getCurrentFrameDateTime().getMillis();
			idx = LayersModel.getSingletonInstance().getNumLayers() - idx -1;
				lastLayer = layers.get(idx);
			View lastView = LayersModel.getSingletonInstance().getActiveView();
			// Just a hack
			GLFilterView opacityFilterView = lastView.getAdapter(GLFilterView.class).getAdapter(GLFilterView.class);
			//Filter opacityFilter = opacityFilterView.getFilter();
			GLFilterView lutView = (GLFilterView)opacityFilterView.getView();
			SOHOLUTFilter lutFilter = (SOHOLUTFilter)lutView.getFilter();
			this.setCurrentLutByName(lutFilter.getLUT(), true);
			this.invertedLut = lutFilter.isInverted();
			if (lastLayer.getImageData() != null){
			this.updateTexture = true;
			}
		}
		else {
			lastLayer = null;
		}
		this.canvas.repaint();
	}

	@Override
	public void viewportGeometryChanged() {
		
	}

	@Override
	public void timestampChanged(int idx) {
		if (LinkedMovieManager.getActiveInstance().getMasterMovie() != null)
			if (this.lastTime != LinkedMovieManager.getActiveInstance().getMasterMovie().getCurrentFrameDateTime().getMillis()){
				this.lastTime = LinkedMovieManager.getActiveInstance().getMasterMovie().getCurrentFrameDateTime().getMillis();
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

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glActiveTexture(GL2.GL_TEXTURE0);
		if (lastLayer != null){
			
		if (this.updateTexture){
        this.createTexture(gl, this.lastLayer.getMetadata().getPhysicalRegion(), this.lastLayer.getImageData());
        updateTexture = false;
		}		
        
		Vector2d lowerleftCorner = this.lastLayer.getMetadata().getPhysicalRegion().getLowerLeftCorner();
		Vector2d size = this.lastLayer.getMetadata().getPhysicalRegion().getSize();
		gl.glDisable(GL2.GL_DEPTH_TEST);
		if (size.x <= 0 || size.y <= 0) {
			return;
		}
		
		float x0 = (float) lowerleftCorner.x;
		float y0 = (float) lowerleftCorner.y;
		float x1 = x0 + (float) size.x;
		float y1 = y0 + (float) size.y;

		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);		

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glViewport(0, 0, this.canvas.getSurfaceWidth(), this.canvas.getSurfaceHeight());
		gl.glLoadIdentity();
		gl.glOrtho(x0, x1, y0, y1, 10, -10);
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glColor3f(1, 1, 1);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
		
		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, lutTexID);

		gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glEnable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		
		gl.glUseProgram(shaderprogram);
		
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "texture"), 0);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "lut"), 1);
		gl.glUniform1f(gl.glGetUniformLocation(shaderprogram, "currentLut"), currentLut);
		gl.glUniform1i(gl.glGetUniformLocation(shaderprogram, "inverted"), this.invertedLut);

		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0.0f,1.0f);
		gl.glVertex2d(x0,y0);
		gl.glTexCoord2f(1.0f,1.0f);
		gl.glVertex2d(x1,y0);
		gl.glTexCoord2f(1.0f,0.0f);
		gl.glVertex2d(x1,y1);
		gl.glTexCoord2f(0.0f,0.0f);
		gl.glVertex2d(x0,y1);
		
		gl.glEnd();
		gl.glUseProgram(0);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);		

		renderRect(gl);
		}
	}

	private void renderRect(GL2 gl){
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_2D || calculateAngleToActiveLayer() < 0.01){
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_BLEND);
		gl.glShadeModel(GL2.GL_FLAT);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glColor3d(1, 1, 136.0/255.0);
		
		//lastLayer.getMetaData().getMaskRotation
		
		Vector3d translation = camera.getTranslation();
		double height = Math.tan(Math.toRadians(camera.getFOV())) * translation.z;
		double width = height * camera.getAspect();
		double x1 = width/2.0;
		double y1 = height/2.0;
		double x0 = -x1;
		double y0 = -y1;
		x0 -= translation.x;
		x1 -= translation.x;
		y0 -= translation.y;
		y1 -= translation.y;
		
		gl.glBegin(GL2.GL_LINE_STRIP);
			gl.glVertex2d(x0, y0);
			gl.glVertex2d(x0, y1);
			gl.glVertex2d(x1, y1);
			gl.glVertex2d(x1, y0);
			gl.glVertex2d(x0, y0);
		gl.glEnd();
		}
		
	}
	
	@Override
	public void dispose(GLAutoDrawable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
		
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		
		int tmp[] = new int[1];
		gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, tmp, 0);
		gl.glGenTextures(1, tmp, 0);
		texID = tmp[0];
		
		this.initShaders(gl);
		
		this.repaint();
		
		this.prepareLut(gl);
		
	}

	@Override
	public void reshape(GLAutoDrawable arg0, int arg1, int arg2, int arg3,
			int arg4) {
		// TODO Auto-generated method stub
		
	}
	
	private void createTexture(GL2 gl, Region region, ImageData imageData){
		int bitsPerPixel = imageData.getImageTransport().getNumBitsPerPixel();
		Buffer buffer;

		switch (bitsPerPixel) {
		case 8:
			buffer = ByteBuffer.wrap(((Byte8ImageTransport) imageData
					.getImageTransport()).getByte8PixelData());
			break;
		case 16:
			buffer = ShortBuffer.wrap(((Short16ImageTransport) imageData
					.getImageTransport()).getShort16PixelData());
			break;
		case 32:
			buffer = IntBuffer.wrap(((Int32ImageTransport) imageData
					.getImageTransport()).getInt32PixelData());
			break;
		default:
			buffer = null;
		}
				
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, bitsPerPixel >> 3);

		textureHelper.checkGLErrors(gl, this + ".afterPixelStore");
		ImageFormat imageFormat = imageData.getImageFormat();
		int internalFormat = GLTextureHelper.mapImageFormatToInternalGLFormat(imageFormat);
		int inputFormat = GLTextureHelper.mapImageFormatToInputGLFormat(imageFormat);
		int width = imageData.getWidth();
		int height = imageData.getHeight();
		int inputType = GLTextureHelper.mapBitsPerPixelToGLType(bitsPerPixel);
		
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
		
		int width2 = nextPowerOfTwo(width);
		int height2 = nextPowerOfTwo(height);

		int bpp = 3;
		switch (inputFormat) {
		case GL2.GL_LUMINANCE:
		case GL2.GL_ALPHA:
			bpp = 1;
			break;
		case GL2.GL_LUMINANCE_ALPHA:
			bpp = 2;
			break;
		case GL2.GL_RGB:
			bpp = 3;
			break;
		case GL2.GL_RGBA:
		case GL2.GL_BGRA:
			bpp = 4;
			break;

		default:
			throw new RuntimeException("" + inputFormat);
		}

		switch (inputType) {
		case GL2.GL_UNSIGNED_BYTE:
			bpp *= 1;
			break;
		case GL2.GL_UNSIGNED_SHORT:
		case GL2.GL_UNSIGNED_SHORT_5_6_5:
		case GL2.GL_UNSIGNED_SHORT_4_4_4_4:
		case GL2.GL_UNSIGNED_SHORT_5_5_5_1:
			bpp *= 2;
			break;
		case GL2.GL_UNSIGNED_INT_8_8_8_8_REV:
			bpp *= 4;
			break;
		default:
			throw new RuntimeException("" + inputType);
		}

		ByteBuffer b = ByteBuffer.allocate(width2 * height2 * bpp);
		b.limit(width2 * height2 * bpp);

		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width2,
				height2, 0, inputFormat, inputType, b);

		textureHelper.checkGLErrors(gl, this + ".glTexImage2d");
		if (buffer != null) {
			gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, width, height,
					inputFormat, inputType, buffer);
		}
		textureHelper.checkGLErrors(gl, this + ".glTexSubImage2d");

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
				GL2.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
				GL2.GL_CLAMP_TO_BORDER);		
	}
	
	public void activate(GLContext context){
		this.canvas.setSharedContext(context);
		this.canvas.addGLEventListener(this);
		this.camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();
		this.camera.addCameraListener(this);
		this.canvas.repaint();
	}


	public void setLayer(JHVJPXView layerOverView) {
		Viewport viewport = StaticViewport.createAdaptedViewport(256, 256);
		layerOverView.setViewport(viewport, null);
		layerOverView.setRegion(layerOverView.getMetaData().getPhysicalRegion(), null);
		layers.add(layerOverView);
	}
	
	public int nextPowerOfTwo(int input) {
		int output = 1;
		while (output < input) {
			output <<= 1;
		}
		return output;
	}

	@Override
	public void cameraMoved(GL3DCamera camera) {
		this.canvas.repaint();
	}

	@Override
	public void cameraMoving(GL3DCamera camera) {
		this.canvas.repaint();
	}
	
	private void initShaders(GL2 gl){
		int vertexShader = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		int fragmentShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
		
		String vertexShaderSrc = loadShaderFromFile("/shader/OverViewVertex.glsl");
		String fragmentShaderSrc = loadShaderFromFile("/shader/OverViewFragment.glsl");
		
		gl.glShaderSource(vertexShader, 1, new String[] {vertexShaderSrc}, (int[]) null, 0);
		gl.glCompileShader(vertexShader);
		
		gl.glShaderSource(fragmentShader, 1, new String[] {fragmentShaderSrc}, (int[]) null, 0);
		gl.glCompileShader(fragmentShader);
		
		this.shaderprogram = gl.glCreateProgram();
		gl.glAttachShader(shaderprogram, vertexShader);
		gl.glAttachShader(shaderprogram, fragmentShader);
		gl.glLinkProgram(shaderprogram);
		gl.glValidateProgram(shaderprogram);
		
		gl.glUseProgram(shaderprogram);
	}
	
	private String loadShaderFromFile(String shaderName){
		StringBuilder shaderCode = new StringBuilder();
		String line = null;
		
		BufferedReader br=new BufferedReader(new InputStreamReader(OverViewPanel.class.getResourceAsStream(shaderName)));
		try {
			while ((line = br.readLine()) != null){
				shaderCode.append(line);
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return shaderCode.toString();
	}

	private void prepareLut(GL2 gl) {
		loadLutTexture(gl, "/UltimateLookupTable.png");
	}

	private void loadLutFromFile(String lutTxtName){
		String line = null;
		
		try {
	        BufferedReader br=new BufferedReader(new InputStreamReader(OverViewPanel.class.getResourceAsStream(lutTxtName),"UTF-8"));
			while ((line = br.readLine()) != null){
				lutMap.put(line, this.nextAvaibleLut++);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void loadLutTexture(GL2 gl, String lutImageName){
		try {
			int tmp[] = new int[1];
			gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, tmp, 0);
			gl.glGenTextures(1, tmp, 0);
			lutTexID = tmp[0];
			BufferedImage bufferedImage = ImageIO.read(OverViewPanel.class.getResourceAsStream(lutImageName));
			
			ByteBuffer buffer = readPixels(bufferedImage, false);

		
			gl.glEnable(GL2.GL_TEXTURE_2D);	
						
			gl.glBindTexture(GL2.GL_TEXTURE_2D, lutTexID);
			
			ByteBuffer b = ByteBuffer.allocate(256*256*3);
			b.limit(256*256*3);

			int internalFormat = GL2.GL_RGB;
			int inputFormat = GL2.GL_RGB;
			int inputType = GL2.GL_UNSIGNED_BYTE;
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, 256,
					256, 0, inputFormat, inputType, b);

			textureHelper.checkGLErrors(gl, this + ".glTexImage2d");
			// Log.debug("GLTextureHelper.genTexture2D: Width="+width+", Height="+height+" Width2="+width2+", Height2="+height2);
			
			if (buffer != null) {
				gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
						inputFormat, inputType, buffer);
			}
			textureHelper.checkGLErrors(gl, this + ".glTexSubImage2d");
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
					GL2.GL_NEAREST);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
					GL2.GL_NEAREST);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
					GL2.GL_CLAMP);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
					GL2.GL_CLAMP);
			

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ByteBuffer readPixels(BufferedImage image, boolean storeAlphaChannel) {
		int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = ByteBuffer.allocate(image.getWidth() * image.getHeight() * 3); //4 for RGBA, 3 for RGB
        
        for(int y = 0; y < image.getHeight(); y++){
            for(int x = 0; x < image.getWidth(); x++){
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));     
                buffer.put((byte) ((pixel >> 8) & 0xFF));      
                buffer.put((byte) (pixel & 0xFF));              
            }
        }

        buffer.flip(); 
        
	    return buffer;
	}
	
	public void setCurrentLutByName(String name, boolean inverted){
		this.invertedLut = 0;
		if (lutMap != null){
			this.currentLut = lutMap.get(name);
			if (inverted) this.invertedLut = 1;
			
			this.canvas.repaint();
		}
	}
	
	public void setCurrentLutByIndex(int idx){
		this.currentLut = idx;
		this.canvas.repaint();
	}

	private void setPan(int x, int y){
		Dimension canvasSize = this.canvas.getSize();
		double xTranslation = -(x / canvasSize.getWidth() - 0.5) * this.lastLayer.getMetaData().getPhysicalImageWidth();
		double yTranslation = (y / canvasSize.getHeight() - 0.5) * this.lastLayer.getMetadata().getPhysicalImageHeight();
		this.camera.setPanning(xTranslation, yTranslation);
		this.camera.updateCameraTransformation();
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		GL3DCameraSelectorModel.getInstance().rotateToCurrentLayer(0);
		this.setPan(e.getX(), e.getY());
	}

	@Override
	public void mousePressed(MouseEvent e) {
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

	@Override
	public void mouseDragged(MouseEvent e) {
		GL3DCameraSelectorModel.getInstance().rotateToCurrentLayer(0);
		this.setPan(e.getX(), e.getY());
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	public void removeLayer(int idx) {
		layers.remove(idx);
	}
	
	public void moveActiveLayer(MOVE_MODE change){
		int oldIndex = 0;
		for (int i = 0; i < layers.size(); i++){
			if (this.layers.get(i) == this.lastLayer){
				oldIndex = i;
				break;
			}
		}
		// up
		if (change == MOVE_MODE.UP && oldIndex < (layers.size()-1)){
			int newIndex = oldIndex + 1;
			this.layers.remove(lastLayer);
			this.layers.add(newIndex, lastLayer);
		}
		else if(change == MOVE_MODE.DOWN && oldIndex > 0){
			int newIndex = oldIndex - 1;
			this.layers.remove(lastLayer);
			this.layers.add(newIndex, lastLayer);
		}
	}

	private double calculateAngleToActiveLayer(){
		Matrix4d camTrans = camera.getRotation().toMatrix().inverse();
		Vector3d camDirection = new Vector3d(0, 0, 1);
		
		View view = LayersModel.getSingletonInstance().getActiveView();
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
}
