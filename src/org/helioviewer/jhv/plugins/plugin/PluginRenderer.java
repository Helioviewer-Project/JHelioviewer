package org.helioviewer.jhv.plugins.plugin;

import java.time.LocalDateTime;

import com.jogamp.opengl.GL2;

public interface PluginRenderer {	
	public void render(GL2 gl);
	public LocalDateTime getDateTime();
	public boolean isVisible();
}
