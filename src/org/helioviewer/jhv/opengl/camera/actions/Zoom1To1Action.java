package org.helioviewer.jhv.opengl.camera.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.camera.animation.CameraZoomAnimation;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class Zoom1To1Action extends AbstractAction {

	private static final long serialVersionUID = 1L;

	public Zoom1To1Action(boolean small) {
		super("Zoom 1:1", small ? IconBank.getIcon(JHVIcon.NEW_ZOOM_1TO1, 16, 16)
				: IconBank.getIcon(JHVIcon.NEW_ZOOM_1TO1, 24 ,24));
		putValue(SHORT_DESCRIPTION, "Zoom to Native Resolution");
		putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
		putValue(ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e) {
		MainPanel compenentView = MainFrame.MAIN_PANEL;
    	LayerInterface activeLayer = Layers.LAYERS.getActiveLayer();
        
    	if (activeLayer != null && activeLayer.getMetaData() != null){
    		MetaData metaData = activeLayer.getMetaData();
			double unitsPerPixel = metaData.getUnitsPerPixel();
			PhysicalRegion region = metaData.getPhysicalRegion();
            
			if (region != null) {
				Dimension dimension = MainFrame.MAIN_PANEL.getCanavasSize();
				double minCanvasDimension = dimension.getHeight();
				
	            double halfFOVRad = Math.toRadians(MainPanel.FOV / 2.0);
	            double distance = (minCanvasDimension/2.0 * unitsPerPixel) / Math.tan(halfFOVRad);
	            distance = distance - compenentView.getTranslation().z;
	            compenentView.addCameraAnimation(new CameraZoomAnimation(distance));
			}
		}
	}
}