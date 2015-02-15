package org.helioviewer.jhv.opengl;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class SplashScreen implements RenderAnimation{

	private int texture = -1;
	private Dimension dimension;
	private final double FACTOR = 8;

	public SplashScreen(GL2 gl) {		
        try {
            BufferedImage image = IconBank.getImage(JHVIcon.NOIMAGE);
            texture = OpenGLHelper.createTexture(gl, image);
            OpenGLHelper.updateTexture(gl, texture, image);
            dimension = new Dimension(image.getWidth(), image.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
		
	}
	
	@Override
	public void render(GL2 gl, double canvasWidth, double canvasHeight) {
		System.out.println("renderSplashscreen");
		gl.glClear( GL.GL_COLOR_BUFFER_BIT );
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		double aspect = canvasWidth / canvasHeight;
		
		double width = aspect > 1 ? dimension.getWidth() * FACTOR : dimension.getWidth() * aspect * FACTOR;
		double height = aspect < 1 ? dimension.getHeight() * FACTOR : dimension.getHeight() / aspect * FACTOR;
		width /= 2.0;
		height /= 2.0;
		double imageWidth = dimension.getWidth() / 2.0;
		double imageHeight = dimension.getHeight() / 2.0;
		gl.glOrtho(-width, width, -height, height, 10, -10);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_BLEND);

		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glEnable(GL2.GL_BLEND);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_LINEAR);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
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
	}

	@Override
	public void isFinish() {
		// TODO Auto-generated method stub
		
	}

}
