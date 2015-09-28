package org.helioviewer.jhv.layers;

import java.awt.Dimension;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
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

	abstract public RenderResult renderLayer(GL2 gl, Dimension canvasSize, MainPanel mainPanel, ByteBuffer _imageData);

	public abstract void writeStateFile(JSONObject jsonLayer);
	
	@Nullable
	public String getDownloadURL()
	{
		return null;
	}

	public abstract String getFullName();

	@Nullable
	public abstract LocalDateTime getCurrentTime();

	public abstract void dispose();

}
