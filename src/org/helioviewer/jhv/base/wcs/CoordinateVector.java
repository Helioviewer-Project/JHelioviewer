package org.helioviewer.jhv.base.wcs;

import org.helioviewer.jhv.base.math.Vector3d;

/**
 * A {@link CoordinateVector} describes a point within its
 * {@link CoordinateSystem} by providing values in each dimension of its
 * {@link CoordinateSystem}. A {@link CoordinateVector} can be transformed to
 * another {@link CoordinateSystem} by using a {@link CoordinateConversion} of
 * its source coordinate system.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class CoordinateVector {
    private final double[] coordinates;

    private CoordinateSystem coordinateSystem;

    protected CoordinateVector(CoordinateSystem coordinateSystem, double... value) {
        this.coordinateSystem = coordinateSystem;
        this.coordinates = value;
    }
    
    public Vector3d toVector3d()
    {
        if (getCoordinateSystem().getDimensions() != 3)
            throw new IllegalCoordinateVectorException("Cannot Create Vector3d from CoordinateVector with " + getCoordinateSystem().getDimensions() + " dimensions");

        return new Vector3d(
                getValue(0),
                getValue(1),
                getValue(2));
    }

    public CoordinateSystem getCoordinateSystem() {
        return this.coordinateSystem;
    }

    public double getValue(int dimension) {
        return this.coordinates[dimension];
    }

    public String toString() {
        String s = "";
        for (int d = 0; d < getCoordinateSystem().getDimensions(); d++) {
            s += this.coordinates[d] + " " + getCoordinateSystem().getDimension(d).getUnit().getAbbreviation();
            if ((d + 1) < getCoordinateSystem().getDimensions())
                s += ", ";
        }
        return "[" + s + "]";
    }
}
