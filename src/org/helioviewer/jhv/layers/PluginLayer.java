package org.helioviewer.jhv.layers;

import org.helioviewer.jhv.plugins.plugin.AbstractPlugin;

import com.jogamp.opengl.GL2;


public class PluginLayer extends AbstractLayer{
	
	final AbstractPlugin plugin;
	
	public PluginLayer(String name, AbstractPlugin plugin) {
		this.plugin = plugin;
		this.name = name;
		setVisible(plugin.isVisible());
	}

	@Override
	void renderLayer(GL2 gl) {
		plugin.render(gl);
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		plugin.setVisible(visible);
	}
}
