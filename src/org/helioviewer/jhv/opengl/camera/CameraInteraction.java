package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.gui.controller.Camera;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

import com.jogamp.opengl.GL2;


public abstract class CameraInteraction{
	protected MainPanel mainPanel;
	protected Camera camera;
	
	public CameraInteraction(MainPanel mainPanel, Camera camera) {
		this.mainPanel = mainPanel;
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
	
	public void renderInteraction(GL2 gl){
		
	}
}

