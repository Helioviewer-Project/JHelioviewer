package org.helioviewer.jhv.opengl.model;

import java.util.List;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;

public class GL3DCircle extends GL3DMesh {
    private double radius;
    private Vector4d color;
    private GL3DImageLayer layer;
    private Matrix4d phiRotation = null;
    
    public GL3DCircle(double radius, Vector4d color, String name, GL3DImageLayer layer) {
        super(name);
        this.radius = radius*0.999;
        this.color = new Vector4d((double) color.x, (double) color.y, (double) color.z, (double) color.w);
        this.layer = layer;
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<Vector3d> positions, List<Vector3d> normals, List<Vector2d> textCoords, List<Integer> indices, List<Vector4d> colors) {
    	int counter = 0;
    	
    	CoordinateVector orientationVector = this.layer.getOrientation();
        CoordinateConversion toViewSpace = this.layer.getCoordinateSystem().getConversion(state.activeCamera.getViewSpaceCoordinateSystem());

        Vector3d orientation = toViewSpace.convert(orientationVector).toVector3d().normalize();

        phiRotation = Quaternion3d.calcRotation(orientation,new Vector3d(0,0,1)).toMatrix().inverse();	        
        
    	if (!(orientation.equals(new Vector3d(0, 1, 0)))) {
            Vector3d orientationXZ = new Vector3d(orientation.x, 0, orientation.z);
            double phi = Math.acos(orientationXZ.z);
            if (orientationXZ.x < 0) {
                phi = 0 - phi;
            }
            
            phiRotation = Matrix4d.rotation(phi, new Vector3d(0, 1, 0));
            
        }
    	
    	for (double i = 0; i < 2*Math.PI; i += 0.1){
    		double x = Math.sin(i)*radius;
    	    double y = Math.cos(i)*radius;
    	    
    	    double cx = x * phiRotation.m[0] + y * phiRotation.m[4] + phiRotation.m[12];
            double cy = x * phiRotation.m[1] + y * phiRotation.m[5] + phiRotation.m[13];
            double cz = x * phiRotation.m[2] + y * phiRotation.m[6] + phiRotation.m[14];
           
    	    double vx = phiRotation.m[8] * (-1) + phiRotation.m[12];
    	    double vy = phiRotation.m[9] * (-1) + phiRotation.m[13];
    	    double vz = phiRotation.m[10] * (-1) + phiRotation.m[14];
    	    
    		positions.add(new Vector3d(cx,cy,cz));
        	indices.add(counter++);
        	normals.add(new Vector3d(vx,vy,vz));
        	colors.add(color);

    	}
    	return GL3DMeshPrimitive.TRIANGLE_FAN;
    }

    public void shapeDraw(GL3DState state) {
        super.shapeDraw(state);
    }
}
