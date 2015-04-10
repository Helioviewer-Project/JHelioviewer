package org.helioviewer.jhv.opengl.model;

import java.util.List;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.opengl.scenegraph.GL3DAABBox;
import org.helioviewer.jhv.opengl.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;

/**
 * The {@link GL3DHitReferenceShape} unifies all possible Image Layers (
 * {@link GL3DImageMesh} nodes in the Scene Graph)�in a single mesh node. This
 * node offers a mathematically simpler representation for faster hit point
 * detection when used for determining the region of interest on the image
 * meshes.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DHitReferenceShape extends GL3DMesh {
    private static final double LARGE_VALUE = Constants.SUN_MEAN_DISTANCE_TO_EARTH * 10;

    private boolean allowBacksideHits;
    private double angle = 0.0;
    private Matrix4d phiRotation;
    public GL3DHitReferenceShape() {
        this(false);
    }

    public GL3DHitReferenceShape(boolean allowBacksideHits) {
        super("Hit Reference Shape");
        this.allowBacksideHits = allowBacksideHits;
    }

    public GL3DHitReferenceShape(boolean allowBacksideHits, double angle) {
        super("Hit Reference Shape");
        this.allowBacksideHits = allowBacksideHits;
        this.angle = angle;
    }
    
    public void shapeDraw(GL3DState state) {
        return;
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<Vector3d> positions, List<Vector3d> normals, List<Vector2d> textCoords, List<Integer> indices, List<Vector4d> colors) {
    	this.phiRotation = Matrix4d.rotation(angle, new Vector3d(0, 1, 0));
        Vector3d ll = createVertex(-LARGE_VALUE, -LARGE_VALUE, 0);
        Vector3d lr = createVertex(LARGE_VALUE, -LARGE_VALUE, 0);
        Vector3d tr = createVertex(LARGE_VALUE, LARGE_VALUE, 0);
        Vector3d tl = createVertex(-LARGE_VALUE, LARGE_VALUE, 0);

        positions.add(ll);// normals.add(new Vector3d(0,0,1));//colors.add(new
                          // GL3DVec4d(0, 0, 1, 0.0));
        positions.add(lr);// normals.add(new Vector3d(0,0,1));//colors.add(new
                          // GL3DVec4d(0, 0, 1, 0.0));
        positions.add(tr);// normals.add(new Vector3d(0,0,1));//colors.add(new
                          // GL3DVec4d(0, 0, 1, 0.0));
        positions.add(tl);// normals.add(new Vector3d(0,0,1));//colors.add(new
                          // GL3DVec4d(0, 0, 1, 0.0));

        indices.add(0);
        indices.add(1);
        indices.add(2);

        indices.add(0);
        indices.add(2);
        indices.add(3);

        return GL3DMeshPrimitive.TRIANGLES;
    }

    private Vector3d createVertex(double x, double y, double z){
    	double cx = x * phiRotation.m[0] + y * phiRotation.m[4] + z * phiRotation.m[8] + phiRotation.m[12];
        double cy = x * phiRotation.m[1] + y * phiRotation.m[5] + z * phiRotation.m[9] + phiRotation.m[13];
        double cz = x * phiRotation.m[2] + y * phiRotation.m[6] + z * phiRotation.m[10] + phiRotation.m[14];
       return new Vector3d(cx, cy, cz);
    }
    
    public boolean hit(GL3DRay ray) {
        // if its hidden, it can't be hit
    	if (isDrawBitOn(Bit.Hidden) || this.wmI == null) {
            return false;
            
        }
       	
        // Transform ray to object space for non-groups
        ray.setOriginOS(this.wmI.multiply(ray.getOrigin()));
        ray.setDirOS(this.wmI.multiply(ray.getDirection()).normalize());
        return this.shapeHit(ray);
    }

    public boolean shapeHit(GL3DRay ray) {
        // Hit detection happens in Object-Space
        boolean isSphereHit = isSphereHit(ray);
        // boolean isSphereHit = isSphereHitInOS(ray);
        if (isSphereHit) {
            Vector3d projectionPlaneNormal = new Vector3d(0, 0, 1);
            Vector3d pointOnSphere = this.wmI.multiply(ray.getHitPoint());
            ray.setHitPointOS(pointOnSphere);
            double cos = pointOnSphere.normalize().dot(projectionPlaneNormal);

            boolean pointOnSphereBackside = cos < 0;
            // boolean pointOnSphereBackside = pointOnSphere.z<0;

            if (pointOnSphereBackside) {
                // Hit the backside of the sphere, ray must have hit the plane
                // first
                isSphereHit = this.allowBacksideHits;
                // Log.debug("GL3DHitReferenceShape: Viewing Plane from Behind! "+pointOnSphere+
                // " Projection Plane: "+ projectionPlaneNormal);
            }
        }

        if (isSphereHit) {
            ray.isOnSun = true;
            // ray.setHitPoint(this.wmI.multiply(ray.getHitPoint()));
            return true;
        } else {
            super.shapeHit(ray);
            if (ray.getHitPoint() != null)
                ray.setHitPointOS(this.wmI.multiply(ray.getHitPoint()));
        }

        return true;
    }

    private boolean isSphereHit(GL3DRay ray) {
    	Vector3d l = ray.getOrigin().scale((double)-1);
        double s = l.dot(ray.getDirection().normalize());
        double l2 = l.lengthSq();
        double r2 = Constants.SUN_RADIUS_SQ;
        if (s < 0 && l2 > r2) {
            return false;
        }

        double s2 = s * s;
        double m2 = l2 - s2;
        if (m2 > r2) {
        	return false;
        }

        double q = Math.sqrt(r2 - m2);
        double t;
        if (l2 > r2) {
            t = s - q;
        } else {
            t = s + q;
        }
        ray.setLength(t);
        ray.setHitPoint(ray.getOrigin().add(ray.getDirection().normalize().scale(t)));
        ray.isOutside = false;
        ray.setOriginShape(this);
        // Log.debug("GL3DShape.shapeHit: Hit at Distance: "+t+" HitPoint: "+ray.getHitPoint());
        return true;
    }

    public GL3DAABBox buildAABB() {
        this.aabb.fromOStoWS(new Vector3d(-LARGE_VALUE, -LARGE_VALUE, -LARGE_VALUE), new Vector3d(LARGE_VALUE, LARGE_VALUE, LARGE_VALUE), this.wm);

        return this.aabb;
    }
}
