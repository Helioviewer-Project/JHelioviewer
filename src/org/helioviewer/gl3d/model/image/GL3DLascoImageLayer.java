package org.helioviewer.gl3d.model.image;

import javax.media.opengl.GL;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.model.GL3DHitReferenceShape;
import org.helioviewer.gl3d.scenegraph.GL3DMesh;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec4f;
import org.helioviewer.gl3d.shader.GL3DImageCoronaFragmentShaderProgram;
import org.helioviewer.gl3d.shader.GL3DImageVertexShaderProgram;
import org.helioviewer.gl3d.shader.GL3DShaderFactory;
import org.helioviewer.gl3d.view.GL3DView;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.viewmodel.view.opengl.shader.GLVertexShaderProgram;

public class GL3DLascoImageLayer extends GL3DImageLayer {
    private GL3DImageCorona lascoImageMesh;
    private GL3DCircle circle;

    public GL3DLascoImageLayer(GL3DView mainView) {
        super("LASCO Image Layer", mainView);
    }

    protected void createImageMeshNodes(GL gl) {
        this.fragmentShader = new GL3DImageCoronaFragmentShaderProgram();        
        
        GLFragmentShaderProgram fragmentShader = GL3DShaderFactory.createFragmentShaderProgram(gl, this.fragmentShader);
        GL3DImageVertexShaderProgram vertex = new GL3DImageVertexShaderProgram();
        GLVertexShaderProgram   vertexShader   = GL3DShaderFactory.createVertexShaderProgram(gl, vertex);
        this.imageTextureView.setVertexShader(vertex);
        
        lascoImageMesh = new GL3DImageCorona("LASCO", imageTextureView, vertexShader, fragmentShader, this);
        this.imageTextureView.metadata = this.metaDataView.getMetaData();
        
        this.accellerationShape = new GL3DHitReferenceShape();
        circle = new GL3DCircle(Constants.SunRadius, new GL3DVec4f(0.5f, 0.5f, 0.5f, 1.0f), "Circle", this);
        
        double xOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getX()+this.imageTextureView.metadata.getPhysicalLowerLeft().getX())/(2.0*this.imageTextureView.metadata.getPhysicalImageWidth());
        double yOffset = (this.imageTextureView.metadata.getPhysicalUpperRight().getY()+this.imageTextureView.metadata.getPhysicalLowerLeft().getY())/(2.0*this.imageTextureView.metadata.getPhysicalImageHeight());
        vertex.setDefaultOffset(xOffset, yOffset);

        MetaData metadata = this.imageTextureView.metadata;
        this.fragmentShader.setDefaultOffset(metadata.getSunPixelPosition().getX()/metadata.getResolution().getX()-xOffset, metadata.getSunPixelPosition().getY()/metadata.getResolution().getY()-yOffset);

        this.addNode(circle);

        this.addNode(lascoImageMesh);
    }

    protected GL3DImageMesh getImageCorona() {
        return lascoImageMesh;
    }

    protected GL3DImageMesh getImageSphere() {
        return null;
    }
    
	@Override
	protected GL3DMesh getCircle() {
		// TODO Auto-generated method stub
		return this.circle;
	}


}
