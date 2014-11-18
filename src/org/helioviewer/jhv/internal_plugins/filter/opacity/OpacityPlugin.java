package org.helioviewer.jhv.internal_plugins.filter.opacity;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.jhv.plugins.viewmodelplugin.filter.SimpleFilterContainer;
import org.helioviewer.jhv.viewmodel.filter.Filter;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.FilterView;
import org.helioviewer.jhv.viewmodel.view.LayeredView;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;

/**
 * Plugin for changing the opacity of the image.
 * 
 * <p>
 * The plugin manages a filter for changing the opacity and a slider to change
 * it.
 * 
 * @author Markus Langenberg
 * 
 */
public class OpacityPlugin extends SimpleFilterContainer {

    float initialOpacity = 1.0f;

    /**
     * {@inheritDoc}
     */

    protected Filter getFilter() {
        return new OpacityFilter(initialOpacity);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * In this case, this function also calculates the inital opacity value.
     */

    protected boolean useFilter(FilterView view) {
    	
        	MetaData currentMetaData = view.getAdapter(MetaDataView.class).getMetaData();
            if (currentMetaData.hasCorona()){
            	initialOpacity = 1.0f;
            	return true;
            }
        if (GuiState3DWCS.mainComponentView == null) {
            initialOpacity = 1.0f;
            return true;
        }

        LayeredView layeredView = GuiState3DWCS.mainComponentView.getAdapter(LayeredView.class);
        
        if (layeredView == null) {
            initialOpacity = 1.0f;
            return true;
        }
            
        /*
        int layerCount = 1;
        for (int i = 0; i < layeredView.getNumLayers(); i++) {
        		MetaData metaData = layeredView.getLayer(i).getAdapter(MetaDataView.class).getMetaData();
            	if (metaData.hasSphere()) layerCount++;
        }*/
        
		initialOpacity = 1.0f;
        return true;
    }

    /**
     * {@inheritDoc}
     */

    protected FilterPanel getPanel() {
        return new OpacityPanel();
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
        return "Opacity";
    }

    /**
     * {@inheritDoc}
     */

    protected FilterTabDescriptor getFilterTab() {
        return new FilterTabDescriptor(FilterTabDescriptor.Type.COMPACT_FILTER, "");
    }

}
