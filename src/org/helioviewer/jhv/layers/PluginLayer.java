package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.plugins.AbstractPlugin;
import org.helioviewer.jhv.plugins.Plugins;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PluginLayer extends AbstractLayer
{	
	final AbstractPlugin plugin;
	
	public PluginLayer(String _name, AbstractPlugin _plugin)
	{
		plugin = _plugin;
		name = _name;
		setVisible(_plugin.isVisible());
	}

	@Override
	public RenderResult renderLayer(GL2 gl, Dimension canvasSize, MainPanel mainPanel,ByteBuffer _imageData)
	{
		plugin.render(gl);
		return RenderResult.OK;
	}
	
	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		plugin.setVisible(visible);
	}

	@Override
	public void dispose()
	{
		Plugins.SINGLETON.deactivatePlugin(plugin);
	}	
	
	@Override
	public boolean retryNeeded()
	{
		return plugin.retryNeeded();
	}
	
	@Override
	public void retry()
	{
		plugin.retry();
	}

	@Override
	public LocalDateTime getCurrentTime()
	{
		return null;
	}

	@Override
	public void writeStateFile(JSONObject _jsonLayer)
	{
		plugin.storeConfiguration(_jsonLayer);
	}

	@Override
	public String getFullName()
	{
		return null;
	}
}
