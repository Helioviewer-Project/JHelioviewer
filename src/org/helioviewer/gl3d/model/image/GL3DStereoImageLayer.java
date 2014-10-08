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

public class GL3DStereoImageLayer extends GL3DImageLayer {
    private GL3DImageSphere sphere = null;
    private GL3DImageCorona corona = null;
    private GL3DCircle circle;
    
    public GL3DStereoImageLayer(GL3DView mainView) {
        super("Stereo Image Layer", mainView);
    }

    protected void createImageMeshNodes(GL gl) {
    	this.gl = gl;
		HelioviewerMetaData hvMetaData = (HelioviewerMetaData) metaDataView.getMetaData();
		//System.out.println("METADATA INSTANCE OF " + hvMetaData.getClass().getName() + " <======================================================= INSTRUMENT: " + hvMetaData.getInstrument() + " / Detector: " + hvMetaData.getDetector());
		
		GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram   vertexShader   = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);
        //this.accellerationShape = new GL3DHitReferenceShape();
		// Always display corona
		this.fragmentShader = new GL3DImageCoronaFragmentShaderProgram();        
        GLFragmentShaderProgram coronaFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, fragmentShader);
        
        corona = new GL3DImageCorona(imageTextureView, vertexShader, coronaFragmentShader, this);
        this.imageTextureView.metadata = this.metaDataView.getMetaData();
                
		this.addNode(corona);
		
		
		circle = new GL3DCircle(Constants.SunRadius, new GL3DVec4f(0.5f, 0.5f, 0.5f, 1.0f), "Circle", this);
        
        double xOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getX()+this.imageTextureView.metadata.getPhysicalLowerLeft().getX())/(2.0*this.imageTextureView.metadata.getPhysicalImageWidth());
        double yOffset = -(this.imageTextureView.metadata.getPhysicalUpperLeft().getY()+this.imageTextureView.metadata.getPhysicalLowerLeft().getY())/(2.0*this.imageTextureView.metadata.getPhysicalImageHeight());
		
		// Don't display sphere for corona images
		if(!hvMetaData.getDetector().startsWith("COR"))
		{
	    	this.sphereFragmentShader = new GL3DImageFragmentShaderProgram();
	    	GLFragmentShaderProgram sphereFragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.sphereFragmentShader);
	    	sphere = new GL3DImageSphere(imageTextureView, vertexShader, sphereFragmentShader, this);
	    	this.addNode(sphere);
	    	this.sphereFragmentShader.setCutOffRadius(Constants.SunRadius/this.imageTextureView.metadata.getPhysicalImageWidth());
		}
        vertex.setDefaultOffset(xOffset, yOffset);
        
        this.fragmentShader.setCutOffRadius(1.00*(Constants.SunRadius/this.imageTextureView.metadata.getPhysicalImageWidth()));

        HelioviewerMetaData metadata = (HelioviewerMetaData)this.imageTextureView.metadata;
        this.fragmentShader.setDefaultOffset(metadata.getSunPixelPosition().getX()/metadata.getResolution().getX()-xOffset, metadata.getSunPixelPosition().getY()/metadata.getResolution().getY()-yOffset);

        this.addNode(circle);
        
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
