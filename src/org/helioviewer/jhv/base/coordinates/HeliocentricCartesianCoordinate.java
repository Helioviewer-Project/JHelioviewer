package org.helioviewer.jhv.base.coordinates;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.Vector3d;

//see http://adsabs.harvard.edu/abs/2006A%26A...449..791T
//see http://fits.gsfc.nasa.gov/wcs/coordinates.pdf
public class HeliocentricCartesianCoordinate
{
	public final double x;
	public final double y;
	public final double z;

	public HeliocentricCartesianCoordinate(double _x, double _y, double _z)
	{
		x = _x;
		y = _y;
		z = _z;
	}

	public HeliographicCoordinate toHeliographicCoordinate()
	{
		double b0 = 0;
		double l0 = 0;

		double cosb = Math.cos(b0);
		double sinb = Math.sin(b0);

		double hecRadius = Math.sqrt(x * x + y * y + z * z);
		double hgLongitude = Math.atan2(x, z * cosb - y * sinb) + l0;
		double hgLatitude = Math.asin((y * cosb + z * sinb) / hecRadius);

		return new HeliographicCoordinate(hgLongitude, hgLatitude, hecRadius);
	}

	public HelioprojectiveCartesianCoordinate toHelioprojectiveCartesianCoordinate(LocalDateTime localDateTime)
	{
		return toHelioprojectiveCartesianCoordinate(SunDistance.computePb0rSunDistance(localDateTime).getSunDistance());
	}
	
	public HelioprojectiveCartesianCoordinate toHelioprojectiveCartesianCoordinate(double sunDistance)
	{
		double zeta = sunDistance - z;
		double distance = Math.sqrt(x * x + y * y + zeta * zeta);
		double hpcx = Math.atan2(x, zeta);
		double hpcy = Math.asin(y / distance);
	
		return new HelioprojectiveCartesianCoordinate(hpcx, hpcy, sunDistance);
	}

	public Vector3d toVector3d()
	{
		return new Vector3d(x,y,z);
	}
}
