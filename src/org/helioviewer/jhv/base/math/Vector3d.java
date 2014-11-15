package org.helioviewer.jhv.base.math;

import java.util.Locale;

/**
 * A class for three dimensional vectors with double coordinates. Instances of
 * Vector3dDouble are immutable.
 * 
 * @author Ludwig Schmidt
 * @author Malte Nuhn
 * 
 */

public final class Vector3d {
    public final double x;
    public final double y;
    public final double z;

    public Vector3d() {
        x = 0.0;
        y = 0.0;
        z = 0.0;
    }

    public Vector3d(final double newX, final double newY, final double newZ) {
        x = newX;
        y = newY;
        z = newZ;
    }

    public Vector3d(final Vector3d v) {
        x = v.x;
        y = v.y;
        z = v.z;
    }

    public Vector3d getXVector() {
        return new Vector3d(x, 0.0, 0.0);
    }

    public Vector3d getYVector() {
        return new Vector3d(0.0, y, 0.0);
    }

    public Vector3d getZVector() {
        return new Vector3d(0.0, 0.0, z);
    }

    public Vector3d add(final Vector3d v) {
        return new Vector3d(x + v.x, y + v.y, z + v.z);
    }

    public static Vector3d add(final Vector3d v1, final Vector3d v2) {
        return new Vector3d(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }

    public Vector3d subtract(final Vector3d v) {
        return new Vector3d(x - v.x, y - v.y, z - v.z);
    }

    public static Vector3d subtract(final Vector3d v1, final Vector3d v2) {
        return new Vector3d(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
    }

    public Vector3d scale(final double d) {
        return new Vector3d(x * d, y * d, z * d);
    }

    public Vector3d scale(final Vector3d v) {
        return new Vector3d(x * v.x, y * v.y, z * v.z);
    }

    public Vector3d invertedScale(final Vector3d v) {
        return new Vector3d(x / v.x, y / v.y, z / v.z);
    }

    public static Vector3d scale(final Vector3d v, final double d) {
        return new Vector3d(v.x * d, v.y * d, v.z * d);
    }

    public Vector3d negate() {
        return new Vector3d(-x, -y, -z);
    }

    public static Vector3d negate(final Vector3d v) {
        return new Vector3d(-v.x, -v.y, -v.z);
    }

    public Vector3d negateX() {
        return new Vector3d(-x, y, z);
    }

    public static Vector3d negateX(final Vector3d v) {
        return new Vector3d(-v.x, v.y, v.z);
    }

    public Vector3d negateY() {
        return new Vector3d(x, -y, z);
    }

    public static Vector3d negateY(final Vector3d v) {
        return new Vector3d(v.x, -v.y, v.z);
    }

    public Vector3d negateZ() {
        return new Vector3d(x, y, -z);
    }

    public static Vector3d negateZ(final Vector3d v) {
        return new Vector3d(v.x, v.y, -v.z);
    }

    public Vector3d crop(Vector3d min, Vector3d max) {
        return new Vector3d(Math.min(max.x, Math.max(min.x, x)), Math.min(max.y, Math.max(min.y, y)), Math.min(max.z, Math.max(min.z, z)));
    }

    public static Vector3d crop(Vector3d v, Vector3d min, Vector3d max) {
        return new Vector3d(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.y, Math.max(min.y, v.y)), Math.min(max.z, Math.max(min.z, v.z)));
    }

    public Vector3d componentMin(final Vector3d v) {
        return new Vector3d(Math.min(x, v.x), Math.min(y, v.y), Math.min(z, v.z));
    }

    public static Vector3d componentMin(final Vector3d v1, final Vector3d v2) {
        return new Vector3d(Math.min(v1.x, v2.x), Math.min(v1.y, v2.y), Math.min(v1.z, v2.z));
    }

    public Vector3d componentMax(final Vector3d v) {
        return new Vector3d(Math.max(x, v.x), Math.max(y, v.y), Math.max(z, v.z));
    }

    public static Vector3d componentMax(final Vector3d v1, final Vector3d v2) {
        return new Vector3d(Math.max(v1.x, v2.x), Math.max(v1.y, v2.y), Math.max(v1.z, v2.z));
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double lengthSq() {
        return x * x + y * y + z * z;
    }

    public Vector3d normalize() {
        double length = this.length();
        return new Vector3d(x / length, y / length, z / length);
    }

    public static Vector3d normalize(final Vector3d v) {
        double length = v.length();
        return new Vector3d(v.x / length, v.y / length, v.z / length);
    }

    public static double dot(final Vector3d v1, final Vector3d v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    public static Vector3d cross(final Vector3d v1, final Vector3d v2) {
        double x1 = v1.y * v2.z - v1.z * v2.y;
        double x2 = v1.z * v2.x - v1.x * v2.z;
        double x3 = v1.x * v2.y - v1.y * v2.x;

        return new Vector3d(x1, x2, x3);
    }

    public Vector3d absolute() {
        return new Vector3d(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    public static Vector3d absolute(final Vector3d v) {
        return new Vector3d(Math.abs(v.x), Math.abs(v.y), Math.abs(v.z));
    }

    public boolean equals(final Object o) {
        if (!(o instanceof Vector3d)) {
            return false;
        }
        Vector3d v = (Vector3d) o;

        return Double.compare(x, v.x) == 0 && Double.compare(y, v.y) == 0 && Double.compare(z, v.z) == 0;
    }

    public boolean epsilonEquals(final Vector3d v, final double epsilon) {
        return Math.abs(x - v.x) < epsilon && Math.abs(y - v.y) < epsilon && Math.abs(z - v.z) < epsilon;
    }

    public int hashCode() {
        long h=Double.doubleToRawLongBits(x) ^ Double.doubleToRawLongBits(y)<<20 ^ Double.doubleToRawLongBits(z)<<40
                 ^ Double.doubleToRawLongBits(y)>>40 ^ Double.doubleToRawLongBits(z)>>20;
        
        return (int)(h ^(h>>32));
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "(%.2f,%.2f,%.2f)", x, y, z);
    }
}
