package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.plugins.AbstractPlugin;
import org.helioviewer.jhv.plugins.Plugins;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PluginLayer extends AbstractLayer
{	
	final AbstractPlugin plugin;
	
	public PluginLayer(String _name, AbstractPlugin _plugin)
	{
		name = _name;
		plugin = _plugin;
		setVisible(_plugin.isVisible());
	}

	@Override
	public RenderResult renderLayer(GL2 gl)
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
	public @Nullable LocalDateTime getCurrentTime()
	{
		return null;
	}

	@Override
	public void writeStateFile(JSONObject _jsonLayer)
	{
		plugin.storeConfiguration(_jsonLayer);
	}

	@Override
	public @Nullable String getFullName()
	{
		return null;
	}
}
