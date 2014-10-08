package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.shader.GL3DImageCoronaFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.metadata.HelioviewerMetaData;
import org.helioviewer.viewmodel.metadata.HelioviewerPositionedMetaData;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DAIAImageLayer extends GL3DImageLayer {
    private GL3DImageSphere sphere;
    private GL3DImageCorona corona;
    private GL3DCircle circle;
    public GL3DAIAImageLayer(GL3DView mainView) {
        super("AIA Image Layer", mainView);
    }

    protected void createImageMeshNodes(GL gl) {
    	this.sphereFragmentShader = new GL3DImageFragmentShaderProgram();
    	GLFragmentShaderProgram sphereFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.sphereFragmentShader);
        
        this.fragmentShader = new GL3DImageCoronaFragmentShaderProgram();        
        GLFragmentShaderProgram coronaFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, fragmentShader);
        GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram   vertexShader   = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);
        this.imageTextureView.metadata = this.metaDataView.getMetaData();
        sphere = new GL3DImageSphere(imageTextureView, vertexShader, sphereFragmentShader, this);
        corona = new GL3DImageCorona(imageTextureView, vertexShader, coronaFragmentShader, this);
        circle = new GL3DCircle(Constants.SunRadius, new GL3DVec4f(0.5f, 0.5f, 0.5f, 1.0f), "Circle", this);
        
        double xOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getX()+this.imageTextureView.metadata.getPhysicalLowerLeft().getX())/(2.0*this.imageTextureView.metadata.getPhysicalImageWidth());
        double yOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getY()+this.imageTextureView.metadata.getPhysicalLowerLeft().getY())/(2.0*this.imageTextureView.metadata.getPhysicalImageHeight());
        vertex.setDefaultOffset((float)xOffset, (float)yOffset);
        this.sphereFragmentShader.setCutOffRadius((float)(Constants.SunRadius/this.imageTextureView.metadata.getPhysicalImageWidth()));
        this.fragmentShader.setCutOffRadius((float)(Constants.SunRadius/this.imageTextureView.metadata.getPhysicalImageWidth()));
        
        HelioviewerMetaData metadata = (HelioviewerMetaData)this.imageTextureView.metadata;
        this.fragmentShader.setDefaultOffset(metadata.getSunPixelPosition().getX()/metadata.getResolution().getX()-xOffset, metadata.getSunPixelPosition().getY()/metadata.getResolution().getY()-yOffset);
        this.addNode(circle);
        this.addNode(corona);
        this.addNode(sphere);
        this.gl = gl;
    }

    protected GL3DImageMesh getImageCorona() {
        return this.corona;
    }

    protected GL3DImageMesh getImageSphere() {
        return this.sphere;
    }

	@Override
	protected GL3DMesh getCircle() {
		// TODO Auto-generated method stub
		return this.circle;
	}

	
    
}
