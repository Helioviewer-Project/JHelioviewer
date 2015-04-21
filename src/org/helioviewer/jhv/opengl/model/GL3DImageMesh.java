package org.helioviewer.jhv.opengl.model;

import javax.media.opengl.GL;

import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.ImageTextureRecapturedReason;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.jhv.viewmodel.view.opengl.GLTextureHelper;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * A {@link GL3DImageMesh} is used to map a image that was rendered in the 2D
 * sub-chain onto a mesh. The image is provided as a texture that was created by
 * a {@link GL3DImageTextureView}.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DImageMesh extends GL3DMesh {

    protected GL3DImageTextureView imageTextureView;

    private GLTextureHelper th = new GLTextureHelper();

    protected GLVertexShaderProgram vertexShaderProgram;
    protected GLFragmentShaderProgram fragmentShaderProgram;

    protected PhysicalRegion capturedRegion;

    private boolean reshapeRequested = false;
    
    public GL3DImageMesh(String name, GL3DImageTextureView _imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram) {
        super(name, new Vector4d(0, 1, 0, 0.5f), new Vector4d(0, 0, 0, 0));
        this.imageTextureView = _imageTextureView;

        this.vertexShaderProgram = vertexShaderProgram;
        this.fragmentShaderProgram = fragmentShaderProgram;
        
        imageTextureView.addViewListener(new ViewListener() {
        	
            public void viewChanged(View sender, ChangeEvent aEvent) {
                ImageTextureRecapturedReason reason = aEvent.getLastChangedReasonByType(ImageTextureRecapturedReason.class);
                if (reason != null) {
                    reshapeRequested = true;
                    capturedRegion = reason.getCapturedRegion();
                    markAsChanged();
                    // Log.debug("GL3DImageMesh.reshape: "+getName()+" Reason="+reason+", Event="+aEvent);
                }
            }
        });
        this.reshapeRequested = true;
        this.markAsChanged();
    }
    
    
    public GL3DImageMesh(String name, GL3DImageTextureView _imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram, boolean viewListener) {
        super(name, new Vector4d(0, 1, 0, 0.5f), new Vector4d(0, 0, 0, 0));
        this.imageTextureView = _imageTextureView;

        this.vertexShaderProgram = vertexShaderProgram;
        this.fragmentShaderProgram = fragmentShaderProgram;

        this.reshapeRequested = true;
        this.markAsChanged();
    }
    

    public void shapeInit(GL3DState state) {
        super.shapeInit(state);
        this.imageTextureView.forceUpdate();
        // Log.debug("GL3DImageMesh.shapeInit: "+getName()+" Forcing image texture to update!");
    }

    public void shapeUpdate(GL3DState state) {
        if (this.reshapeRequested) {
            // Reshape Mesh
            recreateMesh(state);
            // Log.debug("GL3DImageMesh.reshape: "+getName()+" Recreated Mesh!");
            this.reshapeRequested = false;
        }
    }

    public void shapeDraw(GL3DState state) {
        th.bindTexture(state.gl, this.imageTextureView.getTextureId());
        state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        
        GLVertexShaderProgram.pushShader(state.gl);
        GLFragmentShaderProgram.pushShader(state.gl);
        this.vertexShaderProgram.bind(state.gl);
        this.fragmentShaderProgram.bind(state.gl);

        super.shapeDraw(state);

        GLVertexShaderProgram.popShader(state.gl);
        GLFragmentShaderProgram.popShader(state.gl);

        th.bindTexture(state.gl, 0);
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }
}
