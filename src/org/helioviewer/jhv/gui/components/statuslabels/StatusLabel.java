package org.helioviewer.jhv.gui.components.statuslabels;

import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

import javax.swing.JLabel;

import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.gui.components.statuslabels.StatusLabelInterfaces.StatusLabelCamera;
import org.helioviewer.jhv.gui.components.statuslabels.StatusLabelInterfaces.StatusLabelMouse;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.NewLayerListener;
import org.helioviewer.jhv.opengl.raytrace.RayTrace.Ray;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;

public class StatusLabel extends JLabel implements TimeLineListener, StatusLabelMouse, StatusLabelCamera, NewLayerListener{
	
	public StatusLabel() {
		TimeLine.SINGLETON.addListener(this);
		Layers.LAYERS.addNewLayerListener(this);
	}
	
	public void mouseExited(MouseEvent e, Ray ray) {
		// TODO Auto-generated method stub
		
	}

	public void mouseMoved(MouseEvent e, Ray ray) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dateTimesChanged(int framecount) {
		// TODO Auto-generated method stub
		
	}
	
	public void cameraChanged(){
		
	}

	@Override
	public void newlayerAdded() {
	}

	@Override
	public void newlayerRemoved(int idx) {
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
	}	
}
