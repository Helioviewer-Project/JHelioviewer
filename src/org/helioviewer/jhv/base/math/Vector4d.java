package org.helioviewer.jhv.base.math;

public class Vector4d
{
    /**
     * Coordinates
     */
    public final double x;
    public final double y;
    public final double z;
    public final double w;

    public Vector4d(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4d() {
        this(0f, 0f, 0f, 0f);
    }

    public String toString() {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

}
