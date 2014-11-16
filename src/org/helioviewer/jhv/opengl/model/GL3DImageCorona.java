package org.helioviewer.jhv.opengl.model;

import java.util.List;

import org.helioviewer.jhv.base.math.GL3DVec3d;
import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.base.wcs.impl.TextureCoordinateSystem;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * A GL3DImageCorona maps the coronal part of an image layer onto an image plane
 * 
 * @author Simon Sp������rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageCorona extends GL3DImageMesh {
    private GL3DImageLayer layer = null;
    Matrix4d phiRotation = null;
    
    public GL3DImageCorona(String name, GL3DImageTextureView imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram, GL3DImageLayer imageLayer) {
        super(name, imageTextureView, vertexShaderProgram, fragmentShaderProgram);
        this.layer = imageLayer;
    }

    public GL3DImageCorona(GL3DImageTextureView imageTextureView, GLVertexShaderProgram vertexShaderProgram, GLFragmentShaderProgram fragmentShaderProgram ,GL3DImageLayer imageLayer) {
        this("Corona", imageTextureView, vertexShaderProgram, fragmentShaderProgram, imageLayer);
    }
        
    public GL3DMeshPrimitive createMesh(GL3DState state, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<Vector2d> textCoords, List<Integer> indices, List<Vector4d> colors) {
        Region region = this.capturedRegion;
    	if (region != null) {
    		MetaData metaData = this.layer.metaDataView.getMetaData();
    		
            new TextureCoordinateSystem(this.imageTextureView.getTextureScale(), region);
            // Read Boundaries on Solar Disk
            CoordinateVector orientationVector = this.layer.getOrientation();
            CoordinateConversion toViewSpace = this.layer.getCoordinateSystem().getConversion(state.activeCamera.getViewSpaceCoordinateSystem());
            GL3DVec3d orientation = toViewSpace.convert(orientationVector).toVector3d().normalize();

            phiRotation = Quaternion3d.calcRotation(orientation,new GL3DVec3d(0,0,1)).toMatrix().inverse();	        

            if (!(orientation.equals(new GL3DVec3d(0, 1, 0)))) {
                GL3DVec3d orientationXZ = new GL3DVec3d(orientation.x, 0, orientation.z);
                double phi = Math.acos(orientationXZ.z);
                if (orientationXZ.x < 0) {
                    phi = 0 - phi;
                }
                
                phiRotation = Matrix4d.rotation(phi, new GL3DVec3d(0, 1, 0));
                GL3DVec3d direction = new GL3DVec3d(phiRotation.m[8]*1, phiRotation.m[9]*1, phiRotation.m[10]*1);
                this.layer.setLayerDirection(direction);
            }
            
            
            if (phiRotation != null){
            	int vertexCounter = 0;
	
	            pushVertex(metaData.getPhysicalUpperLeft(), positions, normals, textCoords, colors,0.0,1.0);
	            pushVertex(metaData.getPhysicalUpperRight(), positions, normals, textCoords, colors,1.0,1.0);
	            pushVertex(metaData.getPhysicalLowerRight(), positions, normals, textCoords, colors,1.0,0.0);
	            pushVertex(metaData.getPhysicalLowerLeft(), positions, normals, textCoords, colors,0.0,0.0);
	            indices.add(vertexCounter + 0);
	            indices.add(vertexCounter + 1);
	            indices.add(vertexCounter + 2);
	            indices.add(vertexCounter + 3);
            }
        }
    
        return GL3DMeshPrimitive.QUADS;
    }
    
    
    
    private void pushVertex(Vector2d position, List<GL3DVec3d> positions, List<GL3DVec3d> normals, List<Vector2d> texCoords, List<Vector4d> colors, double tx, double ty) {
    	double cx = position.x * phiRotation.m[0] + position.y * phiRotation.m[4] + phiRotation.m[12];
        double cy = position.x * phiRotation.m[1] + position.y * phiRotation.m[5] + phiRotation.m[13];
        double cz = position.x * phiRotation.m[2] + position.y * phiRotation.m[6] + phiRotation.m[14];
       
        positions.add(new GL3DVec3d(cx, cy, cz));
        colors.add(new Vector4d(0, 0, 0, 1));
        texCoords.add(new Vector2d(tx, ty));
    }

    public GL3DImageTextureView getImageTextureView() {
        return imageTextureView;
    }
    
    
    public Region getCapturedRegion() { return capturedRegion; }
    
    
    
}
