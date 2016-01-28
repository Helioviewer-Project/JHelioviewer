package org.helioviewer.jhv.base.math;

public class Vector4d
{
    public final double x;
    public final double y;
    public final double z;
    public final double w;

    public Vector4d(double _x, double _y, double _z, double _w)
    {
        x = _x;
        y = _y;
        z = _z;
        w = _w;
    }

    public Vector4d()
    {
    	x=y=z=w=0;
    }

    public Vector4d(Vector2d _xy, double _z, double _w)
	{
        x = _xy.x;
        y = _xy.y;
        z = _z;
        w = _w;
	}

    public Vector4d(Vector3d _xyz, double _w)
	{
        x = _xyz.x;
        y = _xyz.y;
        z = _xyz.z;
        w = _w;
	}

	public String toString()
    {
        return "[" + x + ", " + y + ", " + z + ", " + w + "]";
    }

    public Vector3d xyz()
    {
    	return new Vector3d(x,y,z);
    }

	public Vector4d normalized()
	{
		double scale = 1 / length();
		return new Vector4d(x * scale, y * scale, z * scale, w * scale);
	}
	
	public double length()
	{
		return Math.sqrt(x * x + y * y + z * z + w * w);
	}
}
