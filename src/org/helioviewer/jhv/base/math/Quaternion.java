package org.helioviewer.jhv.base.math;


public class Quaternion
{
	public static final Quaternion IDENTITY = new Quaternion(1,new Vector3d(0,0,0));
    private static final double EPSILON = 0.000001;

    public final double a;
    public final Vector3d u;

    public static Quaternion createRotation(double _angle, Vector3d _axis)
    {
        return new Quaternion(Math.cos(_angle / 2), _axis.normalized().scaled(Math.sin(_angle / 2)));
    }

    @Deprecated
    public Quaternion(double _a, Vector3d _u)
    {
        a = _a;
        u = _u;
    }
    
    @Deprecated
    public Quaternion(double _a, double _x,double _y,double _z)
    {
    	a=_a;
    	u = new Vector3d(_x,_y,_z);
    }

    public Quaternion(double _longitude, double _latitude)
	{
        double sinLong = Math.sin(_longitude/2);
        double cosLong = Math.cos(_longitude/2);
        double sinLat = Math.sin(_latitude/2);
        double cosLat = Math.cos(_latitude/2);
        
        a = cosLat * cosLong;
        u = new Vector3d(sinLat * cosLong, cosLat * sinLong, sinLat * sinLong);
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

    public Vector3d getRotationAxis()
    {
        return u.normalized();
    }
    
    public double getAngle()
    {
    	return Math.atan2(u.length(),a) * 2;
    }

    public Quaternion add(Quaternion _q)
    {
        return new Quaternion(a + _q.a, u.add(_q.u));
    }

    public Quaternion subtract(Quaternion _q)
    {
        return new Quaternion(a - _q.a, u.subtract(_q.u));
    }

    public Quaternion scaled(double _s)
    {
        return new Quaternion(a * _s, u.scaled(_s));
    }

    public Quaternion multiply(Quaternion _q)
    {
    	double ra = (a * _q.a - u.x * _q.u.x - u.y * _q.u.y - u.z * _q.u.z);
    	double rx = (a * _q.u.x + u.x * _q.a - u.y * _q.u.z + u.z * _q.u.y);
    	double ry = (a * _q.u.y + u.x * _q.u.z + u.y * _q.a - u.z * _q.u.x);
    	double rz = (a * _q.u.z - u.x * _q.u.y + u.y * _q.u.x + u.z * _q.a);
    	
    	return new Quaternion(ra, new Vector3d(rx, ry, rz));
    }
    
    public Quaternion rotated(Quaternion _q)
    {
        double angle =
        		a * _q.a - u.x * _q.u.x - u.y * _q.u.y - u.z * _q.u.z;
        
        Vector3d axis = new Vector3d(
                a * _q.u.x + u.x * _q.a + u.y * _q.u.z - u.z * _q.u.y,
                a * _q.u.y + u.y * _q.a + u.z * _q.u.x - u.x * _q.u.z,
                a * _q.u.z + u.z * _q.a + u.x * _q.u.y - u.y * _q.u.x);
       	
        return new Quaternion(angle, axis).normalized();
    }
    
    public Vector3d toEuler()
    {
     	return new Vector3d( 
     		Math.atan2(2 * (a * u.x + u.y * u.z), 1 - 2 * (u.x * u.x + u.y * u.y)),
     		Math.asin(2 * (a * u.y - u.z * u.x)),
     		Math.atan2(2 * (a * u.z + u.x * u.y), 1 - 2 * (u.y * u.y + u.z * u.z))
     	);
    }
    
    
    public Quaternion powered(double _x)
    {
    	return ln().scaled(_x).exp();
    }
    
	public Quaternion ln()
    {
		double r = Math.sqrt(u.x*u.x+u.y*u.y+u.z*u.z);
		double t = r>0.00001 ? Math.atan2(r,a)/r: 0.f;
		
		return new Quaternion(
				0.5f*Math.log(a*a+u.x*u.x+u.y*u.y+u.z*u.z),
				u.x*t,
				u.y*t,
				u.z*t
			);
	}
	
	public Quaternion exp()
	{
		double r = Math.sqrt(u.x*u.x+u.y*u.y+u.z*u.z);
		double et = Math.exp(a);
		double s = r>=0.00001 ? et*Math.sin(r)/r: 0f;
		
		return new Quaternion(
				et*Math.cos(r),
				u.x*s,
				u.y*s,
				u.z*s
			);
	}


    public Quaternion slerp(Quaternion _r, double _t)
    {
        double cosAngle = dot(_r);
        
        //interpolate close quaternions
        if (cosAngle > 1 - EPSILON)
            return add(_r.subtract(this).scaled(_t)).normalized();
        
        if (cosAngle < 0)
            cosAngle = 0;
        if (cosAngle > 1)
            cosAngle = 1;
        
        double theta0 = Math.acos(cosAngle);
        double theta = theta0 * _t;
        Quaternion v2 = _r.subtract(scaled(cosAngle)).normalized();

        return scaled(Math.cos(theta)).add(v2.scaled(Math.sin(theta))).normalized();
    }
    
    public Quaternion nlerp(Quaternion _r, double _t)
    {
        return scaled(1-_t).add(_r.scaled(_t)).normalized();
    }

    public Quaternion normalized()
    {
        double l = length();
        return new Quaternion(a / l, u.scaled(1/l));
    }

    public double length()
    {
        return Math.sqrt(length2());
    }

    public double length2()
    {
        return a * a + u.lengthSq();
    }

    public double dot(Quaternion _q)
    {
        return a * _q.a + u.x * _q.u.x + u.y * _q.u.y + u.z * _q.u.z;
    }

    public static Quaternion calcRotationBetween(Vector3d _startPoint, Vector3d _endPoint)
    {
    	Vector3d rotationAxis = _startPoint.cross(_endPoint);
        double rotationAngle = Math.atan2(rotationAxis.length(), _startPoint.dot(_endPoint));
        if (rotationAngle == 0.0)
        	rotationAxis = new Vector3d(0,0,1);
        return Quaternion.createRotation(rotationAngle, rotationAxis);
    }

    public Quaternion inversed()
    {
    	Quaternion q = conjugated();
    	double length = q.a*q.a + q.u.x*q.u.x + q.u.y * q.u.y + q.u.z*q.u.z;
    	
    	return new Quaternion(q.a / length, q.u.scaled(1/length));
    }
    
    public Quaternion conjugated()
    {
        return new Quaternion(a, new Vector3d(-u.x,-u.y,-u.z));
    }

    public String toString()
    {
    	return String.format("[%.3f, %.3f, %.3f, %.3f]", a,u.x,u.y,u.z);
    }
}
