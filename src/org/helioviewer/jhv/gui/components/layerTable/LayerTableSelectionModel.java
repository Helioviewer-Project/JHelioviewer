package org.helioviewer.jhv.gui.components.layerTable;

import javax.swing.DefaultListSelectionModel;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * SelectionModel reflecting the "activeLayer" property of the LayersModel
 * 
 * @author Malte Nuhn
 */
public class LayerTableSelectionModel extends DefaultListSelectionModel implements LayersListener {

    private static final long serialVersionUID = 2276237017135257828L;

    /** The sole instance of this class. */
    private static final LayerTableSelectionModel SINGLETON = new LayerTableSelectionModel();

    /**
     * Returns the only instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static LayerTableSelectionModel getSingletonInstance() {
        return SINGLETON;
    }

    /**
     * Default constructor
     */
    private LayerTableSelectionModel() {
        this.setSelectionMode(LayerTableSelectionModel.SINGLE_SELECTION);
        LayersModel.getSingletonInstance().addLayersListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * Update internal state and the underlying LayersModel state
     */

    public void setSelectionInterval(int index0, int index1) {
        super.setSelectionInterval(index0, index1);
        LayersModel.getSingletonInstance().setActiveLayer(index0);
    }

    /**
     * Helper needed to call the super.setSelectionInterval method from within
     * another thread
     * <p>
     * 
     * @see javax.swing.DefaultListSelectionModel#setSelectionInterval(int,int)
     *      setSelectionInterval
     */
    public void superSetSelectionInterval(int index0, int index1) {
        super.setSelectionInterval(index0, index1);
    }

    public void activeLayerChanged(int index) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                superSetSelectionInterval(LayersModel.getSingletonInstance().getActiveLayer(), LayersModel.getSingletonInstance().getActiveLayer());
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    public void layerAdded(int newIndex) {
        // Log.debug("LayerTableSelection: Layer Added, selecting " +
        // layersModel.getActiveLayer());
    }

    /**
     * {@inheritDoc}
     */
    public void layerChanged(int index) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerRemoved(View oldView, int oldIndex) {
        // Log.debug("LayerTableSelection: Layer Removed");
    }

    /**
     * {@inheritDoc}
     */
    public void viewportGeometryChanged() {
    }

    @Override
    public void subImageDataChanged(int idx)
    {
    }

    /**
     * {@inheritDoc}
     */
    public void timestampChanged(int idx) {
    }

    /**
     * {@inheritDoc}
     */
    public void layerDownloaded(int idx) {
    }
}
