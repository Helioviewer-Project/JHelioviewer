package org.helioviewer.jhv.viewmodel.timeline;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Timer;

import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;

public class TimeLine implements LayerListener {

	private LocalDateTime start;
	private LocalDateTime end;
	private LocalDateTime current;

	private int frameCount;
	private boolean isPlaying = false;

	private CopyOnWriteArrayList<TimeLineListener> timeLineListeners;

	@Deprecated
	private LocalDateTime[] frames;
	@Deprecated
	private int frame = 0;

	private int speedFactor;

	public static TimeLine SINGLETON = new TimeLine();

	private Timer timer = new Timer(20, new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			nextFrame();

		}
	});

	private TimeLine() {
		Layers.addNewLayerListener(this);
		timeLineListeners = new CopyOnWriteArrayList<TimeLine.TimeLineListener>();
	}

	public void setFrameCount(int frameCount) {
		this.frameCount = frameCount;
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public void setPlaying(boolean playing) {
		this.isPlaying = playing;
		if (isPlaying) {
			timer.start();
		} else {
			timer.stop();
		}
	}

	public LocalDateTime nextFrame() {
		LocalDateTime last = current;
		current = frames[frame++];
		frame = frame > frameCount ? 0 : frame;
		dateTimeChanged(last);
		return current;
	}
	
	public LocalDateTime previousFrame(){
		LocalDateTime last = current;
		current = frames[frame--];
		frame = frame < 0 ? frameCount : frame;
		dateTimeChanged(last);
		return current;
	}

	@Deprecated
	public void setFrames(LocalDateTime[] frames) {
		this.frames = frames;
		this.frame = 0;
		if (frames != null && frames.length > 0) {
			start = frames[0];
			current = start;
			this.frameCount = frames.length - 1;
			end = frames[frameCount];
			this.notifyUpdateDateTimes();
		}

	}

	@Deprecated
	public int getMaxFrames() {
		return frameCount;
	}

	@Deprecated
	public int getCurrentFrame() {
		return frame;
	}

	public LocalDateTime getCurrentDateTime() {
		return current;
	}

	public void addListener(TimeLineListener timeLineListener) {
		timeLineListeners.add(timeLineListener);
	}

	public void removeListener(TimeLineListener timeLineListener) {
		timeLineListeners.remove(timeLineListener);
	}

	private void dateTimeChanged(LocalDateTime last) {
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners) {
			timeLineListener.timeStampChanged(current, last);
		}
	}

	private void notifyUpdateDateTimes() {
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners) {
			timeLineListener.dateTimesChanged(frameCount);
		}
	}

	public void setSpeedFactor(int speedFactor) {
		this.speedFactor = 1000 / speedFactor;
		timer.setDelay(this.speedFactor);
	}
	
	public int getSpeedFactor(){
		return timer.getDelay();
	}

	@Override
	public void newlayerAdded() {
		// TODO Auto-generated method stub

	}

	@Override
	public void newlayerRemoved(int idx) {
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		this.setFrames(layer.getLocalDateTime());
	}

	public void setCurrentFrame(int value) {
		if (value != frame) {
			this.frame = value;
			LocalDateTime last = current;
			current = frames[frame];
			dateTimeChanged(last);
		}
	}

	public interface TimeLineListener {
		void timeStampChanged(LocalDateTime current, LocalDateTime last);

		@Deprecated
		void dateTimesChanged(int framecount);
	}

	@Deprecated
	public void updateLocalDateTimes(LocalDateTime[] localDateTimes) {
		this.setFrames(localDateTimes);
	}

	public LocalDateTime getFirstDateTime() {
		if (frames == null) return null;
		return frames[0];
	}

	public LocalDateTime getLastDateTime() {
		if (frames == null) return null;
		return frames[frames.length - 1];
	}
}
