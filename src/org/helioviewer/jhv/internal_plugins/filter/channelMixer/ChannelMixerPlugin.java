package org.helioviewer.jhv.internal_plugins.filter.channelMixer;

import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.SimpleFilterContainer;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.view.FilterView;

/**
 * Plugin for modifying the color mask of an image
 * 
 * <p>
 * The plugin manages a filter for modifying the color mask of an image and
 * three check boxes to change it.
 * 
 * @author Markus Langenberg
 * 
 */
public class ChannelMixerPlugin extends SimpleFilterContainer {

    /**
     * {@inheritDoc}
     */

    protected Filter getFilter() {
        return new ChannelMixerFilter();
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
        return new ChannelMixerPanel();
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
        return "Channel Mixer";
    }

    /**
     * {@inheritDoc}
     */

    protected FilterTabDescriptor getFilterTab() {
        return new FilterTabDescriptor(FilterTabDescriptor.Type.COMPACT_FILTER, "");
    }
}
