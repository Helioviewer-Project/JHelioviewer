package org.helioviewer.jhv.base.wcs.impl;

import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.wcs.Cartesian3DCoordinateSystem;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.Unit;
import org.helioviewer.jhv.base.wcs.conversion.SolarSphereToSolarImageConversion;
import org.helioviewer.jhv.base.wcs.conversion.SolarSphereToStonyhurstHeliographicConversion;

/**
 * The 3-dimensional coordinate system that is used for the 3D representations
 * of the solar images. It is also the coordinate system the cameras operate in.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class SolarSphereCoordinateSystem extends Cartesian3DCoordinateSystem {
    public SolarSphereCoordinateSystem() {
        super(Unit.Kilometer);
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem instanceof SolarImageCoordinateSystem) {
            return new SolarSphereToSolarImageConversion(this, (SolarImageCoordinateSystem) coordinateSystem);
        } else if (coordinateSystem instanceof StonyhurstHeliographicCoordinateSystem) {
            return new SolarSphereToStonyhurstHeliographicConversion(this, (StonyhurstHeliographicCoordinateSystem) coordinateSystem);
        }

        return super.getConversion(coordinateSystem);
    }

    public double getSolarRadius() {
        return Constants.SUN_RADIUS;
    }
}
