package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.NewLayer;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.RenderAnimation;
import org.helioviewer.jhv.opengl.camera.CameraInteraction;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.opengl.camera.CameraMode.MODE;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

import com.jogamp.opengl.DefaultGLCapabilitiesChooser;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.awt.ImageUtil;

public class MovieExportOffscreenRenderer extends MainPanel{
	
	
	private int tileWidth;
	private int tileHeight;
	
	private int width;
	private int height;
	
	private MainPanel synchronizedView;
	
	private static final int MAX_TILE_WIDTH = 2048;
	private static final int MAX_TILE_HEIGHT = 2048;
	
	
	public MovieExportOffscreenRenderer(int width, int height) {
		super();
		tileWidth = width < MAX_TILE_WIDTH ? width : MAX_TILE_WIDTH;
		tileHeight = height < MAX_TILE_HEIGHT ? height
				: MAX_TILE_HEIGHT;
		this.width = width;
		this.height = height;

	}
	
	private GLAutoDrawable createOffscreenGL(){
		GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile
				.getDefault());

		GLCapabilities capabilities = new GLCapabilities(GLProfile.getMaxFixedFunc(true));
		capabilities.setDoubleBuffered(false);
		capabilities.setOnscreen(false);
		capabilities.setHardwareAccelerated(true);
		capabilities.setStencilBits(8);

		GLOffscreenAutoDrawable drawable = factory.createOffscreenAutoDrawable(null,capabilities,null,width,height); 
		drawable.setRealized(true);
		GLContext context = drawable.createContext(GLContext.getCurrent());
		context.makeCurrent();
		System.out.println(context.getGL().getGL2());
		return drawable;
	}
	
	public BufferedImage getBufferedImage(ArrayList<String> descriptions){
		System.out.println("width : " + width);
		BufferedImage screenshot = new BufferedImage(width, height,
				BufferedImage.TYPE_3BYTE_BGR);
		ByteBuffer.wrap(((DataBufferByte) screenshot.getRaster()
				.getDataBuffer()).getData());
		GLAutoDrawable drawable = createOffscreenGL();
		GL2 offscreenGL = GLContext.getCurrentGL().getGL2();
		
		this.display(drawable);
		

		return MovieExportOffscreenRenderer.readToBufferedImage(0, 0, width, height, false);
	}
	
	public static BufferedImage readToBufferedImage(int x, int y, int width,
			int height, boolean alpha) {
		int bufImgType = (alpha ? BufferedImage.TYPE_4BYTE_ABGR
				: BufferedImage.TYPE_3BYTE_BGR);
		int readbackType = (alpha ? GL2.GL_ABGR_EXT : GL2.GL_BGR);

		// Allocate necessary storage
		BufferedImage image = new BufferedImage(width, height, bufImgType);

		GLContext glc = GLContext.getCurrent();
		GL gl = glc.getGL();

		// Set up pixel storage modes
		GLPixelStorageModes psm = new GLPixelStorageModes();
		psm.setPackAlignment(gl, 1);

		// read the BGR values into the image
		gl.glReadPixels(x, y, width, height, readbackType, GL.GL_UNSIGNED_BYTE,
				ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData()));

		// Restore pixel storage modes
		psm.restore(gl);

		if (glc.getGLDrawable().isGLOriented()) {
			// Must flip BufferedImage vertically for correct results
			ImageUtil.flipImageVertically(image);
		}
		return image;
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = GLContext.getCurrentGL().getGL2();
		System.out.println("repaint MovieExport");
		gl.getContext().makeCurrent();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glViewport(0, 0, width, height);
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(-1, 1, -1 / aspect, 1 / aspect, 10, -10);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		
		gl.glColor3d(0, 0, 1);
		gl.glBegin(GL2.GL_TRIANGLES);
		gl.glVertex2d(-1, -1);
		gl.glVertex2d(1, 1);
		gl.glVertex2d(-1, 1);
		gl.glEnd();
		

	}
	
	public void setSynchronizedView(MainPanel synchronizedView){
		this.synchronizedView = synchronizedView;
	}
}
