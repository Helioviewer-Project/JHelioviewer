package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.gui.OverviewPanel;
import org.helioviewer.jhv.plugins.Plugin;
import org.helioviewer.jhv.plugins.Plugin.RenderMode;
import org.helioviewer.jhv.plugins.Plugins;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PluginLayer extends Layer
{
	final Plugin plugin;
	
	public boolean supportsFilterContrastGamma()
	{
		return plugin.supportsFilterContrastGamma();
	}
	
	public boolean supportsFilterSharpness()
	{
		return plugin.supportsFilterSharpness();
	}
	
	public boolean supportsFilterRGB()
	{
		return plugin.supportsFilterRGB();
	}
	
	public boolean supportsFilterOpacity()
	{
		return plugin.supportsFilterOpacity();
	}
	
	public boolean supportsFilterLUT()
	{
		return plugin.supportsFilterLUT();
	}
	
	public boolean supportsFilterCorona()
	{
		return plugin.supportsFilterCorona();
	}
	
	public PluginLayer(Plugin _plugin)
	{
		name = _plugin.pluginName;
		plugin = _plugin;
		
		plugin.visibilityChanged(isVisible());
	}

	public RenderResult renderLayer(GL2 gl, MainPanel _parent)
	{
		if(_parent instanceof OverviewPanel)
		{
			if(plugin.renderMode==RenderMode.MAIN_PANEL)
				return RenderResult.OK;
		}
		else
		{
			if(plugin.renderMode==RenderMode.OVERVIEW_PANEL)
				return RenderResult.OK;
		}
		
		plugin.render(gl,this);
		return RenderResult.OK;
	}
	
	@Override
	public void setVisible(boolean _visible)
	{
		super.setVisible(_visible);
		plugin.visibilityChanged(_visible);
	}
	
	@Override
	public void dispose()
	{
		throw new RuntimeException();
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
		Plugins.repaintLayerPanel();
	}

	@Override
	public @Nullable LocalDateTime getCurrentTime()
	{
		return null;
	}

	@Override
	public void storeConfiguration(JSONObject _jsonLayer)
	{
		plugin.storeConfiguration(_jsonLayer);
	}

	@Override
	public @Nullable String getFullName()
	{
		return plugin.pluginName;
	}
}
