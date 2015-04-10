package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Class represents a change reason when the view chain has changed.
 * 
 * This change reason could be used when the view chain itself has changed, e.g.
 * a view was added or removed.
 * 
 * @author Stephan Pagel
 * */
public class ViewChainChangedReason implements ChangedReason {

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    // memorizes the associated view
    private View view;

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    /**
     * Default constructor.
     * 
     * @param aView
     *            View which caused the change reason.
     * */
    public ViewChainChangedReason(View aView) {

        // memorize parameter values
        view = aView;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }
}
