package org.helioviewer.jhv.opengl;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class NoImageScreen implements RenderAnimation{

	private int texture = -1;
	private Dimension dimension;
	private final double FACTOR = 8;

	private OpenGLHelper openGLHelper;
	public NoImageScreen(GL2 gl) {		
		openGLHelper = new OpenGLHelper();
        try {
            BufferedImage image = IconBank.getImage(JHVIcon.NOIMAGE);
            texture = openGLHelper.createTextureID();
            openGLHelper.bindBufferedImageToGLTexture(image);
            dimension = new Dimension(image.getWidth(), image.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
		
	}
	
	@Override
	public void render(GL2 gl, double canvasWidth, double canvasHeight) {
		System.out.println("renderSplashscreen");
		gl.glClear( GL.GL_COLOR_BUFFER_BIT );

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
	}

	@Override
	public void isFinish() {
		// TODO Auto-generated method stub
		
	}

}
