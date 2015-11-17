package org.helioviewer.jhv.viewmodel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeSet;

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

	private NavigableSet<LocalDateTime> localDateTimes;

	private double millisecondsPerFrame = 50;
	private AnimationMode animationMode = AnimationMode.LOOP;

	public static TimeLine SINGLETON = new TimeLine();
	private boolean forward = true;
	
	private static final Timer timer = new Timer(0, new ActionListener()
	{
		@Override
		public void actionPerformed(@Nullable ActionEvent e)
		{
			MainFrame.SINGLETON.MAIN_PANEL.display();
			timer.stop();
		}
	});
	
	private TimeLine()
	{
		localDateTimes = new TreeSet<>();
		Layers.addLayerListener(this);
		timeLineListeners = new ArrayList<>();
	}

	public boolean isPlaying()
	{
		return isPlaying;
	}

	public void setPlaying(boolean _playing)
	{
		if(localDateTimes.isEmpty() && _playing)
		{
			System.out.println("TimeLine: There's nothing to play");
			_playing=false;
		}
		
		if(isPlaying==_playing)
			return;
		
		isPlaying = _playing;
		forward = true;
		
		for(TimeLineListener l:timeLineListeners)
			l.isPlayingChanged(isPlaying);
	}

	public @Nullable LocalDateTime nextFrame()
	{
		if (localDateTimes.isEmpty())
			return null;
		
		LocalDateTime next = localDateTimes.higher(current);
		LocalDateTime last = current;
		if (next == null)
			next = localDateTimes.first();
		
		current = next;
		dateTimeChanged(last);
		return current;
	}

	public @Nullable LocalDateTime previousFrame()
	{
		if (localDateTimes.isEmpty())
			return null;
		LocalDateTime next = localDateTimes.lower(current);
		if (next == null)
			next = localDateTimes.last();
		
		current = next;
		dateTimeChanged(current);
		return current;
	}

	public int getFrameCount()
	{
		return localDateTimes.size();
	}

	public int getCurrentFrameIndex()
	{
		return localDateTimes.headSet(current).size();
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
	}

	public double getMillisecondsPerFrame()
	{
		return millisecondsPerFrame;
	}

	@Override
	public void layerAdded()
	{
	}

	@Override
	public void layersRemoved()
	{
		if (Layers.getActiveImageLayer() == null)
			setLocalDateTimes(new TreeSet<LocalDateTime>());
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
		if (layer != null && layer instanceof ImageLayer)
			setLocalDateTimes(((ImageLayer)layer).getLocalDateTimes());
	}

	public void setCurrentFrame(int _frameNr)
	{
		Iterator<LocalDateTime> it = localDateTimes.iterator();
		int i = 0;
		LocalDateTime newCurrent = null;
		while (it.hasNext() && i <= _frameNr)
		{
			newCurrent = it.next();
			i++;
		}
		if (newCurrent != null && !newCurrent.isEqual(current))
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

		@Deprecated
		void dateTimesChanged(int framecount);
	}

	public void setLocalDateTimes(NavigableSet<LocalDateTime> _localDateTimes)
	{
		localDateTimes = _localDateTimes;
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners)
			timeLineListener.dateTimesChanged(localDateTimes.size());

		/*if(_localDateTimes.isEmpty())
			setCurrentDate(LocalDateTime.now());
		else
			setCurrentDate(current);*/
		
		if(localDateTimes.isEmpty())
			setPlaying(false);
	}

	public @Nullable LocalDateTime getFirstDateTime()
	{
		if (localDateTimes.isEmpty())
			return null;
		return localDateTimes.first();
	}

	public @Nullable LocalDateTime getLastDateTime()
	{
		if (localDateTimes == null || localDateTimes.isEmpty())
			return null;
		return localDateTimes.last();
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
	
	/**
	 * 
	 * 
	 * @param _elapsedMilliseconds
	 * @return Returns true iff the current frame changed
	 */
	public boolean processElapsedAnimationTime(long _elapsedMilliseconds)
	{
		if (_elapsedMilliseconds <= 0)
			return true;
		
		int elapsedFrames = (int)(_elapsedMilliseconds / millisecondsPerFrame);
		if (elapsedFrames <= 0)
		{
			timer.stop();
			timer.setDelay((int) (millisecondsPerFrame - _elapsedMilliseconds));
			timer.start();
			return false;
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
			if (!isPlaying)
			{
				dateTimeChanged(current);
				return true;
			}
		}
		dateTimeChanged(current);
		return true;
	}
	
	private void loop()
	{
		LocalDateTime next=localDateTimes.higher(current);
		
		if(next==null)
			current = localDateTimes.first();
		else
			current = next;
	}
	
	private void stop()
	{
		LocalDateTime next=localDateTimes.lower(current);
		if (next == null)
		{
			current = localDateTimes.first();		
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
			next = localDateTimes.higher(current);
			if (next == null)
			{
				forward = false;
				next = localDateTimes.lower(current);
			}
		}
		else
		{
			next = localDateTimes.lower(current);
			if (next == null)
			{
				forward = true;
				next = localDateTimes.higher(current);
			}
		}
		current = next;
	}
}
