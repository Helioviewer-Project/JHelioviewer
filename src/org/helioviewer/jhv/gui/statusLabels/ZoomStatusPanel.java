package org.helioviewer.jhv.gui.statusLabels;

import java.awt.Dimension;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

/**
 * Status panel for displaying the current zoom.
 * 
 * <p>
 * A displayed zoom of 100% means that one pixel one the screen corresponds to
 * exactly one pixel in the native resolution of the image.
 * 
 * <p>
 * The information of this panel is always shown for the active layer.
 * 
 * <p>
 * If there is no layer present, this panel will be invisible.
 */
public class ZoomStatusPanel extends StatusLabel{

    private static final long serialVersionUID = 1L;
    
    private static final String TITLE = "Zoom: ";
    
    /**
     * Default constructor.
     */
    public ZoomStatusPanel() {
    	super();
    	MainFrame.MAIN_PANEL.addStatusLabelCamera(this);
        setBorder(BorderFactory.createEtchedBorder());

        setPreferredSize(new Dimension(100, 20));
        setText(TITLE);
    }

    /**
     * Updates the displayed zoom.
     */
    private synchronized void updateZoomLevel() {
    	LayerInterface activeLayer = Layers.LAYERS.getActiveLayer();
        
    	if (activeLayer != null && activeLayer.getMetaData() != null){
    		MetaData metaData = activeLayer.getMetaData();

    		double unitsPerPixel = metaData.getUnitsPerPixel();
			double minCanvasDimension = MainFrame.MAIN_PANEL.getCanavasSize().getHeight();
			PhysicalRegion region = metaData.getPhysicalRegion();
    		double halfFOVRad = Math.toRadians(MainPanel.FOV / 2.0);
            double distance = (minCanvasDimension/2.0 * unitsPerPixel) / Math.tan(halfFOVRad);
            long zoom = Math.round(distance / MainFrame.MAIN_PANEL.getTranslation().z * 100);
            setText("Zoom: " + zoom + "%");
    	}
    	else setText(TITLE);
    }


    @Override
    public void activeLayerChanged(LayerInterface layer) {
        updateZoomLevel();
    }
    
    @Override
    public void cameraChanged() {
        updateZoomLevel();
    }

}
