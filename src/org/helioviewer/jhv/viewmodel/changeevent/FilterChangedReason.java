package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Class represents a change reason when a filter has changed.
 * 
 * @author Stephan Pagel
 * @author Markus Langenberg
 * */
public class FilterChangedReason implements ChangedReason {
    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    // memorizes the associated view
    private View view;

    // memorizes the new region
    private Filter filter;

    // ///////////////////////////////////////////////////////////////
    // Methods
    // ///////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * 
     * @param aView
     *            View which caused the change reason.
     * @param aFilter
     *            New defined filter.
     * */
    public FilterChangedReason(View aView, Filter aFilter) {

        // memorize view
        view = aView;
        filter = aFilter;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }

    /**
     * Returns the new filter which has been changed.
     * 
     * @return the new filter.
     * */
    public Filter getNewFilter() {
        return filter;
    }

}
