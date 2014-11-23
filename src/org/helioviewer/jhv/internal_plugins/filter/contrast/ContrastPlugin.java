package org.helioviewer.jhv.internal_plugins.filter.contrast;

import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.SimpleFilterContainer;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.view.FilterView;

/**
 * Plugin for enhancing the contrast of the image.
 * 
 * <p>
 * The plugin manages a filter for enhancing the contrast and a slider to change
 * the parameter.
 * 
 * @author Markus Langenberg
 * 
 */
public class ContrastPlugin extends SimpleFilterContainer {

    /**
     * {@inheritDoc}
     */

    protected Filter getFilter() {
        return new ContrastFilter();
    }

    /**
     * {@inheritDoc}
     */

    protected boolean useFilter(FilterView view) {
        return true;
    }

    /**
     * {@inheritDoc}
     */

    protected FilterPanel getPanel() {
        return new ContrastPanel();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "Contrast";
    }

    /**
     * {@inheritDoc}
     */

    protected FilterTabDescriptor getFilterTab() {
        return new FilterTabDescriptor(FilterTabDescriptor.Type.COMPACT_FILTER, "");
    }
}
