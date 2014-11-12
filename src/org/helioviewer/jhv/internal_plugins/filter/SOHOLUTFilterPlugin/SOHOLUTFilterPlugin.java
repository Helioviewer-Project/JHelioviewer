package org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterContainer;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTab;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabDescriptor.Type;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabList;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabPanelManager;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabPanelManager.Area;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.imageformat.SingleChannelImageFormat;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.FilterView;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.SubimageDataView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJP2View;

/**
 * Filter plugin for applying a color table to single channel images.
 * 
 * <p>
 * This plugin provides several different color tables to improve the visual
 * impression of single channel images. It manages a filter for applying the
 * color table and a combobox to change it.
 * 
 * partly rewritten
 * 
 * @author Helge Dietert
 */
public class SOHOLUTFilterPlugin extends FilterContainer {

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
        return "Color Tables";
    }

    /**
     * If the given meta data contain helioviewer meta data, the default color
     * table for the given instrument is chosen. Otherwise, the color table is
     * set to gray by default, which does not change the color at all.
     */

    protected void installFilterImpl(FilterView filterView, FilterTabList tabList) {
        // Only applicable for SingeChannelFormat
        if (!(filterView.getAdapter(SubimageDataView.class).getImageData().getImageFormat() instanceof SingleChannelImageFormat))
            return;

        SOHOLUTFilter filter = new SOHOLUTFilter();

        FilterTabPanelManager manager = tabList.getFirstPanelManagerByType(Type.COMPACT_FILTER);
        if (manager == null) {
            manager = new FilterTabPanelManager();
            tabList.add(new FilterTab(Type.COMPACT_FILTER, getName(), manager));
        }

        SOHOLUTPanel pane = new SOHOLUTPanel();
        pane.setFilter(filter);
        manager.add(pane, Area.CENTER);

        // Set standard gray and install filter
        pane.setLutByName("Gray");
        filterView.setFilter(filter);

        // Now need to set the initial filter
        JHVJP2View jp2View = filterView.getAdapter(JHVJP2View.class);
        if (jp2View != null) {
            int[] builtIn = jp2View.getBuiltInLUT();
            if (builtIn != null) {
                LUT builtInLut = new LUT("built-in", builtIn, builtIn);
                pane.addLut(builtInLut);
                return;
            }
        }

        // Standard using meta data
        MetaDataView metaDataView = filterView.getAdapter(MetaDataView.class);
        if (metaDataView != null) {
            MetaData metaData = metaDataView.getMetaData();
            	String colorKey = DefaultTable.getSingletonInstance().getColorTable(metaData);
                if (colorKey != null) {
                    Log.debug("Try to apply color table " + colorKey);
                    pane.setLutByName(colorKey);
                    return;
                
            }
        }

        // Otherwise gray
        pane.setLutByName("Gray");
    }

    /**
     * {@inheritDoc}
     */

    public Class<? extends Filter> getFilterClass() {
        return SOHOLUTFilter.class;
    }
}
