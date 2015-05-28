package org.helioviewer.jhv.plugins.plugin;

import java.util.concurrent.CopyOnWriteArrayList;

public class Plugins {
	CopyOnWriteArrayList<RenderablePlugin> plugins;
	
	
	public static final Plugins PLUGINS = new Plugins();

	private Plugins() {
		plugins = new CopyOnWriteArrayList<RenderablePlugin>();
	}
	
	public void addPlugin(RenderablePlugin plugin){
		plugins.add(plugin);
	}
	
	public CopyOnWriteArrayList<RenderablePlugin> getPlugins(){
		return plugins;
	}
	
}
