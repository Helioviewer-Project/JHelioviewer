package org.helioviewer.jhv.gui.statusLabels;

import java.awt.Dimension;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.AbstractLayer;
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
	private static final long serialVersionUID = 1L;

	public ZoomStatusPanel()
	{
		super();
		MainFrame.MAIN_PANEL.addStatusLabelCameraListener(this);
		setBorder(BorderFactory.createEtchedBorder());

		setPreferredSize(new Dimension(100, 20));
		setText("Zoom:");
	}

	/**
	 * Updates the displayed zoom.
	 */
	private void updateZoomLevel()
	{
		AbstractImageLayer activeLayer = Layers.getActiveImageLayer();
		if (activeLayer == null)
		{
			setText("Zoom:");
			return;
		}
		
		LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		MetaData metaData = activeLayer.getMetaData(currentDateTime);
		if(metaData==null)
		{
			setText("Zoom:");
			return;
		}

		double unitsPerPixel = metaData.getUnitsPerPixel();
		double minCanvasDimension = MainFrame.MAIN_PANEL.getCanavasSize().getHeight();
		
		double halfFOVRad = Math.toRadians(MainPanel.FOV / 2.0);
		double distance = (minCanvasDimension / 2.0 * unitsPerPixel) / Math.tan(halfFOVRad);
		long zoom = Math.round(distance	/ MainFrame.MAIN_PANEL.getTranslation().z * 100);
		setText("Zoom: " + zoom + "%");
	}

	@Override
	public void activeLayerChanged(AbstractLayer layer)
	{
		updateZoomLevel();
	}

	@Override
	public void cameraChanged()
	{
		updateZoomLevel();
	}

}
