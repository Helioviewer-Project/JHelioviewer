package org.helioviewer.jhv.base.math;

public class GL3DVec3d {
    /**
     * Predefined Vectors
     */
    public static final GL3DVec3d ZERO = new GL3DVec3d(0, 0, 0);
    public static final GL3DVec3d XAXIS = new GL3DVec3d(1, 0, 0);
    public static final GL3DVec3d YAXIS = new GL3DVec3d(0, 1, 0);
    public static final GL3DVec3d ZAXIS = new GL3DVec3d(0, 0, 1);

    /**
     * Coordinates
     */
    public final double x;
    public final double y;
    public final double z;

    // Constructors

    public GL3DVec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public GL3DVec3d(GL3DVec3d vector) {
        this.x = vector.x;
        this.y = vector.y;
        this.z = vector.z;
    }

    public GL3DVec3d() {
        this(0,0,0);
    }

    public GL3DVec3d(double[] coordinates) {
        if (coordinates == null || coordinates.length < 3) {
            throw new IllegalArgumentException("Coordinate Array must contain at least 3 dimensions");
        }
        this.x = coordinates[0];
        this.y = coordinates[1];
        this.z = coordinates[2];
    }

    public GL3DVec3d setMax(GL3DVec3d vector) {
        if(this.x>=vector.x && this.y >= vector.y && this.z>=vector.z)
            return this;
        
        return new GL3DVec3d(
                this.x > vector.x ? this.x : vector.x,
                this.y > vector.y ? this.y : vector.y,
                this.z > vector.z ? this.z : vector.z);
    }

    public GL3DVec3d setMin(GL3DVec3d vector) {
        if(this.x<=vector.x && this.y <= vector.y && this.z<=vector.z)
            return this;
        
        return new GL3DVec3d(
                this.x < vector.x ? this.x : vector.x,
                this.y < vector.y ? this.y : vector.y,
                this.z < vector.z ? this.z : vector.z);
    }

    public GL3DVec3d add(GL3DVec3d vec) {
        return new GL3DVec3d(x+vec.x,y+vec.y,z+vec.z);
    }

    public GL3DVec3d subtract(GL3DVec3d vec) {
        return new GL3DVec3d(x-vec.x,y-vec.y,z-vec.z);
    }

    public GL3DVec3d divide(double s)
    {
        if (s == 0.0)
            throw new IllegalArgumentException("Division by 0 not allowed!");
        return new GL3DVec3d(x/s,y/s,z/s);
    }

    public GL3DVec3d multiply(GL3DVec3d vec) {
        return new GL3DVec3d(x*vec.x,y*vec.y,z*vec.z);
    }

    public GL3DVec3d multiply(double s) {
        return new GL3DVec3d(x*s,y*s,z*s);
    }

    public double dot(GL3DVec3d vec) {
        return (x * vec.x) + (y * vec.y) + (z * vec.z);
    }

    public GL3DVec3d cross(GL3DVec3d vec) {
        return new GL3DVec3d(y * vec.z - z * vec.y, z * vec.x - x * vec.z, x * vec.y - y * vec.x);
    }

    public GL3DVec3d negate() {
        return new GL3DVec3d(-x,-y,-z);
    }

    public boolean isApproxEqual(GL3DVec3d vec, double tolerance) {
        return Math.abs(this.x - vec.x) <= tolerance && Math.abs(this.y - vec.y) <= tolerance && Math.abs(this.z - vec.z) <= tolerance;
    }

    public double length() {
    	double absmax = Math.max(Math.max(Math.abs(this.x), Math.abs(this.y)), Math.abs(this.z));
    	if (absmax == 0.0)
    		return 0.0;

    	double tmpx = this.x / absmax;
    	double tmpy = this.y / absmax;
    	double tmpz = this.z / absmax;
        return absmax * Math.sqrt(tmpx * tmpx + tmpy * tmpy + tmpz * tmpz);
    }

    public double length2() {
        return (x*x+y*y+z*z);
    }

    public GL3DVec3d normalize() {
    	double len = length();
        if (len == 0.0)
            return this;

   		return divide(len);
    }

    public double[] toArray() {
        return new double[] { x, y, z };
    }

    public boolean equals(Object o) {
        if (o instanceof GL3DVec3d)
            return isApproxEqual((GL3DVec3d) o, 0.0);
        return false;
    }

    public static double[] toArray(GL3DVec3d[] vecs) {
        double[] arr = new double[vecs.length * 3];
        for (int i = 0; i < vecs.length; i++) {
            GL3DVec3d v = vecs[i];
            arr[i * 3 + 0] = v.x;
            arr[i * 3 + 1] = v.y;
            arr[i * 3 + 2] = v.z;
        }
        return arr;
    }

    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
