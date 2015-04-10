package org.helioviewer.jhv.opengl.shader;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.opengl.model.GL3DImageMesh;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * The {@link GL3DShaderFactory} is used to create {@link GL3DImageMesh} nodes.
 * A Factory pattern is used, because the underlying image layer determines what
 * image meshes need to be created. Depending on the image, several image meshes
 * are grouped created.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DShaderFactory {

    public static GLFragmentShaderProgram createFragmentShaderProgram(GL gl, GLFragmentShaderProgram fragmentShaderProgram) {
        // create new shader builder
        GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl.getGL2(), GL2.GL_FRAGMENT_PROGRAM_ARB, true);

        // fill with standard values
        GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
        minimalProgram.build(newShaderBuilder);
        fragmentShaderProgram.build(newShaderBuilder);

        // fill with other filters and compile
        newShaderBuilder.compile();
        // Log.debug("GL3DShaderFactory.createFragmentShaderProgram");
        return fragmentShaderProgram;
    }

    public static GLVertexShaderProgram createVertexShaderProgram(GL gl, GLVertexShaderProgram vertexShaderProgram) {
        // create new shader builder
        GLShaderBuilder newShaderBuilder = new GLShaderBuilder(gl.getGL2(), GL2.GL_VERTEX_PROGRAM_ARB, true);

        // fill with standard values
        GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
        minimalProgram.build(newShaderBuilder);
        vertexShaderProgram.build(newShaderBuilder);

        // fill with other filters and compile
        newShaderBuilder.compile();
        
        // Log.debug("GL3DShaderFactory.createVertexShaderProgram");
        return vertexShaderProgram;
    }
}
