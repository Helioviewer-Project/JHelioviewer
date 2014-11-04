package org.helioviewer.jhv.internal_plugins.filter.gammacorrection;

import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.SimpleFilterContainer;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.view.FilterView;

/**
 * Plugin for applying a gamma correction to the image.
 * 
 * <p>
 * The plugin manages a filter for applying the gamma correction and a slider to
 * change the gamma value.
 * 
 * @author Markus Langenberg
 * 
 */
public class GammaCorrectionPlugin extends SimpleFilterContainer {

    /**
     * {@inheritDoc}
     */

    protected Filter getFilter() {
        return new GammaCorrectionFilter();
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
        return new GammaCorrectionPanel();
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "Gamma Correction";
    }

    /**
     * {@inheritDoc}
     */

    protected FilterTabDescriptor getFilterTab() {
        return new FilterTabDescriptor(FilterTabDescriptor.Type.COMPACT_FILTER, "");
    }
}
