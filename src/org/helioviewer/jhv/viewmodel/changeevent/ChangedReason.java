package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.viewmodel.view.View;

/**
 * A special reason for a {@link ChangeEvent} on which views in the view chain
 * can react.
 * 
 * @author Stephan Pagel
 * @see ChangeEvent
 * */
public interface ChangedReason {

    /**
     * Returns the view where the reason has occurred.
     * 
     * @return view where the reason was created or null if the value was not
     *         set.
     * */
    public View getView();

}
