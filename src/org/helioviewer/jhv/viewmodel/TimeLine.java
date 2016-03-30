package org.helioviewer.jhv.viewmodel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.annotation.Nullable;
import javax.swing.Timer;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MoviePanel.AnimationMode;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;

public class TimeLine implements LayerListener
{
	private long currentTimeMS = 0;

	private boolean isPlaying = false;

	private ArrayList<TimeLineListener> timeLineListeners;

	private long startTimeMS = 0;
	private long endTimeMS = 0;
	private long cadenceMS=1;

	private double millisecondsPerFrame = 50;
	private AnimationMode animationMode = AnimationMode.LOOP;

	public static TimeLine SINGLETON = new TimeLine();
	private boolean forward = true;
	
	private long hurryUntil = 0;
	private long speedUntil = 0;
	
	public enum DecodeQualityLevel
	{
		HURRY,
		SPEED,
		PLAYBACK,
		QUALITY
	}
	
	public DecodeQualityLevel shouldHurry()
	{
		if(isPlaying && System.currentTimeMillis()<hurryUntil)
			return DecodeQualityLevel.HURRY;
		
		if(isPlaying && System.currentTimeMillis()<speedUntil)
			return DecodeQualityLevel.SPEED;
		
		if(isPlaying)
			return DecodeQualityLevel.PLAYBACK;
		
		return DecodeQualityLevel.QUALITY;
	}
	
	private long pendingChangeTime = 0;
	private long lastFrameChange = System.currentTimeMillis();
	private final Timer timer = new Timer(0, new ActionListener()
	{
		@Override
		public void actionPerformed(@Nullable ActionEvent e)
		{
			long now = System.currentTimeMillis();
			long elapsed = now-lastFrameChange;
			lastFrameChange = now;
			
			if(elapsed<=0)
				return;
			
			pendingChangeTime += elapsed;

			int elapsedFrames = (int)(pendingChangeTime / millisecondsPerFrame);
			if (elapsedFrames <= 0)
				return;
			
			pendingChangeTime -= elapsedFrames * millisecondsPerFrame;
			
			if(elapsedFrames>1)
			{
				if(speedUntil >= now)
				{
					hurryUntil=now+1000;
					speedUntil=now+2000;
				}
				else
					speedUntil=now+1000;
			}
			
			while (elapsedFrames > 0)
			{
				elapsedFrames--;
				switch (animationMode)
				{
					case LOOP:
						loop();
						break;
					case STOP:
						stop();
						break;
					case SWING:
						swing();
						break;
					default:
						break;
				}
			}
			dateTimeChanged(currentTimeMS);
		}
	});
	
	private TimeLine()
	{
		Layers.addLayerListener(this);
		timeLineListeners = new ArrayList<>();
	}

	public boolean isPlaying()
	{
		return isPlaying;
	}
	
	public long getCadenceMS()
	{
		return cadenceMS;
	}
	
	public boolean isThereAnythingToPlay()
	{
		return startTimeMS!=endTimeMS;
	}

	public void setPlaying(boolean _playing)
	{
		if(startTimeMS==endTimeMS && _playing)
		{
			System.out.println("TimeLine: There's nothing to play");
			_playing=false;
		}
		
		if(isPlaying==_playing)
			return;
		
		if(_playing)
		{
			lastFrameChange = System.currentTimeMillis();
			speedUntil = 0;
			timer.start();
		}
		else
			timer.stop();
		
		isPlaying = _playing;
		forward = true;
		
		for(TimeLineListener l:timeLineListeners)
			l.isPlayingChanged(isPlaying);
	}

	public long nextFrame()
	{
		long last = currentTimeMS;
		
		long next = currentTimeMS+cadenceMS;
		if (next>endTimeMS)
			next = startTimeMS;
		
		currentTimeMS = next;
		dateTimeChanged(last);
		return currentTimeMS;
	}

	public long previousFrame()
	{
		long last = currentTimeMS;
		
		long next = currentTimeMS-cadenceMS;
		if (next<startTimeMS)
			next = endTimeMS;
		
		currentTimeMS = next;
		dateTimeChanged(last);
		return currentTimeMS;
	}

	@Deprecated
	public int getFrameCount()
	{
		return (int)((endTimeMS-startTimeMS)/cadenceMS)+1;
	}
	
	@Deprecated
	public int getCurrentFrameIndex()
	{
		return (int)((currentTimeMS-startTimeMS)/cadenceMS);
	}

	public long getCurrentFrameStartTimeMS()
	{
		return currentTimeMS;
	}

	public long getCurrentFrameMiddleTimeMS()
	{
		return currentTimeMS+cadenceMS/2;
	}

	public long getCurrentFrameEndTimeMS()
	{
		return currentTimeMS+cadenceMS-1;
	}

	public void addListener(TimeLineListener timeLineListener)
	{
		timeLineListeners.add(timeLineListener);
	}

	public void removeListener(TimeLineListener timeLineListener)
	{
		timeLineListeners.remove(timeLineListener);
	}

	private void dateTimeChanged(long _previous)
	{
		ImageLayer.newRenderPassStarted();
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners)
			timeLineListener.timeStampChanged(currentTimeMS, _previous);
	}

	public void setFPS(int _fps)
	{
		millisecondsPerFrame = 1000d / _fps;
		timer.setDelay((int)Math.round(millisecondsPerFrame));
	}

	public double getMillisecondsPerFrame()
	{
		return millisecondsPerFrame;
	}

	@Override
	public void layerAdded()
	{
	}
	
	public void setTimeRange(long _startMS,long _endMS,long _cadenceMS)
	{
		if(_cadenceMS<1)
			throw new IllegalArgumentException("_cadence=="+_cadenceMS);
		
		startTimeMS=_startMS;
		endTimeMS=_endMS;
		cadenceMS=_cadenceMS;
		
		if(currentTimeMS<startTimeMS || currentTimeMS>endTimeMS)
			setCurrentTimeMS(startTimeMS);
		
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners)
			timeLineListener.timeRangeChanged();
	}
	
	public void setNoTimeRange()
	{
		endTimeMS=startTimeMS=0;
		setCurrentTimeMS(startTimeMS);
		cadenceMS=1;
		setPlaying(false);
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners)
			timeLineListener.timeRangeChanged();
	}

	@Override
	public void layersRemoved()
	{
		if(Layers.getActiveImageLayer()==null)
			setNoTimeRange();
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
		if (layer != null && layer instanceof ImageLayer)
			setTimeRange(((ImageLayer)layer).getStartTimeMS(), ((ImageLayer)layer).getEndTimeMS(), ((ImageLayer)layer).getCadenceMS());
	}

	public void setCurrentFrame(int _frameNr)
	{
		if(_frameNr<0)
			throw new IllegalArgumentException("_frameNr must be >=0");
		
		long newCurrent = startTimeMS + cadenceMS * _frameNr;
		if(newCurrent>endTimeMS)
			throw new IllegalArgumentException("_frameNr too big");
		
		if (newCurrent!=currentTimeMS)
		{
			long last = currentTimeMS;
			currentTimeMS = newCurrent;
			dateTimeChanged(last);
		}
		MainFrame.SINGLETON.MAIN_PANEL.repaint();
	}

	public interface TimeLineListener
	{
		void isPlayingChanged(boolean _isPlaying);
		void timeStampChanged(long _current, long _previous);
		void timeRangeChanged();
	}

	public long getFirstTimeMS()
	{
		return startTimeMS;
	}

	public long getLastTimeMS()
	{
		return endTimeMS;
	}

	public void setCurrentTimeMS(long _newTimeMS)
	{
		long previous = currentTimeMS;
		currentTimeMS = _newTimeMS;
		dateTimeChanged(previous);
	}

	public void setAnimationMode(AnimationMode _animationMode)
	{
		animationMode = _animationMode;
	}
	
	private void loop()
	{
		long next=currentTimeMS+cadenceMS;
		if(next>endTimeMS)
			currentTimeMS = startTimeMS;
		else
			currentTimeMS = next;
	}
	
	private void stop()
	{
		long next=currentTimeMS+cadenceMS;
		if (next>endTimeMS)
		{
			currentTimeMS = startTimeMS;
			setPlaying(false);
		}
		else
			currentTimeMS = next;
	}
	
	private void swing()
	{
		long next;
		if (forward)
		{
			next = currentTimeMS+cadenceMS;
			if (next>endTimeMS)
			{
				forward = false;
				next = currentTimeMS;
			}
		}
		else
		{
			next = currentTimeMS-cadenceMS;
			if (next<startTimeMS)
			{
				forward = true;
				next = currentTimeMS;
			}
		}
		currentTimeMS = next;
	}
}
