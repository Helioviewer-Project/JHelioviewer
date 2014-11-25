package org.helioviewer.jhv.internal_plugins.filter.gammacorrection;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.viewmodel.filter.AbstractFilter;
import org.helioviewer.jhv.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLTextureCoordinate;

/**
 * Filter for applying gamma correction.
 * 
 * <p>
 * It uses the following formula:
 * 
 * <p>
 * p_res(x,y) = 255 * power( p_in(x,y) / 255, gamma)
 * 
 * <p>
 * Here, p_res means the resulting pixel, p_in means the original input pixel
 * and gamma the gamma value used.
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
public class GammaCorrectionFilter extends AbstractFilter implements GLFragmentShaderFilter {

    private GammaCorrectionPanel panel;

    private float gamma = 1.0f;
    private GammaCorrectionShader shader = new GammaCorrectionShader();

    /**
     * Sets the corresponding gamma correction panel.
     * 
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(GammaCorrectionPanel panel) {
        this.panel = panel;
        panel.setValue(gamma);
    }

    /**
     * Sets the gamma value.
     * 
     * @param newGamma
     *            New gamma value.
     */
    void setGamma(float newGamma) {
        gamma = newGamma;
        notifyAllListeners();
    }

    /**
     * Fragment shader for applying the gamma correction.
     */
    private static class GammaCorrectionShader extends GLFragmentShaderProgram {
        private GLTextureCoordinate gammaParam;

        /**
         * Sets the gamma value
         * 
         * @param gl
         *            Valid reference to the current gl object
         * @param gamma
         *            Gamma value
         */
        private void setGamma(GL2 gl, float gamma) {
            if (gammaParam != null) {
                gammaParam.setValue(gl, gamma);
            }
        }

        /**
         * {@inheritDoc}
         */
        protected void buildImpl(GLShaderBuilder shaderBuilder) {
            try {
                gammaParam = shaderBuilder.addTexCoordParameter(1);
                String program = "\toutput.rgb = pow(output.rgb, gamma);";
                program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                program = program.replace("gamma", gammaParam.getIdentifier(3));
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
        shader.setGamma(gl, gamma);
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
        setGamma(Float.parseFloat(state));
        panel.setValue(gamma);
    }

    /**
     * {@inheritDoc}
     */
    public String getState() {
        return Float.toString(gamma);
    }

}
