package org.helioviewer.jhv.opengl.scenegraph.visuals;

import java.util.List;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.opengl.scenegraph.GL3DMesh;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;

public class GL3DSphere extends GL3DMesh {
    private int resolutionX;
    private int resolutionY;
    private double radius;

    private Vector3d center;
    private Vector3d centerOS = new Vector3d(0, 0, 0);

    public GL3DSphere(double radius, int resolutionX, int resolutionY, Vector4d color) {
        this("Sphere", radius, resolutionX, resolutionY, color);
    }

    public GL3DSphere(String name, double radius, int resolutionX, int resolutionY, Vector4d color) {
        super(name, color);
        this.radius = radius;
        this.resolutionX = resolutionX;
        this.resolutionY = resolutionY;
    }

    public GL3DMeshPrimitive createMesh(GL3DState state, List<Vector3d> positions, List<Vector3d> normals, List<Vector2d> textCoords, List<Integer> indices, List<Vector4d> colors) {

        for (int latNumber = 0; latNumber <= this.resolutionX; latNumber++) {
            double theta = latNumber * Math.PI / resolutionX;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);

            for (int longNumber = 0; longNumber <= resolutionY; longNumber++) {
                double phi = longNumber * 2 * Math.PI / resolutionY;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);

                double x = cosPhi * sinTheta;
                double y = cosTheta;
                double z = sinPhi * sinTheta;

                positions.add(new Vector3d(radius * x, radius * y, radius * z));
                normals.add(new Vector3d(x, y, z));
            }
        }

        for (int latNumber = 0; latNumber < this.resolutionX; latNumber++) {
            for (int longNumber = 0; longNumber < resolutionY; longNumber++) {
                int first = (latNumber * (resolutionY + 1)) + longNumber;
                int second = first + resolutionY + 1;

                indices.add(first);
                indices.add(first + 1);
                indices.add(second + 1);
                indices.add(second);
                
                // indices.add(second);
                // indices.add(first + 1);
            }
        }
        
        return GL3DMeshPrimitive.QUADS;
    }

    public void shapeInit(GL3DState state) {
        super.shapeInit(state);
        this.center = this.wm.multiply(this.centerOS);
    }

    public void shapeUpdate(GL3DState state) {
        this.center = this.wm.multiply(this.centerOS);
    }

    public boolean shapeHit(GL3DRay ray) {
        Vector3d l = this.center.subtract(ray.getOrigin());
        double s = l.dot(ray.getDirection().normalize());
        double l2 = l.lengthSq();
        double r2 = this.radius * this.radius;
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
        // ray.setHitPoint(this.wmI.multiply(ray.getHitPoint()));
        ray.isOutside = false;
        ray.setOriginShape(this);
        // Log.debug("GL3DShape.shapeHit: Hit at Distance: "+t+" HitPoint: "+ray.getHitPoint());
        return true;
    }

    public Vector3d getCenter() {
        return center;
    }
}
