package org.helioviewer.jhv.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLScalePowerOfTwoVertexShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderView;

/**
 * Special OpenGL view, necessary if the use of non power of two textures is
 * forbidden.
 * 
 * <p>
 * Depending on the version and available extensions, textures dimension are
 * restricted: Only powers are two may be allowed. To avoid this, this class
 * works together with {@link GLTextureHelper} by creating textures, which
 * satisfy the requirement of being power of two, but only using a part of it
 * for the actual image data. But these to classes are the only ones who have to
 * handle difference. This is handled by scaling the texture coordinates within
 * the vertex shader, to avoid the invalid areas of the texture. This class is
 * responsible for building that vertex vertex shader.
 * 
 * <p>
 * If non power of two textures are activated, this view must not be used.
 * 
 * @author Markus Langenberg
 * 
 */
public class GLScalePowerOfTwoView extends AbstractGLView implements GLVertexShaderView {

    private GLScalePowerOfTwoVertexShaderProgram shader = new GLScalePowerOfTwoVertexShaderProgram();

    /**
     * {@inheritDoc}
     * 
     * In this case, does nothing.
     */
    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
    }

    /**
     * {@inheritDoc}
     */
    public void renderGL(GL2 gl, boolean nextView) {
        gl.glEnable(GL2.GL_VERTEX_PROGRAM_ARB);
        shader.bind(gl);
        this.checkGLErrors(gl,"GLScalePowerOfTwo.afterShaderBinding");

        renderChild(gl);
        this.checkGLErrors(gl,"GLScalePowerOfTwo.afterRender");

        gl.glDisable(GL2.GL_VERTEX_PROGRAM_ARB);
        this.checkGLErrors(gl,"GLScalePowerOfTwo.afterDisable");
        
    }

    /**
     * {@inheritDoc}
     */
    public GLShaderBuilder buildVertexShader(GLShaderBuilder shaderBuilder) {
        GLVertexShaderView nextView = view.getAdapter(GLVertexShaderView.class);
        if (nextView != null) {
            shaderBuilder = nextView.buildVertexShader(shaderBuilder);
        }

        shader.build(shaderBuilder);

        return shaderBuilder;
    }
    
}
