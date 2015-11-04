package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.MainFrame;
import org.json.JSONObject;

public abstract class Layer
{
	private boolean visible = true;
	@SuppressWarnings("null")
	protected String name;
	
	public final String getName()
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
		MainFrame.SINGLETON.MAIN_PANEL.repaint();
	}

	public enum RenderResult
	{
		RETRY_LATER,
		ERROR,
		OK
	}

	public abstract void storeConfiguration(JSONObject jsonLayer);
	
	public @Nullable String getDownloadURL()
	{
		return null;
	}

	//TODO: get rid of this and merge with getName() - or properly document the difference
	public abstract @Nullable String getFullName();

	public abstract @Nullable LocalDateTime getCurrentTime();

	public abstract void dispose();

}
