package org.helioviewer.jhv.viewmodel.view.opengl.shader;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

/**
 * Vertex shader implementing the rescaling of power of two textures.
 * 
 * <p>
 * The {@link org.helioviewer.jhv.viewmodel.view.opengl.GLTextureHelper} places the
 * scaling factor for power-of-two-textures in the texture coordinate
 * GL_TEXTURE1. This code block performs the rescaling of the texture coordinate
 * GL_TEXTURE0 by multiplying them.
 * 
 * @author Markus Langenberg
 */
public class GLScalePowerOfTwoVertexShaderProgram extends GLVertexShaderProgram {

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
        	String program = "\ttexCoord.xy = texCoord.xy * texScaling.zw;";
            program = program.replaceAll("texScaling", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            program = program.replaceAll("texCoord", shaderBuilder.useOutputValue("float4", "TEXCOORD0"));
            shaderBuilder.addMainFragment(program);
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }
    }

    /**
     * Builds a complete, non-expandable vertex shader.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    public void buildStandAlone(GL2 gl) {
        // create new shader builder
        GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl, GL2.GL_VERTEX_PROGRAM_ARB);

        // fill with standard values
        GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
        minimalProgram.build(newShaderBuilder);

        build(newShaderBuilder);

        newShaderBuilder.compile();
    }
}
