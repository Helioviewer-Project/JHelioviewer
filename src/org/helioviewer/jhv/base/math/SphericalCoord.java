package org.helioviewer.jhv.base.math;

import java.util.Locale;

import javax.annotation.Nullable;

public class SphericalCoord
{
	private static final char DEGREE = '\u00B0';
	public double theta = 0.0;
	public double phi = 0.0;
	public double r = 0.0;

	public SphericalCoord(double theta, double phi, double r)
	{
		this.theta = theta;
		this.phi = phi;
		this.r = r;
	}

	public SphericalCoord()
	{
	}

	public SphericalCoord(SphericalCoord stony)
	{
		this(stony.theta, stony.phi, stony.r);
	}

	public String toString()
	{
		return String.format(Locale.ENGLISH, "Theta=%.2f" + DEGREE + ", Phi=%.2f" + DEGREE + ", r=%.2f", theta, phi, r);
	}

	public boolean equals(@Nullable Object otherObject)
	{
		if (!(otherObject instanceof SphericalCoord))
			return false;
			
		SphericalCoord otherCoord = (SphericalCoord) otherObject;
		return (otherCoord.theta == this.theta && otherCoord.phi == this.phi && otherCoord.r == this.r);
	}

	@Override
	public int hashCode()
	{
		return Double.hashCode(theta) ^ (Double.hashCode(phi) << 4) ^ (Double.hashCode(r) << 8);
	}
}
