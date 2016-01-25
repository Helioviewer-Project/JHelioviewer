package org.helioviewer.jhv.viewmodel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

import javax.annotation.Nonnull;
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
	private LocalDateTime current = LocalDateTime.now();

	private boolean isPlaying = false;

	private ArrayList<TimeLineListener> timeLineListeners;

	private LocalDateTime startTime = LocalDateTime.now();
	private LocalDateTime endTime = LocalDateTime.now();
	private int cadence=1;

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
			//skippingFrames=false;
			
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
			dateTimeChanged(current);
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
	
	public boolean isThereAnythingToPlay()
	{
		return !startTime.isEqual(endTime);
	}

	public void setPlaying(boolean _playing)
	{
		if(startTime.isEqual(endTime) && _playing)
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

	public LocalDateTime nextFrame()
	{
		LocalDateTime last = current;
		
		LocalDateTime next = current.plusSeconds(cadence);
		if (next.isAfter(endTime))
			next = startTime;
		
		current = next;
		dateTimeChanged(last);
		return current;
	}

	public LocalDateTime previousFrame()
	{
		LocalDateTime last = current;
		
		LocalDateTime next = current.minusSeconds(cadence);
		if (next.isBefore(startTime))
			next = endTime;
		
		current = next;
		dateTimeChanged(last);
		return current;
	}

	@Deprecated
	public int getFrameCount()
	{
		return (int)(startTime.until(endTime, ChronoUnit.SECONDS)/cadence);
	}
	
	@Deprecated
	public int getCurrentFrameIndex()
	{
		return (int)(startTime.until(current, ChronoUnit.SECONDS)/cadence);
	}

	public LocalDateTime getCurrentDateTime()
	{
		return current;
	}

	public void addListener(TimeLineListener timeLineListener)
	{
		timeLineListeners.add(timeLineListener);
	}

	public void removeListener(TimeLineListener timeLineListener)
	{
		timeLineListeners.remove(timeLineListener);
	}

	private void dateTimeChanged(LocalDateTime _previous)
	{
		ImageLayer.newRenderPassStarted();
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners)
			timeLineListener.timeStampChanged(current, _previous);
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
	
	public void setTimeRange(@Nonnull LocalDateTime _start,@Nonnull LocalDateTime _end,int _cadence)
	{
		if(_cadence<=1)
			throw new IllegalArgumentException("_cadence");
		
		startTime=_start;
		endTime=_end;
		cadence=_cadence;
		
		if(current.isBefore(startTime) || current.isAfter(endTime))
			setCurrentDate(startTime);
		
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners)
			timeLineListener.timeRangeChanged(_start, _end);
	}
	
	public void setNoTimeRange()
	{
		startTime=endTime=LocalDateTime.now();
		setCurrentDate(startTime);
		cadence=1;
		setPlaying(false);
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners)
			timeLineListener.timeRangeChanged(startTime, endTime);
	}

	@Override
	public void layersRemoved()
	{
		if (Layers.getActiveImageLayer() == null)
			setNoTimeRange();
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
		if (layer != null && layer instanceof ImageLayer)
			setTimeRange(((ImageLayer)layer).getFirstLocalDateTime(), ((ImageLayer)layer).getLastLocalDateTime(), ((ImageLayer)layer).getCadence());
	}

	public void setCurrentFrame(int _frameNr)
	{
		if(_frameNr<0)
			throw new IllegalArgumentException("_frameNr must be >=0");
		
		LocalDateTime newCurrent = startTime.plusSeconds(cadence * _frameNr);
		if(newCurrent.isAfter(endTime))
			throw new IllegalArgumentException("_frameNr too big");
		
		if (!newCurrent.isEqual(current))
		{
			LocalDateTime last = current;
			current = newCurrent;
			dateTimeChanged(last);
		}
		MainFrame.SINGLETON.MAIN_PANEL.repaint();
	}

	public interface TimeLineListener
	{
		void isPlayingChanged(boolean _isPlaying);
		void timeStampChanged(LocalDateTime current, LocalDateTime last);
		void timeRangeChanged(LocalDateTime _start, LocalDateTime _end);
	}

	public LocalDateTime getFirstDateTime()
	{
		return startTime;
	}

	public LocalDateTime getLastDateTime()
	{
		return endTime;
	}

	public void setCurrentDate(LocalDateTime _newDateTime)
	{
		LocalDateTime previous = current;
		current = _newDateTime;
		dateTimeChanged(previous);
	}

	public void setAnimationMode(AnimationMode _animationMode)
	{
		animationMode = _animationMode;
	}
	
	private void loop()
	{
		LocalDateTime next=current.plusSeconds(cadence);
		if(next.isAfter(endTime))
			current = startTime;
		else
			current = next;
	}
	
	private void stop()
	{
		LocalDateTime next=current.plusSeconds(cadence);
		if (next.isAfter(endTime))
		{
			current = startTime;		
			setPlaying(false);
		}
		else
			current = next;
	}
	
	private void swing()
	{
		LocalDateTime next;
		if (forward)
		{
			next = current.plusSeconds(cadence);
			if (next.isAfter(endTime))
			{
				forward = false;
				next = current;
			}
		}
		else
		{
			next = current.minusSeconds(cadence);
			if (next.isBefore(startTime))
			{
				forward = true;
				next = current;
			}
		}
		current = next;
	}
}
