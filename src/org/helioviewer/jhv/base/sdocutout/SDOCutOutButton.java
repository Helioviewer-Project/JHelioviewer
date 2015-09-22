package org.helioviewer.jhv.base.sdocutout;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;

public class SDOCutOutButton extends JButton implements
		PropertyChangeListener, LayerListener{

	private static final long serialVersionUID = 1L;

	public SDOCutOutButton()
	{
		super(new SDOCutOutAction());
		initButton();
		Layers.addNewLayerListener(this);
		this.setEnabled(false);
	}

	private void initButton() {
		setSelected(false);
		setIcon(IconBank.getIcon(JHVIcon.SDO_CUT_OUT, 24, 24));
		setToolTipText("Connect to SDO Cut-Out Service");
		setEnabled(true);
		setVerticalTextPosition(SwingConstants.BOTTOM);
		setHorizontalAlignment(SwingConstants.CENTER);
		setHorizontalTextPosition(SwingConstants.CENTER);
	}

	@Override
	/*
	 * This method is called by the event firePropertyChange to add the plugin
	 * button in the TopToolBar
	 */
	public void propertyChange(PropertyChangeEvent evt) {
	}

	@Override
	public void newLayerAdded() {
		boolean enable = false;
		for (AbstractLayer layer : Layers.getLayers()){
			if (layer.getName().contains("AIA")){
				enable = true;
			}
		}
		this.setEnabled(enable);
	}

	@Override
	public void newlayerRemoved(int idx) {
		
		
	}

	@Override
	public void activeLayerChanged(AbstractLayer layer) {
		
		
	}
}
