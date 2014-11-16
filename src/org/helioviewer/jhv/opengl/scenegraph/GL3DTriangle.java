package org.helioviewer.jhv.opengl.scenegraph;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;

/**
 * A representation of a triangle, basically used as the most basic primitive
 * when calculating hit points of a {@link GL3DMesh}. Every Mesh is also stored
 * as a set of {@link GL3DTriangle}s.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DTriangle {
    protected Vector3d a;
    protected Vector3d b;
    protected Vector3d c;

    public GL3DTriangle() {

    }

    public GL3DTriangle(Vector3d a, Vector3d b, Vector3d c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }

    private final static double EPSILON = 0.0001;

    public boolean intersects(GL3DRay ray) {
        Vector3d e1, e2; // edge 1 and 2
        Vector3d AO, K, Q;

        // find vectors for two edges sharing the triangle vertex A
        e1 = this.b.subtract(this.a);
        e2 = this.c.subtract(this.a);

        // begin calculating determinant - also used to calculate U parameter
        K = ray.getDirectionOS().cross(e2);

        // if determinant is near zero, ray lies in plane of triangle
        final double det = e1.dot(K);

        double inv_det, t, u, v;

        if (det < EPSILON && det > -EPSILON)
            return false;

        inv_det = 1.0f / det;

        // calculate distance from A to ray origin
        AO = ray.getOriginOS().subtract(a);

        // Calculate barycentric coordinates: u>0 && v>0 && u+v<=1
        u = AO.dot(K) * inv_det;
        if (u < 0.0f || u > 1.0)
            return false;

        // prepare to test v parameter
        Q = AO.cross(e1);

        // calculate v parameter and test bounds
        v = Q.dot(ray.getDirectionOS()) * inv_det;
        if (v < 0.0f || u + v > 1.0f)
            return false;

        // calculate t, ray intersects triangle
        t = e2.dot(Q) * inv_det;

        // if intersection is closer replace ray intersection parameters
        if (t > ray.getLength() || t < 0.0f)
            return false;

        ray.setLength(t);

        return true;
    }
}
