package org.helioviewer.jhv.internal_plugins.filter.contrast;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.viewmodel.filter.AbstractFilter;
import org.helioviewer.jhv.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLTextureCoordinate;

/**
 * Filter for enhancing the contrast of the image.
 * 
 * <p>
 * It uses the following formula:
 * 
 * <p>
 * p_res(x,y) = 255 * (0.5 * sign(2x/255 - 1) * abs(2x/255 - 1)^(1.5^c) + 0.5)
 * 
 * <p>
 * Here, p_res means the resulting pixel, p_in means the original input pixel
 * and contrast the parameter used.
 * 
 * <p>
 * Since this is a point operation, it is optimized using a lookup table filled
 * by precomputing the output value for every possible input value. The actual
 * filtering is performed by using that lookup table.
 * 
 * <p>
 * The output of the filter always has the same image format as the input.
 * 
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL2.
 * 
 * @author Markus Langenberg
 */
public class ContrastFilter extends AbstractFilter implements GLFragmentShaderFilter {

    private ContrastPanel panel;

    private float contrast = 0.0f;
    private ContrastShader shader = new ContrastShader();

    /**
     * Sets the corresponding contrast panel.
     * 
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(ContrastPanel panel) {
        this.panel = panel;
        panel.setValue(contrast);
    }

    /**
     * Sets the contrast parameter.
     * 
     * @param newContrast
     *            New contrast parameter.
     */
    void setContrast(float newContrast) {
        contrast = newContrast;
        notifyAllListeners();
    }

    /**
     * Fragment shader for enhancing the contrast.
     */
    private static class ContrastShader extends GLFragmentShaderProgram {
        private GLTextureCoordinate contrastParam;

        /**
         * Sets the contrast parameter
         * 
         * @param gl
         *            Valid reference to the current gl object
         * @param contrast
         *            Contrast parameter
         */
        private void setContrast(GL2 gl, float contrast) {
            if (contrastParam != null) {
                contrastParam.setValue(gl, contrast);
            }
        }

        /**
         * {@inheritDoc}
         */
        protected void buildImpl(GLShaderBuilder shaderBuilder) {
            try {
                contrastParam = shaderBuilder.addTexCoordParameter(1);
                String program = "\toutput.rgb = 0.5f * sign(2.0f * output.rgb - 1.0f) * pow(abs(2.0f * output.rgb - 1.0f), pow(1.5f, -contrast)) + 0.5f;";
                program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                program = program.replace("contrast", contrastParam.getIdentifier(1));
                shaderBuilder.addMainFragment(program);
            } catch (GLBuildShaderException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {
        shader.build(shaderBuilder);
        return shaderBuilder;
    }

    /**
     * {@inheritDoc}
     */
    public void applyGL(GL2 gl) {
        shader.bind(gl);
        shader.setContrast(gl, contrast);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This filter is a major filter.
     */
    public boolean isMajorFilter() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void setState(String state) {
        setContrast(Float.parseFloat(state));
        panel.setValue(contrast);
    }

    /**
     * {@inheritDoc}
     */
    public String getState() {
        return Float.toString(contrast);
    }

}
