package org.helioviewer.jhv.opengl.events;

import java.time.LocalDateTime;

public class RenderableEvent {
	
	private final LocalDateTime startDateTime;
	private final LocalDateTime endDateTime;
	
	public RenderableEvent(LocalDateTime startDateTime, LocalDateTime endDateTime) {
		this.startDateTime = startDateTime;
		this.endDateTime = endDateTime;
	}
	
	public LocalDateTime getStartDateTime(){
		return startDateTime;
	}
	
	public LocalDateTime getEndDateTime(){
		return endDateTime;
	}

}
