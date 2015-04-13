package org.helioviewer.jhv.viewmodel.timeline;

import java.time.LocalDateTime;

public class TimeLine {
	
	private LocalDateTime start;
	private LocalDateTime end;
	private LocalDateTime current;
	
	private int frameCount;
	
	public static TimeLine SINGLETON = new TimeLine();
	
	private TimeLine() {
		// TODO Auto-generated constructor stub
	}
	
	public void setFrameCount(int frameCount){
		this.frameCount = frameCount;
	}
}
