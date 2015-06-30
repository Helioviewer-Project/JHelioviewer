package org.helioviewer.jhv.viewmodel.timeline;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Timer;

import org.helioviewer.jhv.JHVException;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;

public class TimeLine implements LayerListener {

	private LocalDateTime current = LocalDateTime.now();

	private boolean isPlaying = false;

	private CopyOnWriteArrayList<TimeLineListener> timeLineListeners;

	private TreeSet<LocalDateTime> localDateTimes = null;
	
	private int speedFactor;

	public static TimeLine SINGLETON = new TimeLine();

	private Timer timer = new Timer(20, new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			nextFrame();

		}
	});

	private TimeLine() {
		localDateTimes = new TreeSet<LocalDateTime>();
		Layers.addNewLayerListener(this);
		timeLineListeners = new CopyOnWriteArrayList<TimeLine.TimeLineListener>();
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
		if (localDateTimes.isEmpty()) return null;
		LocalDateTime next = localDateTimes.higher(current.plusNanos(1));
		LocalDateTime last = current;
		if (next == null){
			next = localDateTimes.first();
		}
		current = next;
		dateTimeChanged(last);
		return current;
	}
	
	public LocalDateTime previousFrame(){
		if (localDateTimes.isEmpty()) return null;
		LocalDateTime next = localDateTimes.lower(current.minusNanos(1));
		if (next == null){
			next = localDateTimes.last();
		}
		current = next;
		dateTimeChanged(current);
		return current;
	}

	@Deprecated
	public int getMaxFrames() {
		return localDateTimes.size();
	}

	@Deprecated
	public int getCurrentFrame() {
		return localDateTimes.headSet(current).size();
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
			timeLineListener.dateTimesChanged(localDateTimes.size());
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
		updateLocalDateTimes(layer.getLocalDateTime());
	}

	public void setCurrentFrame(int value) {
		Iterator<LocalDateTime> it = localDateTimes.iterator();
		int i = 0;
		LocalDateTime current = null;
		while(it.hasNext() && i <= value){
			current = it.next();
			i++;
		}
		if (current != null && !current.isEqual(this.current)) {			
			LocalDateTime last = this.current;
			System.out.println("current");
			this.current = current;
			dateTimeChanged(last);
		}
	}

	public interface TimeLineListener {
		void timeStampChanged(LocalDateTime current, LocalDateTime last);

		@Deprecated
		void dateTimesChanged(int framecount);
	}
	
	public void updateLocalDateTimes(TreeSet<LocalDateTime> localDateTimes){
		this.localDateTimes = localDateTimes;
		notifyUpdateDateTimes();
	}

	public LocalDateTime getFirstDateTime() throws JHVException.TimeLineException {
		if (localDateTimes.size() <= 0) throw new JHVException.TimeLineException("No dates are loaded");
		if (localDateTimes.first() == null) throw new JHVException.TimeLineException("no first date is available");
		return localDateTimes.first();
	}

	public LocalDateTime getLastDateTime() throws JHVException.TimeLineException {
		if (localDateTimes.size() <= 0) throw new JHVException.TimeLineException("No dates are loaded");
		if (localDateTimes.last() == null) throw new JHVException.TimeLineException("no first date is available");
		return localDateTimes.last();
	}
}
