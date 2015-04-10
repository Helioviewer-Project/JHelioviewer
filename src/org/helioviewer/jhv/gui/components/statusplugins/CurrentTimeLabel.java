package org.helioviewer.jhv.gui.components.statusplugins;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;


public class CurrentTimeLabel extends JLabel implements LayersListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -9216168376764918306L;
	
	private String empty = " - ";
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ");
	
	public CurrentTimeLabel() {
        this.setBorder(BorderFactory.createEtchedBorder());

		this.setText(empty);
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
		if (LayersModel.getSingletonInstance().getActiveView() != null){
			JHVJPXView jhvjpxView = LayersModel.getSingletonInstance().getActiveView().getAdapter(JHVJPXView.class);
			this.setText(dateFormat.format(jhvjpxView.getCurrentFrameDateTime().getTime()));
		}
		else{
			this.setText(empty);
		}
	}
	
	@Override
	public void subImageDataChanged(int idx)
	{
	}
	
	@Override
	public void layerDownloaded(int idx)
	{
	}

}
