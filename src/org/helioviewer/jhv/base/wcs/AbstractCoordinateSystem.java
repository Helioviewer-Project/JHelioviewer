package org.helioviewer.jhv.base.wcs;


/**
 * Basic implementation of a {@link CoordinateSystem}.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class AbstractCoordinateSystem implements CoordinateSystem {

    public AbstractCoordinateSystem() {
    }

    public CoordinateVector createCoordinateVector(double... value) {
        if (value.length != getDimensions()) {
            throw new IllegalArgumentException("Need " + getDimensions() + " values to create a Vector in the CoordinateSystem " + this);
        }
        return new CoordinateVector(this, value);
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem.getClass().equals(this.getClass())) {
            return new IdentityMatrixConversion(this, coordinateSystem);
        }
        throw new IllegalArgumentException("No Conversion available from " + this + " to " + coordinateSystem);
    }
}
