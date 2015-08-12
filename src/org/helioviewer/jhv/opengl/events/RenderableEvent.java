package org.helioviewer.jhv.opengl.events;

import java.time.LocalDateTime;

import com.jogamp.opengl.GL2;

public abstract class RenderableEvent {
	enum VISIBLE_MDOE{
		VISIBLE, INVISIBLE, PREVIEW;
	}
	
	enum RENDER_MODE{
		ALL_PANEL, OVERVIEW_PANEL, MAIN_PANEL;
	}
	
	
	private final LocalDateTime startDateTime;
	private final LocalDateTime endDateTime;
	private boolean visible = false;
	private final String id;
	
	private RENDER_MODE renderMode = RENDER_MODE.ALL_PANEL;
	public RenderableEvent(String id, LocalDateTime startDateTime, LocalDateTime endDateTime) {
		this.id = id;
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
	}
	
	abstract public void render(GL2 gl);
	
	
	
}
