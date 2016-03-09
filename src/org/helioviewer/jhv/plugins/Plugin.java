package org.helioviewer.jhv.plugins;

import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.layers.PluginLayer;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public abstract class Plugin
{
	public final String id;
	public final String name;
	private boolean isVisible;
	
	public enum RenderMode
	{
		MAIN_PANEL, OVERVIEW_PANEL, BOTH_PANELS
	}

	public final RenderMode renderMode;

	public Plugin(String _id, String _name, RenderMode _renderMode)
	{
		id = _id;
		name = _name;
		renderMode = _renderMode;
	}
	
	public boolean supportsFilterContrastGamma()
	{
		return false;
	}
	
	public boolean supportsFilterSharpness()
	{
		return false;
	}
	
	public boolean supportsFilterRGB()
	{
		return false;
	}
	
	public boolean supportsFilterOpacity()
	{
		return false;
	}
	
	public boolean supportsFilterLUT()
	{
		return false;
	}
	
	public boolean supportsFilterCorona()
	{
		return false;
	}

	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
	}

	public void timeRangeChanged(@Nullable LocalDateTime _start, @Nullable LocalDateTime _end)
	{
	}
	
	public void visibilityChanged(boolean _isVisible)
	{
		isVisible=_isVisible;
	}
	
	public boolean isVisible()
	{
		return isVisible;
	}

	public void render(GL2 gl, PluginLayer _renderParams)
	{

	}

	public void mouseDragged(MouseEvent e, Vector3d point)
	{
	}

	public void mouseMoved(MouseEvent e, Vector3d point)
	{
	}

	public void mouseClicked(MouseEvent e, Vector3d point)
	{
	}

	public void mousePressed(MouseEvent e, Vector3d point)
	{
	}

	public void mouseReleased(MouseEvent e, Vector3d point)
	{
	}

	public void mouseEntered(MouseEvent e, Vector3d point)
	{
	}

	public void mouseExited(MouseEvent e, Vector3d point)
	{
	}

	abstract public void restoreConfiguration(JSONObject jsonObject) throws JSONException;

	abstract public void storeConfiguration(JSONObject jsonObject) throws JSONException;

	@Override
	public String toString()
	{
		return name;
	}

	abstract public boolean retryNeeded();

	abstract public void retry();
}
