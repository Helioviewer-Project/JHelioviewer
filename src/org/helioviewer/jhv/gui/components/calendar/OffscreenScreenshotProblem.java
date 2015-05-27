package org.helioviewer.jhv.gui.components.calendar;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.awt.ImageUtil;

public class OffscreenScreenshotProblem {
	private static GLWindow window;

	public static void main(String[] args) {
		GLProfile profile = GLProfile.getDefault();
		GLCapabilities capabilities = new GLCapabilities(profile);
		window = GLWindow.create(capabilities);
		Canvas canvas = new Canvas();
		window.addGLEventListener(canvas);
		final NewtCanvasAWT newtCanvas = new NewtCanvasAWT(window);
		final JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(newtCanvas, BorderLayout.CENTER);
		final JFrame frame = new JFrame("Offscreen Screenshot Problem");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(panel);
		frame.setSize(600, 600);
		frame.setVisible(true);
	}

	private static class Canvas implements GLEventListener {
		private int width;
		private int height;
		private boolean screenshotSaved;

		public Canvas() {
			super();
			screenshotSaved = false;
		}

		@Override
		public void reshape(GLAutoDrawable drawable, int x, int y, int width,
				int height) {
			final GL2 gl = drawable.getGL().getGL2();
			gl.glViewport(0, 0, width, height);
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();
			gl.glOrthof(0, width, 0, height, 0, 1);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			this.width = width;
			this.height = height;
		}

		@Override
		public void init(GLAutoDrawable drawable) {
			final GL2 gl = drawable.getGL().getGL2();
			gl.glClearColor(0, 0, 0, 0);
		}

		@Override
		public void dispose(GLAutoDrawable drawable) {

		}

		@Override
		public void display(GLAutoDrawable drawable) {
			final GL2 gl = drawable.getGL().getGL2();
			draw(gl);
			takeScreenshot(gl);
		}

		private void draw(GL2 gl) {
			gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
			gl.glLoadIdentity();
			gl.glColor3f(0.0f, 0.0f, 0.0f);
			// Set up the stencil.
			gl.glEnable(GL2.GL_STENCIL_TEST);
			gl.glClear(GL2.GL_STENCIL_BUFFER_BIT);
			gl.glStencilFunc(GL2.GL_NEVER, 1, 1);
			gl.glStencilOp(GL2.GL_REPLACE, GL2.GL_REPLACE, GL2.GL_REPLACE);
			// Draw a small square into the stencil.
			gl.glBegin(GL2.GL_POLYGON);
			gl.glVertex3f(100.0f, 100.0f, 0.0f);
			gl.glVertex3f(200.0f, 100.0f, 0.0f);
			gl.glVertex3f(200.0f, 200.0f, 0.0f);
			gl.glVertex3f(100.0f, 200.0f, 0.0f);
			gl.glEnd();
			// Set up the stencil.
			gl.glStencilFunc(GL2.GL_NOTEQUAL, 1, 1);
			gl.glStencilOp(GL2.GL_KEEP, GL2.GL_KEEP, GL2.GL_KEEP);
			gl.glColor3f(0.0f, 0.0f, 1.0f);
			// Fill the screen with blue.
			gl.glBegin(GL2.GL_POLYGON);
			gl.glVertex3f(0.0f, 0.0f, 0.0f);
			gl.glVertex3f(width, 0.0f, 0.0f);
			gl.glVertex3f(width, height, 0.0f);
			gl.glVertex3f(0.0f, height, 0.0f);
			gl.glEnd();
			gl.glDisable(GL2.GL_STENCIL_TEST);
		}

		private void takeScreenshot(GL2 gl) {
			if (!screenshotSaved) {
				GLProfile profile = GLProfile.get(GLProfile.GL2);
				GLCapabilities capabilities = new GLCapabilities(profile);
				capabilities.setDoubleBuffered(false);
				capabilities.setOnscreen(false);
				capabilities.setHardwareAccelerated(true);
				GLDrawable offscreenDrawable = window.getFactory()
						.createOffscreenDrawable(null, capabilities, null,
								width, height);
				offscreenDrawable.setRealized(true);
				GLContext offscreenContext = offscreenDrawable.createContext(gl
						.getContext());
				offscreenContext.makeCurrent();
				GL2 offscreenGL = offscreenContext.getGL().getGL2();
				offscreenGL.glViewport(0, 0, width, height);
				offscreenGL.glMatrixMode(GL2.GL_PROJECTION);
				offscreenGL.glLoadIdentity();
				offscreenGL.glOrthof(0, width, 0, height, 0, 1);
				offscreenGL.glMatrixMode(GL2.GL_MODELVIEW);
				offscreenGL.glLoadIdentity();
				offscreenGL.glClearColor(0, 0, 0, 0);
				offscreenGL.glClear(GL2.GL_COLOR_BUFFER_BIT
						| GL2.GL_DEPTH_BUFFER_BIT);
				draw(offscreenGL);
				BufferedImage image = OffscreenScreenshotProblem.readToBufferedImage(0, 0, width,
						height, false);
				offscreenContext.release();
				gl.getContext().makeCurrent();
				System.out.println(System.getProperty("user.dir"));
				File imageFile = new File("/Users/stefanmeier/Documents/Test.png");
				try {
					ImageIO.write(image, "png", imageFile);
				} catch (Exception e) {
					System.err.println(e.toString());
				}
				screenshotSaved = true;
			}
		}
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
}