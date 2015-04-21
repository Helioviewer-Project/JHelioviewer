package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Class represents a change reason when the region has changed.
 * 
 * @author Stephan Pagel
 * */
public final class RegionChangedReason implements ChangedReason {

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    // memorizes the associated view
    private View view;

    // memorizes the new region
    private PhysicalRegion region;

    // ///////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////

    /**
     * Default constructor
     * 
     * @param aView
     *            View which caused the change reason.
     * @param aNewRegion
     *            New defined region.
     * */
    public RegionChangedReason(View aView, PhysicalRegion aNewRegion) {

        // memorize view
        view = aView;
        region = aNewRegion;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the new region which was defined when this change reason
     * occurred.
     * 
     * @return new region.
     * */
    public PhysicalRegion getNewRegion() {
        return region;
    }
}