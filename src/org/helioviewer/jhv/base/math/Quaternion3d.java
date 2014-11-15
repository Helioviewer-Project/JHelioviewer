package org.helioviewer.jhv.base.math;

public class Quaternion3d {
    public static final double EPSILON = 0.000001;

    protected double a;

    protected GL3DVec3d u;

    public static Quaternion3d createRotation(double angle, GL3DVec3d axis) {
        double halfAngle = angle / 2;
        return new Quaternion3d(Math.cos(halfAngle), axis.normalize().multiply(Math.sin(halfAngle)));
    }

    public Quaternion3d(double a, double x, double y, double z) {
        this(a, new GL3DVec3d(x, y, z));
    }

    public Quaternion3d(double a, GL3DVec3d u) {
        this.a = a;
        this.u = u;
    }

    public void clear() {
        Quaternion3d q = Quaternion3d.createRotation(0.0, new GL3DVec3d(0, 1, 0));
        this.a = q.a;
        this.u = q.u;
    }

    public Quaternion3d multiply(Quaternion3d q) {
        double a1 = this.a;
        double x1 = this.u.x;
        double y1 = this.u.y;
        double z1 = this.u.z;
        double a2 = q.a;
        double x2 = q.u.x;
        double y2 = q.u.y;
        double z2 = q.u.z;

        double a = (a1 * a2 - x1 * x2 - y1 * y2 - z1 * z2);
        double x = (a1 * x2 + x1 * a2 - y1 * z2 + z1 * y2);
        double y = (a1 * y2 + x1 * z2 + y1 * a2 - z1 * x2);
        double z = (a1 * z2 - x1 * y2 + y1 * x2 + z1 * a2);
        Quaternion3d res = new Quaternion3d(a, x, y, z);

        return res;
    }

    public Matrix4d toMatrix() {
        double w = a, w2 = w * w;
        double x = u.x, x2 = x * x;
        double y = u.y, y2 = y * y;
        double z = u.z, z2 = z * z;

        return new Matrix4d(w2 + x2 - y2 - z2, 2 * x * y - 2 * w * z, 2 * x * z + 2 * w * y, 0,

        2 * x * y + 2 * w * z, w2 - x2 + y2 - z2, 2 * y * z - 2 * w * x, 0,

        2 * x * z - 2 * w * y, 2 * y * z + 2 * w * x, w2 - x2 - y2 + z2, 0,

        0, 0, 0, w2 + x2 + y2 + z2);
        /*
         * return new GL3DMat4d( w2+x2-y2-z2, 2*x*y+2*w*z, 2*x*z-2*w*y, 0,
         * 
         * 2*x*y-2*w*z, w2-x2+y2-z2, 2*y*z+2*w*x, 0,
         * 
         * 2*x*z+2*w*y, 2*y*z-2*w*x, w2-x2-y2+z2, 0,
         * 
         * 0, 0, 0, 1 );
         */
    }

    public double getAngle() {
        return this.a;
    }

    public GL3DVec3d getRotationAxis() {
        return this.u;
    }

    // public GL3DQuatd interpolate(GL3DQuatd q) {
    // double a = this.a + q.a/2;
    // GL3DVec3d u = this.u.copy().add(q.u).divide(2);
    // return new GL3DQuatd(a, u);
    //

    public Quaternion3d add(Quaternion3d q) {
        this.u.add(q.u);
        this.a += q.a;
        return this;
    }

    public Quaternion3d subtract(Quaternion3d q) {
        this.u.subtract(q.u);
        this.a -= q.a;
        return this;
    }

    public Quaternion3d scale(double s) {
        this.a *= s;
        this.u.multiply(s);
        return this;
    }

    public void rotate(Quaternion3d q2) {
        Quaternion3d q1 = this.copy();

        this.a = q1.a * q2.a - q1.u.x * q2.u.x - q1.u.y * q2.u.y - q1.u.z * q2.u.z;
        this.u.x = q1.a * q2.u.x + q1.u.x * q2.a + q1.u.y * q2.u.z - q1.u.z * q2.u.y;
        this.u.y = q1.a * q2.u.y + q1.u.y * q2.a + q1.u.z * q2.u.x - q1.u.x * q2.u.z;
        this.u.z = q1.a * q2.u.z + q1.u.z * q2.a + q1.u.x * q2.u.y - q1.u.y * q2.u.x;
       	this.normalize();
    }

    public Quaternion3d slerp(Quaternion3d r, double t) {
        double cosAngle = dot(r);
        if (cosAngle > 1 - EPSILON) {
            Quaternion3d result = r.copy().add(this.copy().subtract(r).scale(t));
            result.normalize();
            return result;
        }

        if (cosAngle < 0)
            cosAngle = 0;
        if (cosAngle > 1)
            cosAngle = 1;

        double theta0 = Math.acos(cosAngle);
        double theta = theta0 * t;
        Quaternion3d v2 = r.copy().subtract(this.copy().scale(cosAngle));
        v2.normalize();

        Quaternion3d q = this.copy().scale(Math.cos(theta)).add(v2.scale(Math.sin(theta)));
        q.normalize();
        return q;
    }

    public void set(Quaternion3d q) {
        this.a = q.a;
        this.u = q.u;
    }

    public Quaternion3d normalize() {
        double l = this.length();
        a /= l;
        u.divide(l);
        
        return this;
    }

    public double length() {
        return Math.sqrt(length2());
    }

    public double length2() {
        return a * a + u.length2();
    }

    public double dot(Quaternion3d q) {
        return this.a * q.a + this.u.x * q.u.x + this.u.y * q.u.y + this.u.z * q.u.z;
    }

    public static Quaternion3d calcRotation(GL3DVec3d startPoint, GL3DVec3d endPoint){
    	GL3DVec3d rotationAxis = GL3DVec3d.cross(startPoint, endPoint);
        double rotationAngle = Math.atan2(rotationAxis.length(), GL3DVec3d.dot(startPoint, endPoint));

        return Quaternion3d.createRotation(rotationAngle, rotationAxis.copy());
    }

    public Quaternion3d copy() {
        return new Quaternion3d(this.a, this.u.copy());
    }
    
    public Quaternion3d inverse(){
    	this.conjugate();
    	double length = a*a + u.x*u.x + u.y * u.y + u.z*u.z;
    	
    	a /= length;
    	u.divide(length);
    	
    	return this;
    }
    
    public void conjugate(){
    	u.x = -u.x;
    	u.y = -u.y;
    	u.z = -u.z;
    }

    public String toString() {
        return "[" + a + ", " + u.x + ", " + u.y + ", " + u.z + "]";
    }
}
