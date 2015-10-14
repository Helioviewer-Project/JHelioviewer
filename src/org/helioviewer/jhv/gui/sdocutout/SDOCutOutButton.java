package org.helioviewer.jhv.gui.sdocutout;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataAIA;

public class SDOCutOutButton extends JButton implements	LayerListener
{
	public SDOCutOutButton()
	{
		super(new SDOCutOutAction());
		initButton();
		Layers.addLayerListener(this);
		this.setEnabled(false);
	}

	private void initButton()
	{
		setSelected(false);
		setIcon(IconBank.getIcon(JHVIcon.SDO_CUT_OUT, 24, 24));
		setToolTipText("Open SDO Cut-Out Service");
		setEnabled(true);
		setVerticalTextPosition(SwingConstants.BOTTOM);
		setHorizontalAlignment(SwingConstants.CENTER);
		setHorizontalTextPosition(SwingConstants.CENTER);
	}

	private void enableIffSDOLayersActive()
	{
		for (AbstractLayer layer : Layers.getLayers())
			if(layer instanceof KakaduLayer)
				if(((KakaduLayer)layer).getMetaData(TimeLine.SINGLETON.getCurrentDateTime()) instanceof MetaDataAIA)
				{
					setEnabled(true);
					return;
				}
		
		setEnabled(false);
	}

	@Override
	public void layerAdded()
	{
		enableIffSDOLayersActive();
	}

	@Override
	public void layersRemoved()
	{
		enableIffSDOLayersActive();
	}

	@Override
	public void activeLayerChanged(@Nullable AbstractLayer layer)
	{
	}
}
