package org.helioviewer.jhv.opengl;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.IconBank;

import com.jogamp.common.nio.Buffers;

public class CenterLoadingScreen implements RenderAnimation {

	private int texture;
	private Dimension dimension;
	private final double FPS = 60;
	private final int NUMBER_OF_CIRCLE = 12;
	private final int NUMBER_OF_VISIBLE_CIRCLE = 2;
	private final int POINT_OF_CIRCLE = 36;
	private int verticesSize;
	private final double FACTOR = 8;

	private final float RADIUS = 300;
	private final float CIRCLE_RADIUS = 30;
	private int[] buffers;
	private int vertices;
	private int indices;
	private int color;
	private float CIRCLE_COLOR = 192 / 255f;
	private int indicesSize;

	
	public CenterLoadingScreen() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				OpenGLHelper.glContext.makeCurrent();
				GL2 gl = OpenGLHelper.glContext.getGL().getGL2();
				try {
					BufferedImage image = IconBank
							.getImage(IconBank.JHVIcon.LOADING_BIG);
					texture = OpenGLHelper.createTexture(gl, image);
					OpenGLHelper.updateTexture(gl, texture, image);
					dimension = new Dimension(image.getWidth(), image
							.getHeight());
				} catch (Exception e) {
					e.printStackTrace();
				}
				initCircleVBO(gl);
			}
		});
	}

	@Override
	public void render(GL2 gl, double canvasWidth, double canvasHeight) {
		System.out.println("renderCenterLoadingScreen");

		gl.glUseProgram(0);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);

		double aspect = canvasWidth / canvasHeight;

		double width = aspect > 1 ? dimension.getWidth() * FACTOR : dimension
				.getWidth() * aspect * FACTOR;
		double height = aspect < 1 ? dimension.getHeight() * FACTOR : dimension
				.getHeight() / aspect * FACTOR;
		width /= 2.0;
		height /= 2.0;
		double imageWidth = dimension.getWidth() / 2.0;
		double imageHeight = dimension.getHeight() / 2.0;
		gl.glDisable(GL2.GL_DEPTH_TEST);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrtho(-width, width, -height, height, 10, -10);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glColor3f(1, 0, 0);

		/*
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_LINEAR);
		// gl.glColor3f(1, 0, 0);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex2d(-imageWidth, imageHeight);
		gl.glTexCoord2f(1, 0);
		gl.glVertex2d(imageWidth, imageHeight);
		gl.glTexCoord2f(1, 1);
		gl.glVertex2d(imageWidth, -imageHeight);
		gl.glTexCoord2f(0, 1);
		gl.glVertex2d(-imageWidth, -imageHeight);
		gl.glEnd();
		*/
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);

		renderCircles(gl, 0);

	}

	private void renderCircles(GL2 gl, double t) {
		// gl.glPushMatrix();
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
		//gl.glEnable(GL2.GL_BLEND);
		//gl.glBlendFunc(GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		//gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
		//gl.glEnable(GL2.GL_COLOR_MATERIAL);		
		//gl.glCullFace(GL2.GL_BACK);

		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertices);
		gl.glVertexPointer(2, GL2.GL_FLOAT, 0, 0);
		gl.glColor3d(1, 0, 0);
		//gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, color);
		//gl.glColorPointer(3, GL2.GL_FLOAT, 0, 0);
				
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indices);
		gl.glDrawElements(GL2.GL_TRIANGLES, indicesSize, GL2.GL_UNSIGNED_INT, 0);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
}

	@Override
	public void isFinish() {
		// TODO Auto-generated method stub

	}

	public void initCircleVBO(GL2 gl) {
		System.out.println("initCircle");

		IntBuffer indices = Buffers
				.newDirectIntBuffer((POINT_OF_CIRCLE) * 3 * NUMBER_OF_VISIBLE_CIRCLE);
		FloatBuffer vertices = Buffers.newDirectFloatBuffer(((POINT_OF_CIRCLE) * 2 + 2)
				* NUMBER_OF_VISIBLE_CIRCLE);
		FloatBuffer colors = Buffers.newDirectFloatBuffer((((POINT_OF_CIRCLE) * 3))
				* NUMBER_OF_VISIBLE_CIRCLE);



		for (int j = 0; j  < NUMBER_OF_VISIBLE_CIRCLE; j ++){
			float alpha = (192 - (192 * ((float) j / NUMBER_OF_VISIBLE_CIRCLE))) / 255.f;
			double test = j / (double) NUMBER_OF_CIRCLE;
			System.out.println("test : " + test);
			float y = (float) Math.cos(test * 2 * Math.PI) * RADIUS;
			float x = (float) Math.sin(test * 2 * Math.PI) * RADIUS;
			vertices.put(x);
			vertices.put(y);
			int middle = (POINT_OF_CIRCLE + 1) * j;
			for (int i = 0; i < POINT_OF_CIRCLE; i++){
				vertices.put((float) (Math.cos(i/(double)POINT_OF_CIRCLE*2*Math.PI))*CIRCLE_RADIUS + x);
				vertices.put((float) (Math.sin(i/(double)POINT_OF_CIRCLE*2*Math.PI))*CIRCLE_RADIUS + y);
				System.out.println("vertices x "+(i+1)+": " + ((Math.cos(i/(double)POINT_OF_CIRCLE*2*Math.PI))*CIRCLE_RADIUS + x));
				System.out.println("vertices y "+(i+1)+": " + ((Math.sin(i/(double)POINT_OF_CIRCLE*2*Math.PI))*CIRCLE_RADIUS + x));
			}
			
			for (int i = 1; i <= POINT_OF_CIRCLE; i++) {
				int idx1 = i + j * POINT_OF_CIRCLE;
				int idx2 = i+1 > POINT_OF_CIRCLE ? 1 + j * POINT_OF_CIRCLE : i+1 + j * POINT_OF_CIRCLE;
				indices.put(idx1);
				indices.put(middle);
				indices.put(idx2);
				System.out.println("i"+i+" : " + (idx1));
				System.out.println("i"+i+" : " + (middle));
				System.out.println("i"+i+" : " + idx2);
				colors.put(CIRCLE_COLOR);
				colors.put(CIRCLE_COLOR);
				colors.put(CIRCLE_COLOR);

				//colors.put(alpha);
				System.out.println("alpha : " + alpha);
				}
		}
		vertices.flip();
		colors.flip();
		indices.flip();

		
		int[] buffer = new int[4];
		gl.glGenBuffers(4, buffer, 0);

		this.vertices = buffer[0];
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.vertices);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertices.limit() * Buffers.SIZEOF_FLOAT, vertices,
				GL.GL_STATIC_DRAW);
		verticesSize = vertices.limit();

		this.indices = buffer[1];
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.indices);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, indices.limit() * Buffers.SIZEOF_INT, indices,
				GL.GL_STATIC_DRAW);
		this.indicesSize = indices.limit();

		this.color = buffer[2];
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.color);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, colors.limit() * Buffers.SIZEOF_FLOAT, colors,
				GL.GL_STATIC_DRAW);
		
	}

}
