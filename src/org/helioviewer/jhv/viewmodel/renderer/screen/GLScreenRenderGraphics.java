package org.helioviewer.jhv.viewmodel.renderer.screen;

import java.awt.image.BufferedImage;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.viewmodel.renderer.GLCommonRenderGraphics;

/**
 * Implementation of ScreenRenderGraphics, using OpenGL for drawing.
 * 
 * <p>
 * Maps all methods to corresponding OpenGL methods.
 * 
 * @author Markus Langenberg
 * 
 * */
public class GLScreenRenderGraphics {

    private static final int POINTS_PER_OVAL = 32; // has to be power of two
    private static final float[] SIN_TABLE = new float[POINTS_PER_OVAL];
    
    static
    {
        for (int i = 0; i < POINTS_PER_OVAL; i++) {
            SIN_TABLE[i] = (float) Math.sin(Math.PI * 2 * i / POINTS_PER_OVAL);
        }
    }

    public GL2 gl;
    private GLCommonRenderGraphics commonRenderGraphics;

    /**
     * Default constructor.
     * 
     * <p>
     * The caller has to provide a gl object, which can be used by this
     * renderer.
     * 
     * @param _gl
     *            gl object, that should be used for drawing.
     */
    public GLScreenRenderGraphics(GL2 _gl) {
        gl = _gl;
        commonRenderGraphics = new GLCommonRenderGraphics(_gl);
    }

    /**
     * {@inheritDoc}
     */
    public void fillOval(int x, int y, int width, int height) {
        gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);

        int radiusX = width >> 1;
        int radiusY = height >> 1;
        int centerX = x + radiusX;
        int centerY = y + radiusY;

        gl.glBegin(GL2.GL_TRIANGLE_FAN);

        gl.glVertex2i(centerX, centerY);
        for (int i = 0; i < POINTS_PER_OVAL; i++) {
            gl.glVertex2f(centerX + (radiusX * SIN_TABLE[i]), centerY + (radiusY * SIN_TABLE[(i + (POINTS_PER_OVAL >> 2)) & (POINTS_PER_OVAL - 1)]));
        }
        gl.glVertex2f(centerX, centerY + radiusY);

        gl.glEnd();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Note, that the renderer buffers recently seen images, so it a good idea
     * to use the same image object every call, if the image data does not
     * change.
     */
    public void drawImage(BufferedImage image, int x, int y, int width, int height) {
        commonRenderGraphics.bindScalingShader();
        commonRenderGraphics.bindImage(image);

        gl.glColor3f(1.0f, 1.0f, 1.0f);

        gl.glBegin(GL2.GL_QUADS);
        
        commonRenderGraphics.setTexCoord(0.0f, 0.0f);
        gl.glVertex2i(x, y);
        commonRenderGraphics.setTexCoord(0.0f, 1.0f);
        gl.glVertex2i(x, y + height);
        commonRenderGraphics.setTexCoord(1.0f, 1.0f);
        gl.glVertex2i(x + width, y + height);
        commonRenderGraphics.setTexCoord(1.0f, 0.0f);
        gl.glVertex2i(x + width, y);

        gl.glEnd();

        commonRenderGraphics.gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
    }
}
