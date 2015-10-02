package org.helioviewer.jhv.gui.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.camera.animation.CameraZoomAnimation;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class Zoom1To1Action extends AbstractAction
{
	public Zoom1To1Action(boolean small)
	{
		super("Zoom 1:1", small ? IconBank.getIcon(JHVIcon.NEW_ZOOM_1TO1, 16,16) : IconBank.getIcon(JHVIcon.NEW_ZOOM_1TO1, 24, 24));
		putValue(SHORT_DESCRIPTION, "Zoom to Native Resolution");
		putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e)
	{
		MainPanel componentView = MainFrame.MAIN_PANEL;
		AbstractImageLayer activeLayer = Layers.getActiveImageLayer();
		if (activeLayer == null)
			return;
		
		LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		MetaData metaData = activeLayer.getMetaData(currentDateTime);
		
		if(metaData==null)
			return;
		
		double unitsPerPixel = metaData.getUnitsPerPixel();
		Rectangle2D region = metaData.getPhysicalImageSize();

		if (region == null)
			return;

		Dimension dimension = MainFrame.MAIN_PANEL.getCanavasSize();
		double minCanvasDimension = dimension.getHeight();

		double halfFOVRad = Math.toRadians(MainPanel.FOV / 2.0);
		double distance = (minCanvasDimension / 2.0 * unitsPerPixel) / Math.tan(halfFOVRad);
		distance = distance - componentView.getTranslationEnd().z;
		componentView.addCameraAnimation(new CameraZoomAnimation(componentView, distance));
	}
}