package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.MainFrame;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public abstract class AbstractLayer
{
	private boolean visible = true;
	protected String name;
	
	public String getName()
	{
		return name;
	}

	public boolean isVisible()
	{
		return visible;
	}

	public boolean retryNeeded()
	{
		return false;
	}

	public void retry()
	{
	}

	public void setVisible(boolean _visible)
	{
		if(visible==_visible)
			return;
		
		visible = _visible;
		MainFrame.MAIN_PANEL.repaint();
	}

	public final boolean isImageLayer()
	{
		return this instanceof AbstractImageLayer;
	}
	
	public enum RenderResult
	{
		RETRY_LATER,
		ERROR,
		OK
	}

	public RenderResult renderLayer(GL2 _gl)
	{
		return RenderResult.OK;
	}

	public abstract void writeStateFile(JSONObject jsonLayer);
	
	public @Nullable String getDownloadURL()
	{
		return null;
	}

	public abstract @Nullable String getFullName();

	public abstract @Nullable LocalDateTime getCurrentTime();

	public abstract void dispose();

}
