package org.helioviewer.jhv.base.math;

import java.awt.Point;
import java.util.Locale;

/**
 * A class for two dimensional vectors with integer coordinates. Instances of
 * Vector2dInt are immutable.
 * 
 * The restriction to integer coordinates might lead to overflows in some
 * calculations. Consider using Vector2dDouble or Vector2dLong instead.
 */
public final class Vector2i
{
    public final int x;
    public final int y;

    public Vector2i()
    {
        x = 0;
        y = 0;
    }

    public Vector2i(final int newX, final int newY)
    {
        x = newX;
        y = newY;
    }

    public Vector2i(final Point p)
    {
        x = p.x;
        y = p.y;
    }

    public Vector2i(final Vector2i v)
    {
        x = v.x;
        y = v.y;
    }

    public Vector2i(final Vector2d v)
    {
        x = (int) Math.round(v.x);
        y = (int) Math.round(v.y);
    }

    public Vector2i getXVector() {
        return new Vector2i(x, 0);
    }

    public Vector2i getYVector() {
        return new Vector2i(0, y);
    }

    public Point toPoint() {
        return new Point(x, y);
    }

    public Vector2i add(final Vector2i v) {
        return new Vector2i(x + v.x, y + v.y);
    }

    public static Vector2i add(final Vector2i v1, final Vector2i v2) {
        return new Vector2i(v1.x + v2.x, v1.y + v2.y);
    }

    public Vector2i subtract(final Vector2i v) {
        return new Vector2i(x - v.x, y - v.y);
    }

    public static Vector2i subtract(final Vector2i v1, final Vector2i v2) {
        return new Vector2i(v1.x - v2.x, v1.y - v2.y);
    }

    public Vector2i scale(final int s) {
        return new Vector2i(x * s, y * s);
    }

    public static Vector2i scale(final Vector2i v, final int s) {
        return new Vector2i(v.x * s, v.y * s);
    }

    public Vector2i scale(final double d) {
        return new Vector2i((int) Math.round(x * d), (int) Math.round(y * d));
    }

    public static Vector2i scale(final Vector2i v, final double d) {
        return new Vector2i((int) Math.round(v.x * d), (int) Math.round(v.y * d));
    }

    public Vector2i negate() {
        return new Vector2i(-x, -y);
    }

    public static Vector2i negate(final Vector2i v) {
        return new Vector2i(-v.x, -v.y);
    }

    public Vector2i negateX() {
        return new Vector2i(-x, y);
    }

    public static Vector2i negateX(final Vector2i v) {
        return new Vector2i(-v.x, v.y);
    }

    public Vector2i negateY() {
        return new Vector2i(x, -y);
    }

    public static Vector2i negateY(final Vector2i v) {
        return new Vector2i(v.x, -v.y);
    }

    public Vector2i crop(Vector2i min, Vector2i max) {
        return new Vector2i(Math.min(max.x, Math.max(min.x, x)), Math.min(max.y, Math.max(min.y, y)));
    }

    public static Vector2i crop(Vector2i v, Vector2i min, Vector2i max) {
        return new Vector2i(Math.min(max.x, Math.max(min.x, v.x)), Math.min(max.x, Math.max(min.x, v.y)));
    }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    public int lengthSq() {
        return x * x + y * y;
    }

    public Vector2i normalize() {
        double length = Math.sqrt(x * x + y * y);
        return new Vector2i((int) Math.round(x / length), (int) Math.round(y / length));
    }

    public static Vector2i normalize(final Vector2i v) {
        double length = Math.sqrt(v.x * v.x + v.y * v.y);
        return new Vector2i((int) Math.round(v.x / length), (int) Math.round(v.y / length));
    }

    public static int dot(final Vector2i v1, final Vector2i v2) {
        return v1.x * v2.x + v1.y * v2.y;
    }

    public static double angle(final Vector2i v1, final Vector2i v2) {
        return Math.acos((v1.x * v2.x + v1.y * v2.y) / ((Math.sqrt(v1.x * v1.x + v1.y * v1.y)) * (Math.sqrt(v2.x * v2.x + v2.y * v2.y))));
    }

    public Vector2i absolute() {
        return new Vector2i(Math.abs(x), Math.abs(y));
    }

    public static Vector2i absolute(final Vector2i v) {
        return new Vector2i(Math.abs(v.x), Math.abs(v.y));
    }

    public boolean equals(final Object o) {
        if (!(o instanceof Vector2i)) {
            return false;
        }
        Vector2i v = (Vector2i) o;
        return v.x == x && v.y == y;
    }

    /**
     * The multiplier used for the hash code computation.
     */
    private static final int HASH_CODE_MULTIPLIER = 31;

    /**
     * The initial value used for the hash code computation.
     */
    private static final int HASH_CODE_INITIAL_VALUE = 17;

    public int hashCode() {
        return HASH_CODE_INITIAL_VALUE * HASH_CODE_MULTIPLIER * HASH_CODE_MULTIPLIER + HASH_CODE_MULTIPLIER * x + y;
    }

    public String toString() {
        return String.format(Locale.ENGLISH, "(%d,%d)", x, y);
    }
}
