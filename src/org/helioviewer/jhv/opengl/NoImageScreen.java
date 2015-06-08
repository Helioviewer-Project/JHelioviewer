package org.helioviewer.jhv.opengl;

import java.awt.EventQueue;
import java.awt.image.BufferedImage;

import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class NoImageScreen{

	private static int texture = -1;

	private static OpenGLHelper openGLHelper;

	
	private static void init() {
		openGLHelper = new OpenGLHelper();
				texture = openGLHelper.createTextureID();
				BufferedImage image = IconBank.getImage(JHVIcon.NOIMAGE);
				openGLHelper.bindBufferedImageToGLTexture(image);
	}

	public static void render(GL2 gl) {
		System.out.println("renderSplashscreen");

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
		if (texture < 0) init();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_LINEAR);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0, 0);
		gl.glVertex2d(-0.4, 0.4);
		gl.glTexCoord2f(1, 0);
		gl.glVertex2d(0.4, 0.4);
		gl.glTexCoord2f(1, 1);
		gl.glVertex2d(0.4, -0.4);
		gl.glTexCoord2f(0, 1);
		gl.glVertex2d(-0.4, -0.4);
		gl.glEnd();
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_TEXTURE_2D);
	}
}
