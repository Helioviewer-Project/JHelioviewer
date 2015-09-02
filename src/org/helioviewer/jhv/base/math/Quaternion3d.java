package org.helioviewer.jhv.base.math;


public class Quaternion3d {
    private static final double EPSILON = 0.000001;

    private final double a;
    private final Vector3d u;

    public static Quaternion3d createRotation(double angle, Vector3d axis)
    {
        double halfAngle = angle / 2;
        return new Quaternion3d(Math.cos(halfAngle), axis.normalize().scale(Math.sin(halfAngle)));
    }

    public Quaternion3d(double a, double x, double y, double z)
    {
        this(a, new Vector3d(x, y, z));
    }

    public Quaternion3d(double a, Vector3d u)
    {
        this.a = a;
        this.u = u;
    }

    public Quaternion3d()
    {
    	this(1, new Vector3d(0, 0, 0));
    }

    public Quaternion3d multiply(Quaternion3d q)
    {
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
        
        return new Quaternion3d(a, x, y, z);
    }

    public Matrix4d toMatrix()
    {
        double w = a, w2 = w * w;
        double x = u.x, x2 = x * x;
        double y = u.y, y2 = y * y;
        double z = u.z, z2 = z * z;

        return new Matrix4d(
        		w2 + x2 - y2 - z2, 2 * x * y - 2 * w * z, 2 * x * z + 2 * w * y, 0,
		        2 * x * y + 2 * w * z, w2 - x2 + y2 - z2, 2 * y * z - 2 * w * x, 0,
		        2 * x * z - 2 * w * y, 2 * y * z + 2 * w * x, w2 - x2 - y2 + z2, 0,
		        0, 0, 0, w2 + x2 + y2 + z2);
    }

    public double getAngle()
    {
        return this.a;
    }

    public Vector3d getRotationAxis()
    {
        return this.u;
    }

    public Quaternion3d add(Quaternion3d q)
    {
        return new Quaternion3d(a + q.a, this.u.add(q.u));
    }

    public Quaternion3d subtract(Quaternion3d q)
    {
        return new Quaternion3d(a - q.a, this.u.subtract(q.u));
    }

    public Quaternion3d scale(double s)
    {
        return new Quaternion3d(a * s, this.u.scale(s));
    }

    public Quaternion3d rotate(Quaternion3d q2)
    {
        double angle = this.a * q2.a - this.u.x * q2.u.x - this.u.y * q2.u.y - this.u.z * q2.u.z;
        Vector3d axis = new Vector3d(
                this.a * q2.u.x + this.u.x * q2.a + this.u.y * q2.u.z - this.u.z * q2.u.y,
                this.a * q2.u.y + this.u.y * q2.a + this.u.z * q2.u.x - this.u.x * q2.u.z,
                this.a * q2.u.z + this.u.z * q2.a + this.u.x * q2.u.y - this.u.y * q2.u.x);
       	
        return new Quaternion3d(angle, axis).normalized();
       	
    }

    public Quaternion3d slerp(Quaternion3d r, double t)
    {
        double cosAngle = dot(r);
        if (cosAngle > 1 - EPSILON)
        {
            Quaternion3d result = r.add(this.subtract(r).scale(t));
            return result.normalized();
        }

        if (cosAngle < 0)
            cosAngle = 0;
        if (cosAngle > 1)
            cosAngle = 1;

        double theta0 = Math.acos(cosAngle);
        double theta = theta0 * t;
        Quaternion3d v2 = r.subtract(this.scale(cosAngle)).normalized();

        Quaternion3d q = this.scale(Math.cos(theta)).add(v2.scale(Math.sin(theta)));
        return q.normalized();
    }


    public Quaternion3d normalized()
    {
        double l = this.length();
        return new Quaternion3d(a / l, u.scale(1/l));
    }

    public double length()
    {
        return Math.sqrt(length2());
    }

    public double length2()
    {
        return a * a + u.lengthSq();
    }

    public double dot(Quaternion3d q)
    {
        return this.a * q.a + this.u.x * q.u.x + this.u.y * q.u.y + this.u.z * q.u.z;
    }

    public static Quaternion3d calcRotation(Vector3d startPoint, Vector3d endPoint)
    {
    	Vector3d rotationAxis = startPoint.cross(endPoint);
        double rotationAngle = Math.atan2(rotationAxis.length(), startPoint.dot(endPoint));
        if (rotationAngle == 0.0) rotationAxis = new Vector3d(0,0,1);
        return Quaternion3d.createRotation(rotationAngle, rotationAxis);
    }

    public Quaternion3d inversed()
    {
    	Quaternion3d q = this.conjugated();
    	double length = q.a*q.a + q.u.x*q.u.x + q.u.y * q.u.y + q.u.z*q.u.z;
    	
    	return new Quaternion3d(q.a / length, q.u.scale(1/length));
    }
    
    public Quaternion3d conjugated()
    {
        return new Quaternion3d(this.a, new Vector3d(-u.x,-u.y,-u.z));
    }

    public String toString()
    {
        return "[" + a + ", " + u.x + ", " + u.y + ", " + u.z + "]";
    }
}
