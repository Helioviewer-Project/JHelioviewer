package org.helioviewer.jhv.opengl.scenegraph;

import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.CoordinateSystemChangeListener;

public abstract class GL3DCoordinateSystemGroup extends GL3DGroup implements CoordinateSystemChangeListener {

    public GL3DCoordinateSystemGroup(String name) {
        super(name);
    }

    public void shapeInit(GL3DState state) {
        getCoordinateSystem().addListener(this);
        super.shapeInit(state);
    }

    public void coordinateSystemChanged(CoordinateSystem coordinateSystem) {
        this.markAsChanged();
        // Log.debug("GL3DCoordinateSystemGroup: CoordinateSystemChanged, marking as changed");
    }

    public abstract CoordinateSystem getCoordinateSystem();
}
