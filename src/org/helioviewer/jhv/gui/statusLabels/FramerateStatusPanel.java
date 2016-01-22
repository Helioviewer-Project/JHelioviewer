package org.helioviewer.jhv.gui.statusLabels;

import java.awt.Dimension;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;

/**
 * Status panel for displaying the framerate for image series.
 * 
 * <p>
 * The information of this panel is always shown for the active layer.
 * 
 * <p>
 * This panel is not visible, if the active layer is not an image series.
 */
public class FramerateStatusPanel extends StatusLabel
{
	private static float lastFPS;
	private static boolean newFrameRendered;
	
	private int counter = 0;
	private long startMeasurement = 0;
	
	public FramerateStatusPanel()
	{
		super();
		setBorder(BorderFactory.createEtchedBorder());

		setPreferredSize(new Dimension(90, 20));
		setText("FPS: ");
		
		startMeasurement = System.currentTimeMillis();
	}
	
	public static float getFPS()
	{
		return lastFPS;
	}

	private void updateFramerate()
	{
		lastFPS = counter/((System.currentTimeMillis()-startMeasurement)/1000f);
		
		setVisible(true);
		setText(String.format("FPS: %.1f", lastFPS));
		counter = 0;
		startMeasurement = System.currentTimeMillis();
	}
	
	@Override
	public void isPlayingChanged(boolean _isPlaying)
	{
		counter = 0;
		startMeasurement = System.currentTimeMillis();
	}
	
	public static void notifyRenderingNewFrame()
	{
		newFrameRendered=true;
	}
	
	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
		if ((System.currentTimeMillis() - startMeasurement) >= 1000)
			updateFramerate();
		
		if(newFrameRendered)
			counter++;
		
		newFrameRendered=false;
	}
}
