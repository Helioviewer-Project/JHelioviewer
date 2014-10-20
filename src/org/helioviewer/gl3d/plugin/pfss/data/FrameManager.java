package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.Date;

import javax.media.opengl.GL2;

/**
 * This class is responsible for managing frames. it Tries to have all frames pre-loaded and pre-initialized before they are requested
 * 
 * @author Jonas Schwammberger
 */
public class FrameManager {
	private final PfssDataCreator loader;
	private final PfssFrameCreator frameCreator;
	private final PfssFrameInitializer initializer;
	private final DateRangeManager manager;
	
	private PfssFrame[] preloadedqueue;
	private int currentIndex;
	private int lastIndex;
	private int nextDateIndex;
	
	public FrameManager() {
		manager = new DateRangeManager();
		loader = new PfssDataCreator(manager);
		initializer = new PfssFrameInitializer();
		frameCreator = new PfssFrameCreator(initializer);
	}
	
	public PfssFrame getFrame(Date date) {
		//still the same frame
		if(preloadedqueue[currentIndex].getDateRange().isInRange(date))
			return preloadedqueue[currentIndex];
		
		//advance to next
		if(this.isNext(date))
		{
			unload(currentIndex);
			currentIndex++; currentIndex %= preloadedqueue.length;
			loadFollowing(lastIndex);
			lastIndex++; lastIndex %= preloadedqueue.length;
			
	    //user has skipped some frames
		} else {
			//Improvement:check if it has to invalidate the whole preloaded queue
			this.invalidatePreloaded();
			this.initPreloaded(date);
		}
		
		return preloadedqueue[currentIndex];
	}
	
	private boolean isNext(Date d) {
		return false;
	}
	
	private void unload(int index) {
		//handle destruction
	}
	
	private void loadFollowing(int index) {
		int destinationIndex = (index+1) % preloadedqueue.length;
		PfssData d = loader.getDataAsync(nextDateIndex);
		preloadedqueue[destinationIndex] = frameCreator.createFrameAsync(d);
	}
	
	private void invalidatePreloaded() {
		for(int i = 0; i < preloadedqueue.length;i++)
			unload(i);
		
		currentIndex = 0;
		lastIndex = 0;
	}
	
	private void initPreloaded(Date d) {
		
	}
	
	public void setDateRange(Date start, Date end) {
		
	}
	
	public void preInitFrames(GL2 gl) {
		initializer.init(gl);
	}
	
}
