package org.helioviewer.jhv.opengl.scenegraph;

import java.util.Stack;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DComponentView;

/**
 * The {@link GL3DState} is recreated every render pass by the
 * {@link GL3DComponentView}. It provides the reference to the {@link GL} object
 * and stores some globally relevant information such as width and height of the
 * viewport, etc. Also it allows for the stacking of the view transformations.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DState {
    private static GL3DState instance;

    public GL2 gl;

    protected Matrix4d mv;
    private Stack<Matrix4d> matrixStack;

    protected Matrix4d mvInverse;

    public GL3DCamera activeCamera;

    protected int viewportWidth;
    protected int viewportHeight;

    public enum VISUAL_TYPE {
		MODE_2D, MODE_3D
	};
	
	private VISUAL_TYPE stateType = VISUAL_TYPE.MODE_3D;
	
    public static GL3DState create(GL2 gl) {
        instance = new GL3DState(gl);
        return instance;
    }

    public static GL3DState get() {
        return instance;
    }

    public static GL3DState getUpdated(GL2 gl, int width, int height) {
        instance.gl = gl;
        instance.viewportWidth = width;
        instance.viewportHeight = height;
        return instance;
    }

    private GL3DState(GL2 gl) {
        this.gl = gl;
        this.mv = Matrix4d.identity();
        this.matrixStack = new Stack<Matrix4d>();
    }

    public void pushMV() {
        gl.glPushMatrix();
        this.matrixStack.push(new Matrix4d(this.mv));
        // Log.debug("GL3DState.pushMV: "+this.matrixStack.size());
    }

    public void popMV() {
        gl.glPopMatrix();
        this.mv = this.matrixStack.pop();
        // Log.debug("GL3DState.popMV: "+this.matrixStack.size());
    }

    public void loadIdentity() {
        this.mv = Matrix4d.identity();
        this.mvInverse = Matrix4d.identity();
        this.matrixStack.push(new Matrix4d(this.mv));
        this.gl.glLoadIdentity();
    }

    public Matrix4d multiplyMV(Matrix4d m) {
        this.mv.multiply(m);
        gl.glMultMatrixd(m.m, 0);
        return mv;
    }

    public void buildInverseAndNormalMatrix() {
        try {
            this.mvInverse = this.mv.inverse();
        } catch (IllegalArgumentException e) {
            // TODO: What to do when matrix cannot be inverted?
            Log.error("Cannot Invert ModelView Matrix! Singularity occurred!", e);
            this.mvInverse = Matrix4d.identity();
            this.mv = Matrix4d.identity();
        }
    }

    public Matrix4d getMVInverse() {
        return new Matrix4d(this.mvInverse);
    }

    public boolean checkGLErrors(String message) {
        if (gl == null) {
            Log.warn("OpenGL not yet Initialised!");
            return true;
        }
        int glErrorCode = gl.glGetError();

        if (glErrorCode != GL.GL_NO_ERROR) {
            GLU glu = new GLU();
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode) + " - @" + message);
            if (glErrorCode == GL.GL_INVALID_OPERATION) {
                // Find the error position
                int[] err = new int[1];
                gl.glGetIntegerv(GL2.GL_PROGRAM_ERROR_POSITION_ARB, err, 0);
                if (err[0] >= 0) {
                    String error = gl.glGetString(GL2.GL_PROGRAM_ERROR_STRING_ARB);
                    Log.error("GL error at " + err[0] + ":\n" + error);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean checkGLErrors(GL gl) {
        if (gl == null) {
            Log.warn("OpenGL not yet Initialised!");
            return true;
        }
        int glErrorCode = gl.glGetError();

        if (glErrorCode != GL.GL_NO_ERROR) {
            GLU glu = new GLU();
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode));
            if (glErrorCode == GL.GL_INVALID_OPERATION) {
                // Find the error position
                int[] err = new int[1];
                gl.glGetIntegerv(GL2.GL_PROGRAM_ERROR_POSITION_ARB, err, 0);
                if (err[0] >= 0) {
                    String error = gl.glGetString(GL2.GL_PROGRAM_ERROR_STRING_ARB);
                    Log.error("GL error at " + err[0] + ":\n" + error);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public int getViewportHeight() {
        return viewportHeight;
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void set2DState(){
    	this.stateType = VISUAL_TYPE.MODE_2D;
    }
    
    public void set3DState(){
    	this.stateType = VISUAL_TYPE.MODE_3D;
    }
    
    public VISUAL_TYPE getState(){
    	return this.stateType;
    }
}
