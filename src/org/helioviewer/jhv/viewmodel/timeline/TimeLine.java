package org.helioviewer.jhv.viewmodel.timeline;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Timer;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.NewLayerListener;

public class TimeLine implements NewLayerListener{
	
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
		GuiState3DWCS.layers.addNewLayerListener(this);
		timeLineListeners = new CopyOnWriteArrayList<TimeLine.TimeLineListener>();
	}
	
	public void setFrameCount(int frameCount){
		this.frameCount = frameCount;
	}

	public boolean isPlaying() {
		return isPlaying;
	}

	public void setPlaying(boolean playing) {
		this.isPlaying = playing;
		if (isPlaying){
			timer.start();
		}
		else {
			timer.stop();
		}
	}
	
	private LocalDateTime nextFrame(){
		current = frames[frame++];
		frame = frame > frameCount ? 0 : frame;
		dateTimeChanged();
		return current;
	}
	
	@Deprecated
	public void setFrames(LocalDateTime[] frames){
		this.frames = frames;
		this.frame = 0;
		start = frames[0];
		current = start;
		this.frameCount = frames.length-1;
		end = frames[frameCount];
		
	}	
	
	@Deprecated
	public int getMaxFrames(){
		return frameCount;
	}
	
	@Deprecated
	public int getCurrentFrame(){
		return frame;
	}
	
	public LocalDateTime getCurrentDateTime(){
		return current;
	}
	
	public void addListener(TimeLineListener timeLineListener){
		timeLineListeners.add(timeLineListener);
	}
	
	public void removeListener(TimeLineListener timeLineListener){
		timeLineListeners.remove(timeLineListener);
	}
	
	private void dateTimeChanged(){
		for (TimeLine.TimeLineListener timeLineListener : timeLineListeners){
			timeLineListener.timeStampChanged(current);
		}
	}
	
	public interface TimeLineListener{
		void timeStampChanged(LocalDateTime localDateTime);
	}

	public void setSpeedFactor(int speedFactor) {
		this.speedFactor = 1000 / speedFactor;
		timer.setDelay(this.speedFactor);
	}

	@Override
	public void newlayerAdded() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newlayerRemoved(int idx) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void newtimestampChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		this.setFrames(layer.getLocalDateTime());
	}

	public void setCurrentFrame(int value) {
		this.frame = value;
		current = frames[frame];
		dateTimeChanged();
	}
}
