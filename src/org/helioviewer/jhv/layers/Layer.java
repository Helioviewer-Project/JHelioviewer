package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.gui.MainFrame;
import org.json.JSONObject;

public abstract class Layer
{
	public double opacity = 1;
	public double sharpness = 0;
	public double gamma = 1;
	public double contrast = 0;
	@Nullable protected LUT lut = null;
	public boolean redChannel = true;
	public boolean greenChannel = true;
	public boolean blueChannel = true;
	public boolean invertedLut = false;
	protected boolean coronaVisible = true;
	
	private boolean visible = true;
	protected String name;
	
	
	@Nullable public LUT getLUT()
	{
		return lut;
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
	
	public void setLUT(@Nullable LUT _lut)
	{
		if(!supportsFilterLUT())
			throw new IllegalStateException("Layer does not support LUTs");
		
		lut = _lut;
		MainFrame.SINGLETON.FILTER_PANEL.update();
	}

	public void toggleCoronaVisibility()
	{
		coronaVisible=!coronaVisible;
	}
	
	public boolean isCoronaVisible()
	{
		if(!supportsFilterCorona())
			throw new IllegalStateException("Layer does not support LUTs");
		
		return coronaVisible;
	}

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
	
	public boolean isLoading()
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
