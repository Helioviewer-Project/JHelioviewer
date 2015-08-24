package org.helioviewer.jhv.opengl.events;

import java.awt.image.BufferedImage;
import java.util.HashMap;


public class Events {
	
	private static final HashMap<String, RenderableEvents> events = new HashMap<String, RenderableEvents>();

	public static RenderableEvents addEvents(String name){
		return addEvents(name, null);
	}
	
	public static RenderableEvents addEvents(String name, BufferedImage image){
		RenderableEvents renderableEvents = new RenderableEvents(name, image);
		events.put(name, renderableEvents);
		return renderableEvents;		
	}	
}
