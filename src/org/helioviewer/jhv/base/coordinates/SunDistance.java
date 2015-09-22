package org.helioviewer.jhv.base.coordinates;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.physics.Constants;


class SunDistance {

	private final SunPosition sunPosition;

	private final double p;

	private final double b0;

	private final double semiDiameter;

	private final double sunDistance;

	public SunDistance(SunPosition sunPosition, double p, double b0, double semiDiameter, double sunDistance) {
		this.sunPosition = sunPosition;
		this.p = p;
		this.b0 = b0;
		this.semiDiameter = semiDiameter;
		this.sunDistance = sunDistance;
	}

	/**
	 * "Absolute" position of the sun for a given time.
	 * 
	 * @return the Sun's position
	 */
	public SunPosition getSunPosition() {
		return sunPosition;
	}

	/**
	 * Solar P (position angle of pole) (degrees)
	 * 
	 * @return p
	 */
	public double getP() {
		return p;
	}

	/**
	 * latitude of point at disk centre (degrees)
	 * 
	 * @return b0
	 */
	public double getB0() {
		return b0;
	}

	/**
	 * semi-diameter of the solar disk in arcminutes
	 * 
	 * @return
	 */
	public double getSemiDiameter() {
		return semiDiameter;
	}

	/**
	 * Sun distance.
	 * 
	 * @return Sun distance.
	 */
	public double getSunDistance() {
		return sunDistance;
	}


	
	public static SunDistance computePb0rSunDistance(LocalDateTime localDateTime){
		
	    // number of Julian days since 2415020.0
	    double de = CoordinateHelper.julianDaySinceJ19000101(localDateTime);

	    SunPosition sunPos = SunPosition.computeSunPos(localDateTime);
	    
		double longmed = Math.toDegrees(sunPos.getLongitude());
	    double appl = Math.toDegrees(sunPos.getApparentLongitude());
	    double oblt = Math.toDegrees(sunPos.getObliquity());

	    // form the aberrated longitude
	    double lambda = longmed - (20.50 / 3600.0);
	
	    // form longitude of ascending node of sun's equator on ecliptic
	    double node = 73.6666660 + (50.250 / 3600.0) * ((de / 365.250) + 50.0);
	    double arg = lambda - node;
	
	    // calculate P, the position angle of the pole
	    double p = Math.toDegrees(
	        Math.atan(-Math.tan(Math.toRadians(oblt)) * Math.cos(Math.toRadians(appl))) + 
	        Math.atan(-0.127220 * Math.cos(Math.toRadians(arg))));
	
	    // B0 the tilt of the axis...
	    double b = Math.toDegrees(Math.asin(0.12620 * Math.sin(Math.toRadians(arg))));

	    // ... and the semi-diameter
	    // Form the mean anomalies of Venus(MV),Earth(ME),Mars(MM),Jupiter(MJ)
	    // and the mean elongation of the Moon from the Sun(D).
	    double t = de / 36525.0;
	    double mv = 212.60 + (58517.80 * t % 360.0);
	    double me = 358.4760 + (35999.04980 * t % 360.0);
	    double mm = 319.50 + (19139.860 * t % 360.0);
	    double mj = 225.30 + (3034.690 * t % 360.0);
	    double d = 350.70 + (445267.110 * t % 360.0);

	    // Form the geocentric distance(r) and semi-diameter(sd)
	    // r is in fraction of 1AU
	    double r = 1.0001410 - (0.0167480 - 0.00004180 * t) * Math.cos(Math.toRadians(me)) 
	        - 0.000140 * Math.cos(Math.toRadians(2.0 * me)) 
	        + 0.0000160 * Math.cos(Math.toRadians(58.30 + 2.0 * mv - 2.0 * me)) 
	        + 0.0000050 * Math.cos(Math.toRadians(209.10 + mv - me)) 
	        + 0.0000050 * Math.cos(Math.toRadians(253.80 - 2.0 * mm + 2.0 * me)) 
	        + 0.0000160 * Math.cos(Math.toRadians(89.50 - mj + me)) 
	        + 0.0000090 * Math.cos(Math.toRadians(357.10 - 2.0 * mj + 2.0 * me)) 
	        + 0.0000310 * Math.cos(Math.toRadians(d));

	    double sd_const = Constants.SUN_RADIUS / Constants.AU;
	    double sd = Math.asin(sd_const / r) * 10800.0 / Math.PI;

	    SunDistance sunDistance = new SunDistance(sunPos, Math.toRadians(p), Math.toRadians(b), Math.toRadians(sd / Constants.ARCMIN_FACTOR), r * Constants.AU);
		
		return sunDistance;
	}

	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SunDistance [p=").append(p);
		builder.append(", b0=").append(b0);
		builder.append(", semiDiameter=").append(semiDiameter);
		builder.append(", sunDistance=").append(sunDistance).append("]");
		return builder.toString();
	}
}
