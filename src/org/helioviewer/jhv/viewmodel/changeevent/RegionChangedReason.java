package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.viewmodel.region.Region;
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
    private Region region;

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
    public RegionChangedReason(View aView, Region aNewRegion) {

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
    public Region getNewRegion() {
        return region;
    }
}