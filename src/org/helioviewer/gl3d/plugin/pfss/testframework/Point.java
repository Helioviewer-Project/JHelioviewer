package org.helioviewer.gl3d.plugin.pfss.testframework;

import org.helioviewer.base.physics.Constants;

public class Point {
	private float x;
	private float y;
	private float z;
	private int index;
	
	private short rawR;
	private short rawPhi;
	private short rawTheta;

	public Point(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public Point(int index, short ptr, short ptph, short ptth, double l0,
			double b0) {
		this.index = index;
		double r0 = ptr / 8192.0 * Constants.SunRadius;
		double phi0 = ptph / 32768.0 * 2 * Math.PI;
		double theta0 = ptth / 32768.0 * 2 * Math.PI;

		this.rawR = ptr;
		this.rawPhi = ptph;
		this.rawTheta = ptth;
		
		phi0 -= l0 / 180.0 * Math.PI;
		theta0 += b0 / 180.0 * Math.PI;
		z = (float) (r0 * Math.sin(theta0) * Math.cos(phi0));
		x = (float) (r0 * Math.sin(theta0) * Math.sin(phi0));
		y = (float) (r0 * Math.cos(theta0));
	}
	
	public Point(int index, float ptr, float ptph, float ptth, double l0, double b0) {
		this.index = index;
		double r0 = ptr * Constants.SunRadius;
		double phi0 = ptph * 2 * Math.PI;
		double theta0 = ptth * 2 * Math.PI;

		phi0 -= l0 / 180.0 * Math.PI;
		theta0 += b0 / 180.0 * Math.PI;
		z = (float) (r0 * Math.sin(theta0) * Math.cos(phi0));
		x = (float) (r0 * Math.sin(theta0) * Math.sin(phi0));
		y = (float) (r0 * Math.cos(theta0));
	}
	
	public short getRawR() {
		return rawR;
	}

	public short getRawPhi() {
		return rawPhi;
	}

	public short getRawTheta() {
		return rawTheta;
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getZ() {
		return z;
	}

	public int getIndex() {
		return index;
	}
	
	public double getDistanceTo(Point p) {
		Point vec = getVector(this,p);
		return vec.magnitude();
	}
	
	public double magnitude() {
		return Math.sqrt(x*x+y*y+z*z);
	}
	
	
	public static Point getVector(Point start, Point end) {
		return new Point(end.x- start.x,end.y-start.y,end.z-start.z);
	}
	
	public static double dot(Point p0, Point p1) {
		return p0.x*p1.x+p0.y*p1.y+p0.z*p1.z;
	}
	
	public static Point cross(Point p0, Point p1) {
		float x = p0.y*p1.z-p0.z*p1.y;
		float y = p0.z*p1.x- p0.x*p1.z;
		float z = p0.x*p1.y-p0.y*p1.x;
		return new Point(x,y,z);
	}
}
