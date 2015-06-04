package org.helioviewer.jhv.plugins.plugin;

import java.time.LocalDateTime;

import com.jogamp.opengl.GL2;

public abstract class NewPlugin {
	
	public void timeStampChanged(LocalDateTime current, LocalDateTime last){
		
	}
	
	public void dateTimesChanged(int framecount) {
		// TODO Auto-generated method stub
		
	}
	
	public void render(GL2 gl){
		
	}
	
	public abstract String getAboutLicenseText();	
}
