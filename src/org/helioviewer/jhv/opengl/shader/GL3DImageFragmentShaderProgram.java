package org.helioviewer.jhv.opengl.shader;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder.GLBuildShaderException;

public class GL3DImageFragmentShaderProgram extends GLFragmentShaderProgram {

    public GL3DImageFragmentShaderProgram() {
    }

    /**
     * {@inheritDoc}
     */
    protected void buildImpl(GLShaderBuilder shaderBuilder) {
        try {
            String program = "\toutput.a = opacity;" + GLShaderBuilder.LINE_SEP;

        	program += "\tfloat2 texture;" + GLShaderBuilder.LINE_SEP;
            program += "\ttexture.x = textureCoordinate.z - 0.5;" + GLShaderBuilder.LINE_SEP;
            program += "\ttexture.y = textureCoordinate.w - 0.5;" + GLShaderBuilder.LINE_SEP;
            
            shaderBuilder.addEnvParameter("float opacity");
            shaderBuilder.addEnvParameter("float cutOffRadius");
            shaderBuilder.addEnvParameter("float alpha");
            
            program = program.replace("output", shaderBuilder.useOutputValue("float4", "COLOR"));
            program = program.replace("textureCoordinate", shaderBuilder.useStandardParameter("float4", "TEXCOORD0"));

            shaderBuilder.addMainFragment(program);
            //System.out.println("GL3D Image Fragment Shader:\n" + shaderBuilder.getCode());
        } catch (GLBuildShaderException e) {
            e.printStackTrace();
        }

    }
    
    public void setCutOffRadius(double cutOffRadius){
    	this.cutOffRadius = cutOffRadius;
    }
    
    public void setOpacity(float opacity){
    	this.opacity = opacity;
    }

}
