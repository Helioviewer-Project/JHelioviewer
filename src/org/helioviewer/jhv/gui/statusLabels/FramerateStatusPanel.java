package org.helioviewer.jhv.gui.statusLabels;

import java.awt.Dimension;
import java.time.LocalDateTime;

import javax.swing.BorderFactory;

import org.helioviewer.jhv.viewmodel.TimeLine;

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
		
		updateFramerate();
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
	public void timeStampChanged(long current, long last)
	{
		if(!TimeLine.SINGLETON.isPlaying())
			return;
		
		if ((System.currentTimeMillis() - startMeasurement) >= 1000)
			updateFramerate();
		
		if(newFrameRendered)
			counter++;
		
		newFrameRendered=false;
	}
}
