package org.helioviewer.jhv.opengl;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

import com.jogamp.common.nio.Buffers;

public class CenterLoadingScreen implements RenderAnimation{

	private int texture;
	private Dimension dimension;
	private int VBOVertices;
	private final double FPS = 60;
	private final int NUMBER_OF_CIRCLE = 32;
	private final int NUMBER_OF_VISIBLE_CIRCLE = 12;
	private int size;
	private final double FACTOR = 8;

	public CenterLoadingScreen() {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				OpenGLHelper.glContext.makeCurrent();
				GL2 gl = OpenGLHelper.glContext.getGL().getGL2();
		        try {
		            BufferedImage image = IconBank.getImage(IconBank.JHVIcon.LOADING_BIG);
		            texture = OpenGLHelper.createTexture(gl, image);
		            OpenGLHelper.updateTexture(gl, texture, image);
		            dimension = new Dimension(image.getWidth(), image.getHeight());
		        } catch (Exception e) {
		            e.printStackTrace();
		        }
		        VBOVertices = initCircleVBO(gl);        				
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
		
		double width = aspect > 1 ? dimension.getWidth() * FACTOR : dimension.getWidth() * aspect * FACTOR;
		double height = aspect < 1 ? dimension.getHeight() * FACTOR : dimension.getHeight() / aspect * FACTOR;
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
		gl.glColor3f(1, 1, 1);
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
		//gl.glColor3f(1, 0, 0);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0,0);
		gl.glVertex2d(-imageWidth,imageHeight);
		gl.glTexCoord2f(1,0);
		gl.glVertex2d(imageWidth,imageHeight);
		gl.glTexCoord2f(1,1);
		gl.glVertex2d(imageWidth,-imageHeight);
		gl.glTexCoord2f(0,1);
		gl.glVertex2d(-imageWidth,-imageHeight);
		gl.glEnd();
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		*/
		renderCircles(gl, 0);
		
	}
	
	private void renderCircles(GL2 gl, double t){
		for (int i = 0; i < NUMBER_OF_VISIBLE_CIRCLE; i ++){
			gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
			gl.glDisable(GL2.GL_FRAGMENT_PROGRAM_ARB);
			gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
			gl.glDisable(GL2.GL_LIGHTING);
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_LINE_SMOOTH);
			gl.glDepthMask(false);
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, VBOVertices);
			gl.glVertexPointer(2, GL2.GL_FLOAT, 0, 0);
			gl.glDrawArrays(GL2.GL_LINE_STRIP, 0, size);
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);

		}
	}
	
	@Override
	public void isFinish() {
		// TODO Auto-generated method stub
		
	}
	
	public int initCircleVBO(GL2 gl){
		System.out.println("initCircle");
		FloatBuffer vertices = Buffers.newDirectFloatBuffer(360 * 2);
		
		for (int i = 0; i < 360; i ++){
			vertices.put((float)Math.cos(Math.toRadians(i)));
			vertices.put((float)Math.sin(Math.toRadians(i)));
		}
		vertices.flip();
		
		int[] buffer = new int[1];
		gl.glGenBuffers(1, buffer,0);
		VBOVertices = buffer[0];
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, VBOVertices);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, vertices.limit() * Buffers.SIZEOF_FLOAT, vertices, GL.GL_STATIC_DRAW);
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
		size = vertices.limit();
		return buffer[0];
	}

}
