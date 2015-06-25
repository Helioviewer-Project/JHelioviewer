package org.helioviewer.jhv.plugins.plugin;

import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.Vector3d;

import com.jogamp.opengl.GL2;

public abstract class NewPlugin {
	public enum RENDER_MODE {
		MAIN_PANEL, OVERVIEW_PANEL, ALL_PANEL;
	};
	protected RENDER_MODE renderMode = RENDER_MODE.ALL_PANEL;
	
	public void timeStampChanged(LocalDateTime current, LocalDateTime last){
		
	}
	
	public void dateTimesChanged(int framecount) {
		// TODO Auto-generated method stub
		
	}
	
	public void render(GL2 gl){
		
	}
	
	public abstract String getAboutLicenseText();	
	
	
	public void mouseDragged(MouseEvent e, Vector3d point) {
	}

	public void mouseMoved(MouseEvent e, Vector3d point) {
	}

	public void mouseClicked(MouseEvent e, Vector3d point) {
	}

	public void mousePressed(MouseEvent e, Vector3d point) {
	}

	public void mouseReleased(MouseEvent e, Vector3d point) {
	}

	public void mouseEntered(MouseEvent e, Vector3d point) {
	}

	public void mouseExited(MouseEvent e, Vector3d point) {
	}
	
	public RENDER_MODE getRenderMode(){
		return this.renderMode;
	}
}
