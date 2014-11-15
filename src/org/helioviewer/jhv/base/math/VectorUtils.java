package org.helioviewer.jhv.base.math;

public class VectorUtils {

    /**
     * Helper routine needed for testing if a point is inside a triangle
     * 
     * @param a
     *            - first point
     * @param b
     *            - second point
     * @param c
     *            - third point
     * 
     * @return number representing the turn-direction of the three points
     */
    public static double clockwise(Vector2d a, Vector2d b, Vector2d c) {
        return (c.x - a.x) * (b.y - a.y) - (b.x - a.x) * (c.y - a.y);
    }

    /**
     * Check if a given point lies inside the given triangle
     * 
     * @param trianglePointA
     *            - first triangle point
     * @param trianglePointB
     *            - second triangle point
     * @param trianglePointC
     *            - third triangle point
     * @param toCheck
     *            - point in question
     * @return true if the point is located inside the triangle
     */
    public static boolean pointInTriangle(Vector2d trianglePointA, Vector2d trianglePointB, Vector2d trianglePointC, Vector2d toCheck) {
        double cw0 = clockwise(trianglePointA, trianglePointB, toCheck);
        double cw1 = clockwise(trianglePointB, trianglePointC, toCheck);
        double cw2 = clockwise(trianglePointC, trianglePointA, toCheck);

        cw0 = Math.abs(cw0) < Double.MIN_NORMAL ? 0 : cw0 < 0 ? -1 : 1;
        cw1 = Math.abs(cw1) < Double.MIN_NORMAL ? 0 : cw1 < 0 ? -1 : 1;
        cw2 = Math.abs(cw2) < Double.MIN_NORMAL ? 0 : cw2 < 0 ? -1 : 1;

        if (Math.abs(cw0 + cw1 + cw2) >= 2) {
            return true;
        } else {
            return Math.abs(cw0) + Math.abs(cw1) + Math.abs(cw2) <= 1;
        }

    }

    /**
     * Project the given 2d in-plane coordinates back to the 3d space
     * 
     * @param planeCenter
     *            - define the center of the plane
     * @param planeVectorA
     *            - first in-plane direction vector
     * @param planeVectorB
     *            - second in-plane direction vector
     * @param toProject
     *            - point to be projected into the plane
     * @return
     */
    public static Vector3d projectBack(Vector3d planeCenter, Vector3d planeVectorA, Vector3d planeVectorB, Vector2d toProject) {
        Vector3d inPlane = planeCenter.add(planeVectorA.scale(toProject.x)).add(planeVectorB.scale(toProject.y));
        return inPlane;
    }

    /**
     * Get the (projected) in-plane coordinates of the given point
     * 
     * @param planeCenter
     *            - define the center of the plane
     * @param planeVectorA
     *            - first in-plane direction vector
     * @param planeVectorB
     *            - second in-plane direction vector
     * @param toProject
     *            - point to be projected into the plane
     * @return
     */
    public static Vector2d inPlaneCoord(Vector3d planeCenter, Vector3d planeVectorA, Vector3d planeVectorB, Vector3d toProject) {
        Vector3d inPlane = projectToPlane(planeCenter, toProject);
        double x = Vector3d.dot(planeVectorA, inPlane);
        double y = Vector3d.dot(planeVectorB, inPlane);
        return new Vector2d(x, y);
    }

    /**
     * Calculate the in-plane-vector of the given point to the plane with origin
     * planeCenter and normal norm(planeCenter)
     * 
     * @param planeCenter
     *            - the plane's normal vector
     * @param toProject
     *            - point to project
     * 
     * @return the projection of the targetPoint
     */
    public static Vector3d inPlaneShift(Vector3d planeCenter, Vector3d toProject) {
        Vector3d normal = planeCenter.normalize();
        Vector3d inPlaneShift = toProject.subtract(normal.scale(Vector3d.dot(normal, toProject)));
        return inPlaneShift;
    }

    /**
     * Calculate the projection of the given point to the plane with origin
     * planeCenter and normal norm(planeCenter)
     * 
     * @param planeCenter
     * @param toProject
     * @return
     */
    public static Vector3d projectToPlane(Vector3d planeCenter, Vector3d toProject) {
        Vector3d normal = planeCenter.normalize();
        Vector3d inPlaneShift = toProject.subtract(normal.scale(Vector3d.dot(normal, toProject)));
        Vector3d projection = planeCenter.add(inPlaneShift);
        return projection;
    }

}
