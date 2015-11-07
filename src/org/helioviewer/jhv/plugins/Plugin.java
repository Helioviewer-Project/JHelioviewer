package org.helioviewer.jhv.plugins;

import com.jogamp.opengl.GL2;
import org.helioviewer.jhv.base.math.Vector3d;
import org.json.JSONObject;

import java.awt.event.MouseEvent;
import java.time.LocalDateTime;

public abstract class Plugin
{
	public final String pluginName;
	private boolean isVisible;
	
	public enum RenderMode
	{
		MAIN_PANEL, OVERVIEW_PANEL, BOTH_PANELS
	}

	public final RenderMode renderMode;

	public Plugin(String name, RenderMode _renderMode)
	{
		pluginName = name;
		renderMode = _renderMode;
	}

	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
	}

	public void dateTimesChanged(int framecount)
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

	public void render(GL2 gl)
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

	abstract public void restoreConfiguration(JSONObject jsonObject);

	abstract public void storeConfiguration(JSONObject jsonObject);

	@Override
	public String toString()
	{
		return pluginName;
	}

	abstract public boolean retryNeeded();

	abstract public void retry();
}
