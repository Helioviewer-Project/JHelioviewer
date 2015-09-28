package org.helioviewer.jhv.base.math;

import java.awt.Point;
import java.util.Locale;

/**
 * A class for two dimensional vectors with double coordinates. Instances of
 * Vector2dDouble are immutable.
 */

public final class Vector2d {

    public static final Vector2d NULL_VECTOR = new Vector2d(0, 0);
    public static final Vector2d NEGATIVE_INFINITY_VECTOR = new Vector2d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
    public static final Vector2d POSITIVE_INFINITY_VECTOR = new Vector2d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    public final double x;

    public final double y;

    public Vector2d() {
        x = 0.0;
        y = 0.0;
    }

    public Vector2d(final double newX, final double newY) {
        x = newX;
        y = newY;
    }

    public Vector2d(final Point p) {
        x = (double) p.x;
        y = (double) p.y;
    }

    public Vector2d(final Vector2d v) {
        x = v.x;
        y = v.y;
    }

    public Vector2d(final Vector2i v) {
        x = (double) v.getX();
        y = (double) v.getY();
    }

    public Vector2d getXVector() {
        return new Vector2d(x, 0.0);
    }

    public Vector2d getYVector() {
        return new Vector2d(0.0, y);
    }

    public Point toPoint() {
        return new Point((int) Math.round(x), (int) Math.round(y));
    }

    public Vector2d add(final Vector2d v) {
        return new Vector2d(x + v.x, y + v.y);
    }

    public static Vector2d add(final Vector2d v1, final Vector2d v2) {
        return new Vector2d(v1.x + v2.x, v1.y + v2.y);
    }

    public Vector2d subtract(final Vector2d v) {
        return new Vector2d(x - v.x, y - v.y);
    }

    public static Vector2d subtract(final Vector2d v1, final Vector2d v2) {
        return new Vector2d(v1.x - v2.x, v1.y - v2.y);
    }

    public Vector2d scale(final double d) {
        return new Vector2d(x * d, y * d);
    }

    public Vector2d scale(final Vector2d v) {
        return new Vector2d(x * v.x, y * v.y);
    }

    public Vector2d invertedScale(final Vector2d v) {
        return new Vector2d(x / v.x, y / v.y);
    }

    public static Vector2d scale(final Vector2d v, final double d) {
        return new Vector2d(v.x * d, v.y * d);
    }
    
    public static Vector2d scale(final Vector2i v, final double d){
    	return new Vector2d(v.getX() * d, v.getY() * d);
    }

    public Vector2d negate() {
        return new Vector2d(-x, -y);
    }

    public static Vector2d negate(final Vector2d v) {
        return new Vector2d(-v.x, -v.y);
    }

    public Vector2d negateX() {
        return new Vector2d(-x, y);
    }

    public static Vector2d negateX(final Vector2d v) {
        return new Vector2d(-v.x, v.y);
    }

    public Vector2d negateY() {
        return new Vector2d(x, -y);
    }

    public static Vector2d negateY(final Vector2d v) {
        return new Vector2d(v.x, -v.y);
    }

    public Vector2d crop(Vector2d min, Vector2d max) {
        return new Vector2d(Math.min(max.x, Math.max(min.x, x)), Math.min(max.y, Math.max(min.y, y)));
    }

    public static Vector2d crop(Vector2d v, Vector2d min, Vector2d max) {
        return new Vector2d(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.x, Math.max(min.x, v.y)));
    }

    public Vector2d componentMin(final Vector2d v) {
        return new Vector2d(Math.min(x, v.x), Math.min(y, v.y));
    }

    public static Vector2d componentMin(final Vector2d v1, final Vector2d v2) {
        return new Vector2d(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y));
    }

    public Vector2d componentMax(final Vector2d v) {
        return new Vector2d(Math.max(x, v.x), Math.max(y, v.y));
    }

    public static Vector2d componentMax(final Vector2d v1, final Vector2d v2) {
        return new Vector2d(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y));
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public double lengthSq() {
        return x * x + y * y;
    }

    public Vector2d normalize() {
        double length = Math.sqrt(x * x + y * y);
        return new Vector2d(x / length, y / length);
    }

    public static Vector2d normalize(final Vector2d v) {
        double length = Math.sqrt(v.x * v.x + v.y * v.y);
        return new Vector2d(v.x / length, v.y / length);
    }

    public double dot(final Vector2d v) {
        return x * v.x + y * v.y;
    }
    
    public static double angle(final Vector2d v1, final Vector2d v2) {
        return Math.acos((v1.x * v2.x + v1.y * v2.y) / ((Math.sqrt(v1.x * v1.x + v1.y * v1.y)) * (Math.sqrt(v2.x * v2.x + v2.y * v2.y))));
    }

    public Vector2d absolute() {
        return new Vector2d(Math.abs(x), Math.abs(y));
    }

    public static Vector2d absolute(final Vector2d v) {
        return new Vector2d(Math.abs(v.x), Math.abs(v.y));
    }

    public boolean equals(final Object o) {
        if (!(o instanceof Vector2d)) {
            return false;
        }
        Vector2d v = (Vector2d) o;

        return Double.compare(x, v.x) == 0 && Double.compare(y, v.y) == 0;
    }

    public boolean epsilonEquals(final Vector2d v, final double epsilon) {
        return Math.abs(x - v.x) < epsilon && Math.abs(y - v.y) < epsilon;
    }

    /**
     * The multiplier used for the hash code computation.
     */
    private static final int HASH_CODE_MULTIPLIER = 31;

    /**
     * The initial value used for the hash code computation.
     */
    private static final int HASH_CODE_INITIAL_VALUE = 17;

    /**
     * The number of bits in an integer, used to compute a has value of the
     * double values.
     */
    private static final int HASH_CODE_INT_BITS = 32;

    public int hashCode() {
        long xBits = Double.doubleToLongBits(x);
        long yBits = Double.doubleToLongBits(y);
        return HASH_CODE_INITIAL_VALUE * HASH_CODE_MULTIPLIER * HASH_CODE_MULTIPLIER + HASH_CODE_MULTIPLIER * (int) (xBits ^ (xBits >>> HASH_CODE_INT_BITS)) + (int) (yBits ^ (yBits >>> HASH_CODE_INT_BITS));
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "(%f,%f)", x, y);
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
     * @return
     */
    public Vector3d projectBack(Vector3d planeCenter, Vector3d planeVectorA, Vector3d planeVectorB) {
        return planeCenter.add(planeVectorA.scale(x)).add(planeVectorB.scale(y));
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
     * @return true if the point is located inside the triangle
     */
    public boolean isInTriangle(Vector2d trianglePointA, Vector2d trianglePointB, Vector2d trianglePointC) {
        double cw0 = clockwise(trianglePointA, trianglePointB, this);
        double cw1 = clockwise(trianglePointB, trianglePointC, this);
        double cw2 = clockwise(trianglePointC, trianglePointA, this);
    
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

    public double cross(Vector2d v1)
    {
        return this.x * v1.y - this.y * v1.x;
    }
}
