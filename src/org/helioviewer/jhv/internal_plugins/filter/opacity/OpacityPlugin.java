package org.helioviewer.jhv.internal_plugins.filter.opacity;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.viewmodel.filter.Filter;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodelplugin.filter.FilterPanel;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.viewmodelplugin.filter.SimpleFilterContainer;

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
    	
    	try {
        	MetaData currentMetaData = view.getAdapter(MetaDataView.class).getMetaData();
            if (currentMetaData.getDetector().startsWith("COR")){
            	initialOpacity = 1.0f;
            	return true;
            }
		} catch (Exception e) {
			Log.error("Metadata " + view.getAdapter(MetaDataView.class).getMetaData() + " can't be cast to org.helioviewer.viewmodel.metadata.HelioviewerMetaData");
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
            
        int layerCount = 1;
        for (int i = 0; i < layeredView.getNumLayers(); i++) {
        		MetaData metaData = layeredView.getLayer(i).getAdapter(MetaDataView.class).getMetaData();
            	if (!metaData.getDetector().startsWith("COR")) layerCount++;
        }
        
		initialOpacity = 1.0f / layerCount;
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
