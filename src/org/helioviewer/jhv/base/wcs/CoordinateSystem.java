package org.helioviewer.jhv.base.wcs;

/**
 * A {@link CoordinateSystem} represents a set of {@link CoordinateDimension}s
 * that can be used to describe the location of a {@link CoordinateVector}
 * within its space. {@link CoordinateConversion} need to be provided to convert
 * {@link CoordinateVector} from one {@link CoordinateSystem} to another.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public interface CoordinateSystem {
    public int getDimensions();

    public CoordinateDimension getDimension(int dimension);

    public CoordinateVector createCoordinateVector(double... value);

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem);
}
