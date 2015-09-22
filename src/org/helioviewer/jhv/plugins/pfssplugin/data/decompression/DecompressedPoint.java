package org.helioviewer.jhv.plugins.pfssplugin.data.decompression;


/**
 * Immutable Class representing a Point in 3D Space
 */
class DecompressedPoint {
	private final float x;
	private final float y;
	private final float z;
	
	public DecompressedPoint(float x, float y, float z) {
		super();
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	/**
	 * @return the x
	 */
	public float getX() {
		return x;
	}
	
	/**
	 * @return the y
	 */
	public float getY() {
		return y;
	}
	
	/**
	 * @return the z
	 */
	public float getZ() {
		return z;
	}
	
	/**
	 * 
	 * @return Magnitude 
	 */
	public double magnitude() {
		double xi = x;
		double yi = y;
		double zi = z;
		return Math.sqrt(xi*xi+yi*yi+zi*zi);
	}
	
	/**
	 * Returns the angle between between the lines before-->this and this-->next
	 * @param next 
	 * @param before
	 * @return
	 */
	public double AngleTo(DecompressedPoint next,DecompressedPoint before)
    {
        return calculateAngleBetween2Vecotrs(next.x - x,
                                                next.y - y, next.z - z, x - before.x, y - before.y, z
                                                                             - before.z);
    }
	
	private double calculateAngleBetween2Vecotrs(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return (x1 * x2 + y1 * y2 + z1 * z2)
                                     / (Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1) * Math.sqrt(x2 * x2
                                                                 + y2 * y2 + z2 * z2));
    }

}
