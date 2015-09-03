package org.helioviewer.jhv.base.physics;

import java.util.Locale;

import org.helioviewer.jhv.base.math.MathUtils;

public class StonyhurstHeliographicCoordinates implements SolarCoordinates
{
    // origin is at the center of the sun
    // the angles are measured on the sun relative to the intersection of the
    // solar equator
    // and the central meridian as seen from earth

    // latitude, increasing towards solar north
    public final double theta;

    // longitude, increasing towards the solar west limb
    public final double phi;

    // radius
    public final double r;

    public StonyhurstHeliographicCoordinates(double newTheta, double newPhi, double newR)
    {
        theta = newTheta;
        phi = newPhi;
        r = newR;
    }

    public HeliocentricEarthEquatorialCoordinates convertToHeliocentricEarthEquatorialCoordinates()
    {
        double heeqX = r * Math.cos(theta / MathUtils.RAD_TO_DEG) * Math.cos(phi / MathUtils.RAD_TO_DEG);
        double heeqY = r * Math.cos(theta / MathUtils.RAD_TO_DEG) * Math.sin(phi / MathUtils.RAD_TO_DEG);
        double heeqZ = r * Math.sin(theta / MathUtils.RAD_TO_DEG);
        return new HeliocentricEarthEquatorialCoordinates(heeqX, heeqY, heeqZ);
    }

    public String toString()
    {
        return String.format(Locale.ENGLISH, "theta = %f, phi = %f, r = %f", theta, phi, r);
    }
}
