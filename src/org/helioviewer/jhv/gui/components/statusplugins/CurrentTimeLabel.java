package org.helioviewer.jhv.gui.components.statusplugins;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.viewmodel.view.View;


public class CurrentTimeLabel extends JLabel implements LayersListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9216168376764918306L;
	
	private String title = "Current Date: ";
	private String empty = " - ";
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
	
	public CurrentTimeLabel() {
        this.setBorder(BorderFactory.createEtchedBorder());

		this.setText(title + empty);
		LayersModel.getSingletonInstance().addLayersListener(this);		
	}
	@Override
	public void layerAdded(int idx) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void layerRemoved(View oldView, int oldIdx) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void layerChanged(int idx) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void activeLayerChanged(int idx) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void viewportGeometryChanged() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void timestampChanged(int idx) {
		if (LayersModel.getSingletonInstance().getLastUpdatedTimestamp() != null){
			this.setText(title + dateFormat.format(LayersModel.getSingletonInstance().getLastUpdatedTimestamp()));
		}
		else{
			this.setText(title + empty);
		}
	}
	@Override
	public void subImageDataChanged() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void layerDownloaded(int idx) {
		// TODO Auto-generated method stub
		
	}

}
