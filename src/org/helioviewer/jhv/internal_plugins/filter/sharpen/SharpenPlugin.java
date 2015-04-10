package org.helioviewer.jhv.internal_plugins.filter.sharpen;

import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.SimpleFilterContainer;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.view.FilterView;

/**
 * Plugin for sharpen the image.
 * 
 * <p>
 * This plugin provides the capability to sharpen the image. It manages a filter
 * for applying the sharpening and a slider to influence its weighting.
 * 
 * <p>
 * Depending of the graphics card, it may only provide a software implementation
 * although OpenGL is enabled.
 * 
 * @author Markus Langenberg
 * 
 */
public class SharpenPlugin extends SimpleFilterContainer {
    /**
     * {@inheritDoc}
     */
    protected Filter getFilter() {
        return new SharpenGLFilter();
    }

    /**
     * Use the basis class to refer to it consitently
     * 
     * @see org.helioviewer.jhv.plugins.viewmodelplugin.filter.SimpleFilterContainer#getFilterClass()
     */

    public Class<? extends Filter> getFilterClass() {
        return SharpenFilter.class;
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
        return new SharpenPanel();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "Sharpening";
    }

    /**
     * {@inheritDoc}
     */

    protected FilterTabDescriptor getFilterTab() {
        return new FilterTabDescriptor(FilterTabDescriptor.Type.COMPACT_FILTER, "");
    }
}
