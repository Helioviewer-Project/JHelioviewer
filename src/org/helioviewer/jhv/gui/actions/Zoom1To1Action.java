package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.camera.animation.CameraZoomAnimation;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class Zoom1To1Action extends AbstractAction implements LayerListener
{
	public Zoom1To1Action(boolean small)
	{
		super("Zoom 1:1", small ? IconBank.getIcon(JHVIcon.NEW_ZOOM_1TO1, 16,16) : IconBank.getIcon(JHVIcon.NEW_ZOOM_1TO1, 24, 24));
		putValue(SHORT_DESCRIPTION, "Zoom to 100%");
		putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK));
		
		Layers.addLayerListener(this);
	}

	public void actionPerformed(@Nullable ActionEvent e)
	{
		MainPanel componentView = MainFrame.SINGLETON.MAIN_PANEL;
		ImageLayer activeLayer = Layers.getActiveImageLayer();
		if (activeLayer == null)
			return;
		
		MetaData metaData = activeLayer.getMetaData(TimeLine.SINGLETON.getCurrentTimeMS());
		
		if(metaData==null)
			return;
		
		Vector2d unitsPerPixel = metaData.getUnitsPerPixel();

		double halfFOVRad = Math.toRadians(MainPanel.FOV / 2.0);
		double distance = (MainFrame.SINGLETON.MAIN_PANEL.getCanavasSize().getHeight() / 2.0 * unitsPerPixel.y) / Math.tan(halfFOVRad);
		
		distance = distance - componentView.getTranslationEnd().z;
		componentView.addCameraAnimation(new CameraZoomAnimation(componentView, distance));
	}
	
	@Override
	public void layerAdded()
	{
	}

	@Override
	public void layersRemoved()
	{
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
		setEnabled(Layers.getActiveImageLayer()!=null);
	}
}