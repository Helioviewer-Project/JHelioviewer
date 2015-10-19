package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.gui.OverviewPanel;
import org.helioviewer.jhv.plugins.AbstractPlugin;
import org.helioviewer.jhv.plugins.AbstractPlugin.RenderMode;
import org.helioviewer.jhv.plugins.Plugins;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PluginLayer extends Layer
{	
	final AbstractPlugin plugin;
	
	public PluginLayer(String _name, AbstractPlugin _plugin)
	{
		name = _name;
		plugin = _plugin;
	}

	public RenderResult renderLayer(GL2 gl, MainPanel _parent)
	{
		if(_parent instanceof OverviewPanel)
		{
			if(plugin.getRenderMode()==RenderMode.MAIN_PANEL)
				return RenderResult.OK;
		}
		else
		{
			if(plugin.getRenderMode()==RenderMode.OVERVIEW_PANEL)
				return RenderResult.OK;
		}
		
		plugin.render(gl);
		return RenderResult.OK;
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
