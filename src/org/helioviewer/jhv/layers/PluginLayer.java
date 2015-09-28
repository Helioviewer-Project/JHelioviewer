package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;

import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.plugins.AbstractPlugin;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.viewmodel.TimeLine;

import com.jogamp.opengl.GL2;


public class PluginLayer extends AbstractLayer{
	
	final AbstractPlugin plugin;
	
	public PluginLayer(String name, AbstractPlugin plugin) {
		this.plugin = plugin;
		this.name = name;
		setVisible(plugin.isVisible());
	}

	@Override
	public RenderResult renderLayer(GL2 gl, Dimension canvasSize, MainPanel mainPanel,ByteBuffer _imageData)
	{
		plugin.render(gl);
		return RenderResult.OK;
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		plugin.setVisible(visible);
	}

	@Override
	void dispose() {
		Plugins.SINGLETON.deactivatePlugin(plugin);
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
	public void retryFailedRequests() {
		plugin.retryBadReqeuest();
	}
}
