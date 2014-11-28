package org.helioviewer.jhv.internal_plugins.filter.opacity;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.opengl.model.GL3DImageLayer;
import org.helioviewer.jhv.viewmodel.filter.AbstractFilter;
import org.helioviewer.jhv.viewmodel.filter.GLFragmentShaderFilter;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLTextureCoordinate;

/**
 * Filter for changing the opacity of an image.
 * 
 * <p>
 * The output of the filter always is an ARGB image, since that is currently the
 * only format supporting an alpha channel. Thus, this filter should be applied
 * as late as possible.
 * 
 * <p>
 * This filter supports software rendering as well as rendering in OpenGL2.
 * 
 * @author Markus Langenberg
 * 
 */
public class OpacityFilter extends AbstractFilter implements GLFragmentShaderFilter {

    // ////////////////////////////////////////////////////////////////
    // Definitions
    // ////////////////////////////////////////////////////////////////

    private float opacity;
    private OpacityShader shader = new OpacityShader();
    private OpacityPanel panel;
    private GL3DImageLayer imageLayer;
    private boolean initLayer = false;
    // ////////////////////////////////////////////////////////////////
    // Methods
    // ////////////////////////////////////////////////////////////////

    public OpacityFilter(float initialOpacity) {
        this.setOpacity(initialOpacity);
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
     * Sets the corresponding opacity panel.
     * 
     * @param panel
     *            Corresponding panel.
     */
    void setPanel(OpacityPanel panel) {
        this.panel = panel;
        panel.setValue(opacity);
    }

    /**
     * Sets the opacity.
     * 
     * This function does not the slider, thus should only be called by the
     * slider itself. Otherwise, use {@link #setOpacityExternal(float)}.
     * 
     * @param newOpacity
     *            New opacity, value has to be within [0, 1]
     */
    void setOpacity(float newOpacity) {
        if (opacity == newOpacity && initLayer) {
            return;
        }
        if (imageLayer != null && imageLayer.getSphereFragmentShader() != null) {
        	imageLayer.getSphereFragmentShader().setOpacity(newOpacity);
        }
        opacity = newOpacity;
        notifyAllListeners();
    }

    /**
     * Fragment shader setting the opacity.
     */
    private static class OpacityShader extends GLFragmentShaderProgram {
        private GLTextureCoordinate alphaParam;

        /**
         * Sets the new alpha value.
         * 
         * @param gl
         *            Valid reference to the current gl object
         * @param alpha
         *            Alpha value
         */
        private void setAlpha(GL2 gl, float alpha) {
            if (alphaParam != null) {
                alphaParam.setValue(gl, alpha);
            }
        }

        /**
         * {@inheritDoc}
         */
        protected void buildImpl(GLShaderBuilder shaderBuilder) {
            try {
                alphaParam = shaderBuilder.addTexCoordParameter(1);
                String program = "\toutput.a = output.a * alpha;";
                program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
                program = program.replace("alpha", alphaParam.getIdentifier());
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
        this.checkGLErrors(gl, this+"afterBinding");
        shader.setAlpha(gl, opacity);
        this.checkGLErrors(gl, this+"afterSetAlpha");
    }

    /**
     * {@inheritDoc}
     */
    public void setState(String state) {
        setOpacity(Float.parseFloat(state));
        panel.setValue(opacity);
    }

    /**
     * {@inheritDoc}
     */
    public String getState() {
        return Float.toString(opacity);
    }
    
    public boolean checkGLErrors(GL2 gl, String message) {
        if (gl == null) {
            Log.warn("OpenGL not yet Initialised!");
            return true;
        }
        int glErrorCode = gl.glGetError();

        if (glErrorCode != GL2.GL_NO_ERROR) {
            GLU glu = new GLU();
            Log.error("GL Error (" + glErrorCode + "): " + glu.gluErrorString(glErrorCode) + " - @" + message);
            if (glErrorCode == GL2.GL_INVALID_OPERATION) {
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

	public void setImageLayer(GL3DImageLayer imageLayer) {
		this.imageLayer = imageLayer;
		imageLayer.addOpacityFilter(this);
	}

	public void initOpacity() {
		this.setOpacity(opacity);
		this.initLayer = true;
	}
}
