package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.viewmodel.view.View;

/**
 * Class represents a change reason when the image data has changed.
 * 
 * @author Stephan Pagel
 * */
public class SubImageDataChangedReason implements ChangedReason {
    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    // memorizes the associated view
    private View view;

    // ///////////////////////////////////////////////////////////////
    // Definitions
    // ///////////////////////////////////////////////////////////////

    /**
     * Default constructor
     * 
     * @param aView
     *            View which caused the change reason
     */
    public SubImageDataChangedReason(View aView) {

        // memorize view
        view = aView;
    }

    /**
     * {@inheritDoc}
     */
    public View getView() {
        return view;
    }
}
