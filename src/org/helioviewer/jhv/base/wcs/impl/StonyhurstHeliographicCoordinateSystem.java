package org.helioviewer.jhv.base.wcs.impl;

import org.helioviewer.jhv.base.wcs.AbstractCoordinateSystem;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateDimension;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.GenericCoordinateDimension;
import org.helioviewer.jhv.base.wcs.Unit;

/**
 * The {@link StonyhurstHeliographicCoordinateSystem} is used for calculating
 * the Solar Rotation.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class StonyhurstHeliographicCoordinateSystem extends AbstractCoordinateSystem implements CoordinateSystem {
    private static final int PHI = 0;
    private static final int THETA = 1;
    private static final int RADIUS = 2;

    private CoordinateDimension phiDimension;
    private CoordinateDimension thetaDimension;
    private CoordinateDimension radiusDimension;

    public StonyhurstHeliographicCoordinateSystem() {
        this.phiDimension = new GenericCoordinateDimension(Unit.Radian, "Phi");
        this.thetaDimension = new GenericCoordinateDimension(Unit.Radian, "Theta");
        this.radiusDimension = new GenericCoordinateDimension(Unit.Kilometer, "Radius");
    }

    public int getDimensions() {
        return 3;
    }

    public CoordinateDimension getDimension(int dimension) {
        switch (dimension) {
        case PHI:
            return this.phiDimension;
        case THETA:
            return this.thetaDimension;
        case RADIUS:
            return this.radiusDimension;
        default:
            throw new IllegalArgumentException("Illegal dimension Number " + dimension + " for Coordinate System " + this);
        }
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        return super.getConversion(coordinateSystem);
    }
}
