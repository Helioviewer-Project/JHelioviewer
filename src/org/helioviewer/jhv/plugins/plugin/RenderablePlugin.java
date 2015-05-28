package org.helioviewer.jhv.plugins.plugin;

import java.time.LocalDateTime;

import com.jogamp.opengl.GL2;

public interface RenderablePlugin {	
	void render(GL2 gl);
	LocalDateTime getDateTime();
	
}
