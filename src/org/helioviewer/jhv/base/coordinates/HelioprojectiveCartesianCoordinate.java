package org.helioviewer.jhv.base.coordinates;

import org.helioviewer.jhv.base.physics.Constants;

//see http://adsabs.harvard.edu/abs/2006A%26A...449..791T
//see also http://fits.gsfc.nasa.gov/wcs/coordinates.pdf
public class HelioprojectiveCartesianCoordinate
{
	public final double thetaX;
	public final double thetaY;
	public final double sunDistance;
	
	public HelioprojectiveCartesianCoordinate(double thetaX, double thetaY)
	{
		this(thetaX, thetaY, 1);
	}

	public HelioprojectiveCartesianCoordinate(double _thetaX, double _thetaY, double _sunDistance)
	{
		thetaX = _thetaX;
		thetaY = _thetaY;
		sunDistance = _sunDistance;
	}
	
	public HeliocentricCartesianCoordinate toHeliocentricCartesianCoordinate()
	{
		double cosx = Math.cos(thetaX);
		double sinx = Math.sin(thetaX);
		double cosy = Math.cos(thetaY);
		double siny = Math.sin(thetaY);

		double q = sunDistance * cosy * cosx;
		double distance = q * q - sunDistance * sunDistance + Constants.SUN_RADIUS_SQ;
		distance = q - Math.sqrt(distance);

		double rx = distance * cosy * sinx;
		double ry = distance * siny;
		double rz = sunDistance - distance * cosy * cosx;
		return new HeliocentricCartesianCoordinate(rx, ry, rz);
	}
	
	public HeliographicCoordinate toHeliographicCoordinate()
	{
		return toHeliocentricCartesianCoordinate().toHeliographicCoordinate();
	}

	public double getThetaXAsArcSec()
	{
		return Math.toDegrees(thetaX) * Constants.ARCSEC_FACTOR;
	}

	public double getThetaYAsArcSec()
	{
		return Math.toDegrees(thetaY) * Constants.ARCSEC_FACTOR;
	}
}
