package org.helioviewer.jhv.base.coordinates;

import java.time.LocalDateTime;
import java.time.temporal.JulianFields;

public class CoordinateHelper {
	/**
	 * Julian date for January, 1st, 1900.
	 */
	private static final double JAN_1_1900 = 2415020.0;
	
	public enum Observer {
		EARTH,
		SOHO,
		STEREO_A,
		STEREO_B
	}
	
	public static double julianDaySinceJ19000101(LocalDateTime localDateTime) {
		double dd = localDateTime.getLong(JulianFields.JULIAN_DAY) - JAN_1_1900;
		return dd;
	}
	
}
