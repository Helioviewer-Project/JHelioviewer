package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;


public abstract class CameraInteraction{

	protected boolean enable = false;
	protected CompenentView compenentView;
	
	public CameraInteraction(CompenentView compenentView) {
		this.compenentView = compenentView;
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
	
	public void setEnable(boolean enable){
		this.enable = enable;
	}
	
	public boolean isEnable(){
		return this.enable;
	}
}

