package org.helioviewer.jhv.base.coordinates;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.physics.Constants;


public class HeliographicCoordinate {
	
	public final double longitude;
	public final double latitude;
	public final double radius;
	public HeliographicCoordinate(double hgLongitude, double hgLatitude, double radius) {
		this.longitude = hgLongitude;
		this.latitude = hgLatitude;
		this.radius = radius;
	}
	
	public HeliographicCoordinate(double hgLongitude, double hgLatitude) {
		this(hgLongitude, hgLatitude, Constants.SUN_RADIUS);
	}
	
	public HeliocentricCartesianCoordinate toHeliocentricCartesianCoordinate(double b0, double l0){
		
		double cosb = Math.cos(b0);
		double sinb = Math.sin(b0);

		double longitude = this.longitude - l0;
		
		double cosx = Math.cos(longitude);
		double sinx = Math.sin(longitude);
		double cosy = Math.cos(latitude);
		double siny = Math.sin(latitude);

		double x = radius * cosy * sinx;
		double y = radius * (siny * cosb - cosy * cosx * sinb);
		double z = radius * (siny * sinb + cosy * cosx * cosb);

		return new HeliocentricCartesianCoordinate(x, y, z);
	}
	
	public HeliocentricCartesianCoordinate toHeliocentricCartesianCoordinate(){
		return toHeliocentricCartesianCoordinate(0, 0);
	}
	
	
	public HelioprojectiveCartesianCoordinate toHelioprojectiveCartesianCoordinate(LocalDateTime localDateTime){
		return toHeliocentricCartesianCoordinate().toHelioprojectiveCartesianCoordinate(localDateTime);
	}

	public double getHgLongitudeAsDeg(){
		return Math.toDegrees(longitude);		
	}
	
	public double getHgLatitudeAsDeg() {
		return Math.toDegrees(latitude);
	}
	
}
