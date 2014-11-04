    package org.helioviewer.jhv.opengl.shader;

import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageCoronaFragmentShaderProgram extends GLFragmentShaderProgram {
    public GL3DImageCoronaFragmentShaderProgram() {
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\toutput.a = 1*alpha;" + GLShaderBuilder.LINE_SEP;
            
            program += "\tfloat2 texture;" + GLShaderBuilder.LINE_SEP;
            program += "\ttexture.x = textureCoordinate.z - offset.x;" + GLShaderBuilder.LINE_SEP;
            program += "\ttexture.y = textureCoordinate.w - offset.y;" + GLShaderBuilder.LINE_SEP;
            program += "\toutput.a *= step(cutOffRadius, length(texture));" + GLShaderBuilder.LINE_SEP;
            program += "\tif(output.a < 0.01) discard;" + GLShaderBuilder.LINE_SEP;
            shaderBuilder.addEnvParameter("float cutOffRadius");
            shaderBuilder.addEnvParameter("float alpha");
            shaderBuilder.addEnvParameter("float2 offset");
            
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("textureCoordinate", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));
            shaderBuilder.addMainFragment(program);
            
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
        
    public void changeAlpha(double alpha){
    	this.alpha = alpha;
    }
    
    public void setCutOffRadius(double cutOffRadius){
    	this.cutOffRadius = cutOffRadius;
    }

	public void setDefaultOffset(double xOffset, double yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

}
