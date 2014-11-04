package org.helioviewer.jhv.base.wcs.conversion;

import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.base.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.jhv.base.wcs.impl.SphericalCoordinateSystem;

public class SphericalToSolarSphereConversion implements CoordinateConversion {

    private SphericalCoordinateSystem sphericalCoordinateSystem;

    private SolarSphereCoordinateSystem solarSphereCoordinateSystem;

    public SphericalToSolarSphereConversion(SphericalCoordinateSystem sphericalCoordinateSystem, SolarSphereCoordinateSystem solarSphereCoordinateSystem) {
        this.sphericalCoordinateSystem = sphericalCoordinateSystem;
        this.solarSphereCoordinateSystem = solarSphereCoordinateSystem;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return sphericalCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.solarSphereCoordinateSystem;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double phi = vector.getValue(SphericalCoordinateSystem.PHI);
        double theta = vector.getValue(SphericalCoordinateSystem.THETA);

        double sinTheta = Math.sin(theta);

        double x = this.solarSphereCoordinateSystem.getSolarRadius() * sinTheta * Math.sin(phi);
        double y = this.solarSphereCoordinateSystem.getSolarRadius() * Math.cos(theta);
        double z = this.solarSphereCoordinateSystem.getSolarRadius() * Math.cos(phi) * sinTheta;

        return this.solarSphereCoordinateSystem.createCoordinateVector(x, y, z);
    }
}
