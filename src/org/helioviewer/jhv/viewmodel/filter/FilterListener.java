package org.helioviewer.jhv.viewmodel.filter;

/**
 * Listener to be called when a filter has changed.
 * 
 * @author Ludwig Schmidt
 * 
 */
public interface FilterListener {

    /**
     * Callback function that will be called when a filter has changed.
     * 
     * @param f
     *            New filter
     */
    public void filterChanged(Filter f);

}
