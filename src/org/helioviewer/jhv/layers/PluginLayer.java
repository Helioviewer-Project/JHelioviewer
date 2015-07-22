package org.helioviewer.jhv.layers;

import java.awt.Dimension;

import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.plugins.plugin.AbstractPlugin;
import org.helioviewer.jhv.plugins.plugin.UltimatePluginInterface;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;

import com.jogamp.opengl.GL2;


public class PluginLayer extends AbstractLayer{
	
	final AbstractPlugin plugin;
	
	public PluginLayer(String name, AbstractPlugin plugin) {
		this.plugin = plugin;
		this.name = name;
		setVisible(plugin.isVisible());
	}

	@Override
	public boolean renderLayer(GL2 gl, Dimension canvasSize, MainPanel mainPanel) {
		plugin.render(gl);
		return true;
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		plugin.setVisible(visible);
	}

	@Override
	void remove() {
		UltimatePluginInterface.SINGLETON.removePlugin(plugin);
	}	
	
	@Override
	public boolean checkBadRequest() {
		return plugin.checkBadRequests(TimeLine.SINGLETON.getFirstDateTime(), TimeLine.SINGLETON.getLastDateTime());
	}
	
	@Override
	public int getBadRequestCount() {
		return plugin.getBadRequestCount();
	}
	
	@Override
	public void retryBadRequest() {
		plugin.retryBadReqeuest();
	}
}
