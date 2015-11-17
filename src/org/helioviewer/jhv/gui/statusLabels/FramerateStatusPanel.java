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
	private static int lastCounter;
	private static boolean newFrameRendered;
	
	private int counter = 0;
	private long startMeasurement = 0;
	
	public FramerateStatusPanel()
	{
		super();
		setBorder(BorderFactory.createEtchedBorder());

		setPreferredSize(new Dimension(70, 20));
		setText("FPS: ");
		
		startMeasurement = System.currentTimeMillis();
	}
	
	public static int getFPS()
	{
		return lastCounter;
	}

	private void updateFramerate()
	{
		lastCounter = counter;
		
		setVisible(true);
		setText("FPS: " + counter);
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
