package org.helioviewer.jhv.gui.statusLabels;

import java.awt.Dimension;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

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
public class ZoomStatusPanel extends StatusLabel
{
	public ZoomStatusPanel(MainPanel _mp)
	{
		super();
		_mp.addCameraListener(this);
		setBorder(BorderFactory.createEtchedBorder());

		setPreferredSize(new Dimension(100, 20));
		setText("Zoom:");
	}

	/**
	 * Updates the displayed zoom.
	 */
	private void updateZoomLevel()
	{
		ImageLayer activeLayer = Layers.getActiveImageLayer();
		if (activeLayer == null)
		{
			setText("Zoom:");
			return;
		}
		
		MetaData metaData = activeLayer.getMetaData(TimeLine.SINGLETON.getCurrentTimeMS());
		if(metaData==null)
		{
			setText("Zoom:");
			return;
		}

		double halfFOVRad = Math.toRadians(MainPanel.FOV / 2.0);
		double distance = (MainFrame.SINGLETON.MAIN_PANEL.getCanavasSize().getHeight() / 2.0 * metaData.getUnitsPerPixel().y) / Math.tan(halfFOVRad);
		long zoom = Math.round(distance	/ MainFrame.SINGLETON.MAIN_PANEL.getTranslationCurrent().z * 100);
		setText("Zoom: " + zoom + "%");
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
		updateZoomLevel();
	}

	@Override
	public void cameraChanged()
	{
		updateZoomLevel();
	}

}
