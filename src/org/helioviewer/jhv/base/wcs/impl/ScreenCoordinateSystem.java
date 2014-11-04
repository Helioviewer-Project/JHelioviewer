package org.helioviewer.jhv.base.wcs.impl;

import org.helioviewer.jhv.base.wcs.Cartesian2DCoordinateSystem;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.Unit;
import org.helioviewer.jhv.base.wcs.conversion.ScreenToSolarImageConversion;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;

/**
 * The ScreenCoordinateSystem can be used to convert Screen Points to other
 * {@link CoordinateSystem}s.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class ScreenCoordinateSystem extends Cartesian2DCoordinateSystem {

    private Viewport viewport;
    private Region region;

    public ScreenCoordinateSystem(Viewport viewport, Region region) {
        super(Unit.Pixel);
        this.viewport = viewport;
        this.region = region;
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        if (coordinateSystem.getClass().isAssignableFrom(SolarImageCoordinateSystem.class)) {
            return new ScreenToSolarImageConversion(this, (SolarImageCoordinateSystem) coordinateSystem);
        }

        return super.getConversion(coordinateSystem);
    }

    public Viewport getViewport() {
        return viewport;
    }

    public Region getRegion() {
        return this.region;
    }
}
