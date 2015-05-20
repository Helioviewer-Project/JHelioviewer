package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.gui.controller.Camera;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;


public abstract class CameraInteraction{
	protected MainPanel compenentView;
	protected Camera camera;
	
	public CameraInteraction(MainPanel compenentView, Camera camera) {
		this.compenentView = compenentView;
		this.camera = camera;
	}

	public void mouseWheelMoved(MouseWheelEvent e){	
	}
	
	public void mousePressed(MouseEvent e) {
	}
	
	public void mouseDragged(MouseEvent e) {
	}
	
	public void mouseReleased(MouseEvent e) {
	}
	
	public void setYAxisBlocked(boolean selected) {
		// TODO Auto-generated method stub
		
	}	
}

