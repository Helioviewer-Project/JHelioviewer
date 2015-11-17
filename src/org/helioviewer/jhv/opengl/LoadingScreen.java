package org.helioviewer.jhv.opengl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

public class LoadingScreen
{
	private static final double TOTAL_SEC_4_ONE_ROTATION = 2;
	private static final int NUMBER_OF_CIRCLE = 32;
	private static final int NUMBER_OF_VISIBLE_CIRCLE = 12;
	private static final int POINT_OF_CIRCLE = 36;

	private static final float RADIUS = 0.43f;
	private static final float CIRCLE_RADIUS = 0.04f;
	private static final float DEFAULT_X_OFFSET = -0.003f;
	private static final float DEFAULT_Y_OFFSET = 0.027f;
	
	private static int vertices;
	private static int indices;
	private static int color;
	private static final float CIRCLE_COLOR = 192 / 255f;
	private static int indicesSize;
	private static Texture openGLHelper;

	static
	{
		openGLHelper = new Texture();
		openGLHelper.upload(IconBank.getImage(JHVIcon.LOADING_BIG));
		initCircleVBO(GLContext.getCurrentGL().getGL2());
	}

	public static void render(GL2 gl)
	{
		gl.glUseProgram(0);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		
		gl.glDisable(GL2.GL_DEPTH_TEST);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glOrtho(-0.5, 0.5, -0.5, 0.5, 10, -10);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glColor4f(1, 1, 1, 1);
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		
		gl.glBindTexture(GL2.GL_TEXTURE_2D, openGLHelper.openGLTextureId);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		// gl.glColor3f(1, 0, 0);
		long time = System.currentTimeMillis();
		long counter = (time / 600) % 4;
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, counter * 0.25f);
		gl.glVertex2d(-0.2, 0.5);
		gl.glTexCoord2f(1, counter * 0.25f);
		gl.glVertex2d(0.8, 0.5);
		gl.glTexCoord2f(1, (counter + 1) * 0.25f);
		gl.glVertex2d(0.8, 0.25);
		gl.glTexCoord2f(0, (counter + 1) * 0.25f);
		gl.glVertex2d(-0.2, 0.25);
		gl.glEnd();
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);

		//renderCircles(gl);
	}

	@SuppressWarnings("unused")
	private static void renderCircles(GL2 gl)
	{
		// gl.glPushMatrix();
		double t = System.currentTimeMillis() / (TOTAL_SEC_4_ONE_ROTATION*1000.0);
		t = t - (int) t;
		//t /= FPS;
		t = t - (int) t;
		gl.glTranslated(DEFAULT_X_OFFSET, DEFAULT_Y_OFFSET, 0);
		gl.glRotated(t*360, 0, 0, -1);
		gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
		gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_COLOR_MATERIAL);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
		gl.glEnable(GL2.GL_COLOR_MATERIAL);		
		gl.glCullFace(GL2.GL_BACK);

		if (vertices > 0){
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, vertices);
		gl.glVertexPointer(2, GL2.GL_FLOAT, 0, 0);
		
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, color);
		gl.glColorPointer(4, GL2.GL_FLOAT, 0, 0);
				
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, indices);
		gl.glDrawElements(GL2.GL_TRIANGLES, indicesSize, GL2.GL_UNSIGNED_INT, 0);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		}
		gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
	}

	private static void initCircleVBO(GL2 gl) {

		IntBuffer indices = Buffers
				.newDirectIntBuffer((POINT_OF_CIRCLE) * 3 * NUMBER_OF_VISIBLE_CIRCLE);
		FloatBuffer vertices = Buffers.newDirectFloatBuffer(((POINT_OF_CIRCLE) * 2 + 2)
				* NUMBER_OF_VISIBLE_CIRCLE);
		FloatBuffer colors = Buffers.newDirectFloatBuffer(((POINT_OF_CIRCLE) * 4 + 4)
				* NUMBER_OF_VISIBLE_CIRCLE);



		for (int j = 0; j  < NUMBER_OF_VISIBLE_CIRCLE; j ++){
			float alpha = (192 - (192 * ((float) j / NUMBER_OF_VISIBLE_CIRCLE))) / 255.f;
			double test = j / (double) NUMBER_OF_CIRCLE;
			
			float x = (float) Math.cos(test * 2 * Math.PI) * RADIUS;
			float y = (float) Math.sin(test * 2 * Math.PI) * RADIUS;
			vertices.put(x);
			vertices.put(y);
			
			colors.put(CIRCLE_COLOR);
			colors.put(CIRCLE_COLOR);
			colors.put(CIRCLE_COLOR);
			colors.put(alpha);
			int middle = (POINT_OF_CIRCLE + 1) * j;
			for (int i = 0; i < POINT_OF_CIRCLE; i++){
				vertices.put((float) (Math.cos(i/(double)POINT_OF_CIRCLE*2*Math.PI))*CIRCLE_RADIUS + x);
				vertices.put((float) (Math.sin(i/(double)POINT_OF_CIRCLE*2*Math.PI))*CIRCLE_RADIUS + y);
				colors.put(CIRCLE_COLOR);
				colors.put(CIRCLE_COLOR);
				colors.put(CIRCLE_COLOR);
				colors.put(alpha);
			}
			
			for (int i = 1; i <= POINT_OF_CIRCLE; i++) {
				int idx1 = i + j * (POINT_OF_CIRCLE + 1);
				int idx2 = i+1 > POINT_OF_CIRCLE ? 1 + j * (POINT_OF_CIRCLE+1) : i+1 + j * (POINT_OF_CIRCLE+1);
				indices.put(idx1);
				indices.put(middle);
				indices.put(idx2);
				}
		}
		vertices.flip();
		colors.flip();
		indices.flip();
		
		int[] buffer = new int[3];
		gl.glGenBuffers(3, buffer, 0);

		LoadingScreen.vertices = buffer[0];
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, LoadingScreen.vertices);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertices.limit() * Buffers.SIZEOF_FLOAT, vertices,
				GL.GL_STATIC_DRAW);

		LoadingScreen.indices = buffer[1];
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, LoadingScreen.indices);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, indices.limit() * Buffers.SIZEOF_INT, indices,
				GL.GL_STATIC_DRAW);
		indicesSize = indices.limit();

		color = buffer[2];
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, color);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, colors.limit() * Buffers.SIZEOF_FLOAT, colors,
				GL.GL_STATIC_DRAW);
		
	}
}
