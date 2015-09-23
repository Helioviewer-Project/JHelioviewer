package org.helioviewer.jhv.base.coordinates;

import java.time.LocalDateTime;
import java.time.temporal.JulianFields;

public class CoordinateHelper
{
	/**
	 * Julian date for January, 1st, 1900.
	 */
	private static final double JAN_1_1900 = 2415020.0;
	
	public static double julianDaySinceJ19000101(LocalDateTime localDateTime) {
		double dd = localDateTime.getLong(JulianFields.JULIAN_DAY) - JAN_1_1900;
		return dd;
	}
	
    public static double calculateRotationInDegrees(double latitude, double timeDifferenceInSeconds) {
        return calculateRotationInRadians(latitude, timeDifferenceInSeconds) * 180.0 / Math.PI;
    }

    public static HeliographicCoordinate calculateNextPosition(HeliographicCoordinate currentPosition, double timeDifferenceInSeconds) {
        double rotation = calculateRotationInDegrees(currentPosition.latitude, timeDifferenceInSeconds);
        double longitude = currentPosition.longitude + rotation;
        longitude %= 360.0;
        return new HeliographicCoordinate(longitude, currentPosition.latitude, currentPosition.radius);
    }
    
    public static double calculateRotationInRadians(double latitude, double timeDifferenceInSeconds) {
        double sin2l = Math.sin(latitude);
        sin2l = sin2l * sin2l;
        double sin4l = sin2l * sin2l;
        return 1.0e-6 * timeDifferenceInSeconds * (2.894 - 0.428 * sin2l - 0.37 * sin4l);
    }
}
