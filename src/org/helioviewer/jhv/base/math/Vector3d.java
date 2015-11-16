package org.helioviewer.jhv.base.math;

import java.util.Locale;

import javax.annotation.Nullable;

/**
 * A class for three dimensional vectors with double coordinates. Instances of
 * Vector3d are immutable.
 */
public class Vector3d
{
	public static final Vector3d ZERO = new Vector3d(0, 0, 0);

	public final double x;
	public final double y;
	public final double z;

	public Vector3d()
	{
		x = 0.0;
		y = 0.0;
		z = 0.0;
	}

	public Vector3d(final double newX, final double newY, final double newZ)
	{
		x = newX;
		y = newY;
		z = newZ;
	}

	public Vector3d(final Vector3d v)
	{
		x = v.x;
		y = v.y;
		z = v.z;
	}

	public Vector3d add(final Vector3d v)
	{
		return new Vector3d(x + v.x, y + v.y, z + v.z);
	}

	public static Vector3d add(final Vector3d v1, final Vector3d v2)
	{
		return new Vector3d(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
	}

	public Vector3d subtract(final Vector3d v)
	{
		return new Vector3d(x - v.x, y - v.y, z - v.z);
	}

	public static Vector3d subtract(final Vector3d v1, final Vector3d v2)
	{
		return new Vector3d(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
	}

	public Vector3d scaled(final double _d)
	{
		return new Vector3d(x * _d, y * _d, z * _d);
	}

	public Vector3d scale(final Vector3d _v)
	{
		return new Vector3d(x * _v.x, y * _v.y, z * _v.z);
	}

	public Vector3d invertedScale(final Vector3d _v)
	{
		return new Vector3d(x / _v.x, y / _v.y, z / _v.z);
	}

	public boolean isApproxEqual(Vector3d _other, double _epsilon)
	{
		return Math.abs(this.x - _other.x) <= _epsilon && Math.abs(this.y - _other.y) <= _epsilon
				&& Math.abs(this.z - _other.z) <= _epsilon;
	}

	public Vector3d negated()
	{
		return new Vector3d(-x, -y, -z);
	}

	public Vector3d negatedX()
	{
		return new Vector3d(-x, y, z);
	}

	public Vector3d negatedY()
	{
		return new Vector3d(x, -y, z);
	}

	public Vector3d negatedZ()
	{
		return new Vector3d(x, y, -z);
	}

	public double length()
	{
		return Math.sqrt(x * x + y * y + z * z);
	}

	public double lengthSq()
	{
		return x * x + y * y + z * z;
	}

	public Vector3d normalized()
	{
		double length = length();
		return new Vector3dNormalized(x / length, y / length, z / length);
	}

	public double dot(final Vector3d _v)
	{
		return _v.x * x + _v.y * y + _v.z * z;
	}

	public Vector3d cross(final Vector3d _v)
	{
		double x1 = y * _v.z - z * _v.y;
		double x2 = z * _v.x - x * _v.z;
		double x3 = x * _v.y - y * _v.x;
		
		return new Vector3d(x1, x2, x3);
	}

	public Vector3d cross(final Vector3dNormalized _v)
	{
		double x1 = y * _v.z - z * _v.y;
		double x2 = z * _v.x - x * _v.z;
		double x3 = x * _v.y - y * _v.x;
		
		return new Vector3d(x1, x2, x3);
	}
	
	public Vector3d absolute()
	{
		return new Vector3d(Math.abs(x), Math.abs(y), Math.abs(z));
	}

	public boolean equals(final @Nullable Object o)
	{
		if (!(o instanceof Vector3d))
			return false;
		
		Vector3d v = (Vector3d) o;
		return x == v.x && y == v.y && z == v.z;
	}

	public int hashCode()
	{
		long h = Double.doubleToRawLongBits(x) ^ Double.doubleToRawLongBits(y) << 20
				^ Double.doubleToRawLongBits(z) << 40 ^ Double.doubleToRawLongBits(y) >> 40
				^ Double.doubleToRawLongBits(z) >> 20;

		return (int) (h ^ (h >> 32));
	}

	public String toString()
	{
		return String.format(Locale.ENGLISH, "(%.2f,%.2f,%.2f)", x, y, z);
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
	 * @return
	 */
	public Vector2d inPlaneCoord(Vector3d planeCenter, Vector3d planeVectorA, Vector3d planeVectorB)
	{
		Vector3d inPlane = this.projectedToPlane(planeCenter);
		double x = planeVectorA.dot(inPlane);
		double y = planeVectorB.dot(inPlane);
		return new Vector2d(x, y);
	}

	/**
	 * Calculate the in-plane-vector of the given point to the plane with origin
	 * planeCenter and normal norm(planeCenter)
	 * 
	 * @param planeCenter
	 *            - the plane's normal vector
	 * 
	 * @return the projection of the targetPoint
	 */
	public Vector3d inPlaneShift(Vector3d planeCenter)
	{
		Vector3d normal = planeCenter.normalized();
		return subtract(normal.scaled(normal.dot(this)));
	}

	/**
	 * Calculate the projection of the given point to the plane with origin
	 * planeCenter and normal norm(planeCenter)
	 * 
	 * @param _planeCenter
	 * @return
	 */
	public Vector3d projectedToPlane(Vector3d _planeCenter)
	{
		Vector3d normal = _planeCenter.normalized();
		Vector3d inPlaneShift = subtract(normal.scaled(normal.dot(this)));
		return _planeCenter.add(inPlaneShift);
	}

	
	/**
	 * A normalized variant of a 3d vector, with shortcuts for some calculations.
	 */
	private static class Vector3dNormalized extends Vector3d
	{
		private Vector3dNormalized(final double newX, final double newY, final double newZ)
		{
			super(newX,newY,newZ);
		}

		@Override
		public Vector3dNormalized normalized()
		{
			return this;
		}
		
		@Override
		public Vector3dNormalized absolute()
		{
			return new Vector3dNormalized(Math.abs(x), Math.abs(y), Math.abs(z));
		}
		
		@Override
		public Vector3d cross(final Vector3dNormalized _v)
		{
			double x1 = y * _v.z - z * _v.y;
			double x2 = z * _v.x - x * _v.z;
			double x3 = x * _v.y - y * _v.x;
			
			return new Vector3dNormalized(x1, x2, x3);
		}
		
		@Override
		public double length()
		{
			return 1;
		}

		@Override
		public double lengthSq()
		{
			return 1;
		}
		
		@Override
		public Vector3d negatedX()
		{
			return new Vector3dNormalized(-x, y, z);
		}
		
		@Override
		public Vector3d negatedY()
		{
			return new Vector3dNormalized(x, -y, z);
		}

		@Override
		public Vector3d negatedZ()
		{
			return new Vector3dNormalized(x, y, -z);
		}
		
		@Override
		public Vector3d negated()
		{
			return new Vector3dNormalized(-x, -y, -z);
		}
	}
}
