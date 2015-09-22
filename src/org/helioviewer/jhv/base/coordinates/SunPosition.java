package org.helioviewer.jhv.base.coordinates;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

import java.time.LocalDateTime;

class SunPosition {
	
	/**
	 * Date for this position of the sun.
	 */
	private final LocalDateTime localDateTime;

	/**
	 * Longitude of sun for mean equinox of date
	 */
	private final double longitude;

	/**
	 * Apparent RA for true equinox of date
	 */
	private final double ra;

	/**
	 * Apparent declination for true equinox of date
	 */
	private final double dec;

	/**
	 * Apparent longitude
	 */
	private final double apparentLongitude;

	/**
	 * True obliquity
	 */
	private final double obliquity;

	private SunPosition(LocalDateTime localDateTime, double longitude, double ra, double dec, double apparentLongitude, double obliquity) {
		this.localDateTime = localDateTime;
		this.longitude = longitude;
		this.ra = ra;
		this.dec = dec;
		this.apparentLongitude = apparentLongitude;
		this.obliquity = obliquity;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getRa() {
		return ra;
	}

	public double getDec() {
		return dec;
	}

	public double getApparentLongitude() {
		return apparentLongitude;
	}

	public double getObliquity() {
		return obliquity;
	}

	public LocalDateTime getDateTime() {
		return localDateTime;
	}

	
	public static SunPosition computeSunPos(LocalDateTime localDateTime){
		double dd = CoordinateHelper.julianDaySinceJ19000101(localDateTime);

		// form time in Julian centuries from 1900.0
		double t = dd / 36525.0;

		// form sun's mean longitude
		double l = (279.6966780 + (36000.7689250 * t) % 360.00) * 3600.0;

		// allow for ellipticity of the orbit (equation of centre) using the
		// Earth's
		// mean anomaly ME
		double me = 358.4758440 + (35999.049750 * t) % 360.0;
		double ellcor = (6910.10 - 17.20 * t) * sin(toRadians(me)) + 72.30 * sin(toRadians(2.0 * me));
		l = l + ellcor;

		// allow for the Venus perturbations using the mean anomaly of Venus MV
		double mv = 212.603219 + (58517.8038750 * t) % 360.0;
		double vencorr = 4.80 * cos(toRadians(299.10170 + mv - me)) + 5.50
						* cos(toRadians(148.31330 + 2.0 * mv - 2.0 * me)) + 2.50
						* cos(toRadians(315.94330 + 2.0 * mv - 3.0 * me)) + 1.60
						* cos(toRadians(345.25330 + 3.0 * mv - 4.0 * me)) + 1.00
						* cos(toRadians(318.15000 + 3.0 * mv - 5.0 * me));
		l = l + vencorr;

		// Allow for the Mars perturbations using the mean anomaly of Mars MM
		double mm = 319.5294250 + (19139.858500 * t) % 360.0;
		double marscorr = 2.0 * cos(toRadians(343.88830 - 2.0 * mm + 2.0 * me)) + 1.80
						* cos(toRadians(200.40170 - 2.0 * mm + me));
		l = l + marscorr;

		// Allow for the Jupiter perturbations using the mean anomaly of Jupiter
		// MJ
		double mj = 225.3283280 + (3034.69202390 * t % 360.00);
		double jupcorr = 7.20 * cos(toRadians(179.53170 - mj + me)) + 2.60 * cos(toRadians(263.21670 - mj)) + 2.70
						* cos(toRadians(87.14500 - 2.0 * mj + 2.0 * me)) + 1.60
						* cos(toRadians(109.49330 - 2.0 * mj + me));
		l = l + jupcorr;

		// Allow for the Moons perturbations using the mean elongation of the
		// Moon
		// from the Sun D
		double d = 350.73768140 + (445267.114220 * t % 360.0);
		double mooncorr = 6.50 * sin(toRadians(d));
		l = l + mooncorr;

		// Note the original code is
		// longterm = + 6.4d0 * sin(( 231.19d0 + 20.20d0 * t )*!dtor)
		double longterm = 6.40 * sin(toRadians(231.190 + 20.20 * t));
		l = l + longterm;
		l = (l + 2592000.0) % 1296000.0;
		double longmed = l / 3600.0;

		// Allow for Aberration
		l = l - 20.5;

		// Allow for Nutation using the longitude of the Moons mean node OMEGA
		double omega = 259.1832750 - (1934.1420080 * t) % 360.0;
		l = l - 17.20 * sin(toRadians(omega));

		// Form the True Obliquity
		double oblt = 23.4522940 - 0.01301250 * t + (9.20 * cos(toRadians(omega))) / 3600.0;

		// Form Right Ascension and Declination
		l = l / 3600.0;

		double ra = atan2(sin(toRadians(l)) * cos(toRadians(oblt)), cos(toRadians(l)));

		if (ra < 0.0) {
			ra = ra + 360.0;
		}

		double dec = asin(sin(toRadians(l)) * sin(toRadians(oblt)));

		// convert the internal variables to those required by the result.
		SunPosition sunPosition = new SunPosition(localDateTime, Math.toRadians(longmed), ra, dec, Math.toRadians(l), Math.toRadians(oblt));

		return sunPosition;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SunPosition [dateTime=").append(localDateTime).append(", longitude=").append(longitude)
						.append("rad, ra=").append(ra).append("rad, dec=").append(dec)
						.append("rad, apparentLongitude=").append(apparentLongitude).append("rad, obliquity=")
						.append(obliquity).append("rad]");
		return builder.toString();
	}


	
}
