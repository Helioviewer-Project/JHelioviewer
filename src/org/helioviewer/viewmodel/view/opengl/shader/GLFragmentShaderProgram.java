package org.helioviewer.viewmodel.view.opengl.shader;

import java.util.Stack;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * Abstract class to build customized fragment shaders.
 * 
 * <p>
 * To use this class, implement it and put the generation of the shader in the
 * buildImpl-function. That function will be called during the rebuild of all
 * shaders.
 * 
 * <p>
 * Every implementation of this class represents one block of the final shader
 * code.
 * 
 * <p>
 * For further information about how to build shaders, see
 * {@link GLShaderBuilder} as well as the Cg User Manual.
 * 
 * @author Markus Langenberg
 */
public abstract class GLFragmentShaderProgram {

    protected static final int target = GL2.GL_FRAGMENT_PROGRAM_ARB;

    private static Stack<Integer> shaderStack = new Stack<Integer>();
    private static int shaderCurrentlyUsed = -1;
    private int shaderID;
    protected double alpha = 1.0f;
    protected double cutOffRadius = 0.0f;
    protected double xOffset = 0.0f;
    protected double yOffset = 0.0f;
    
    /**
     * Build the shader.
     * 
     * This function is called during the building process of all shaders,
     * providing a shader builder object. That object may already contain code
     * from other shader blocks. This functions calls
     * {@link #buildImpl(GLShaderBuilder)} and remembers the shader if the
     * shader, to be able to bind it later.
     * 
     * @param shaderBuilder
     *            ShaderBuilder to append customized code
     */
    public final void build(GLShaderBuilder shaderBuilder) {
        buildImpl(shaderBuilder);
        shaderID = shaderBuilder.getShaderID();
        shaderCurrentlyUsed = -1;
    }

    /**
     * Build customized part of the shader.
     * 
     * This function is called during the building process of all shaders,
     * providing a shader builder object. That object may already contain code
     * from other shader blocks. Just append the additional code within this
     * function.
     * 
     * @param shaderBuilder
     *            ShaderBuilder to append customized code
     */
    protected abstract void buildImpl(GLShaderBuilder shaderBuilder);

    /**
     * Binds (= activates it) the shader, if it is not active so far.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public final void bind(GL2 gl) {
    	bind(gl, shaderID, alpha, cutOffRadius, xOffset, yOffset);
    }

    /**
     * Pushes the shader currently in use onto a stack.
     * 
     * This is useful to load another shader but still being able to restore the
     * old one, similar to the very common pushMatrix() in OpenGL2.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @see #popShader(GL)
     */
    public static void pushShader(GL2 gl) {
        shaderStack.push(shaderCurrentlyUsed);
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        // Log.debug("GL3DFragmentShaderProgram: pushShader, current="+shaderCurrentlyUsed);
    }

    /**
     * Takes the top of from the shader stack and binds it.
     * 
     * This restores a shader pushed onto the stack earlier, similar to the very
     * common popMatrix() in OpenGL2.
     * 
     * @param gl
     *            Valid reference to the current gl object
     * @see #pushShader(GL)
     */
    public static void popShader(GL2 gl) {
        gl.glPopAttrib();
        Integer restoreShaderObject = shaderStack.pop();
        int restoreShader = restoreShaderObject == null ? 0 : restoreShaderObject.intValue();
        if (restoreShader >= 0) {
            bind(gl, restoreShader, 0.0f, 0.0f, 0.0f,0.0f);
        }
    }

    /**
     * Binds (= activates it) the given shader, if it is not active so far.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    private static void bind(GL2 gl, int shader, double alpha, double cutOffRadius, double xOffset, double yOffset) {
        if (shader != shaderCurrentlyUsed) {
            shaderCurrentlyUsed = shader;
            gl.glBindProgramARB(target, shader);
            gl.glProgramLocalParameter4dARB(target, 1, alpha, 0.0f, 0.0f, 0.0f);
            gl.glProgramLocalParameter4dARB(target, 0, cutOffRadius, 0.0f, 0.0f, 0.0f);
            gl.glProgramLocalParameter4dARB(target, 2, xOffset, yOffset, 0.0f, 0.0f);
        }
    }

    public int getId() {
        return this.shaderID;
    }
}
