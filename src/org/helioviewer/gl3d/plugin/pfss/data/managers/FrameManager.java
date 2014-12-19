package org.helioviewer.gl3d.plugin.pfss.data.managers;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.plugin.pfss.data.FileDescriptor;
import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.plugin.pfss.data.PfssFrame;
import org.helioviewer.gl3d.plugin.pfss.data.caching.DataCache;
import org.helioviewer.gl3d.plugin.pfss.data.caching.FrameCache;
import org.helioviewer.gl3d.plugin.pfss.data.creators.PfssDataCreator;
import org.helioviewer.gl3d.plugin.pfss.data.creators.PfssFrameCreator;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * This class is responsible for managing frames. it Tries to have all frames
 * pre-loaded and pre-initialized before they are requested
 * 
 * @author Jonas Schwammberger
 */
public class FrameManager {
	private final FileDescriptorManager descriptorManager;

	private final FrameCache frameCache;
	private final ConcurrentLinkedQueue<PfssFrame> destructionQueue = new ConcurrentLinkedQueue<>();
	private final ConcurrentLinkedQueue<PfssFrame> initQueue = new ConcurrentLinkedQueue<>();
	
	private final PfssFrame[] preloadQueue;
	private int currentIndex = 0;
	private int lastIndex = 0;

	public FrameManager() {
		descriptorManager = new FileDescriptorManager();
		preloadQueue = new PfssFrame[PfssSettings.FRAME_PRELOAD];
		DataCache dataCache = new DataCache(descriptorManager);
		frameCache = new FrameCache(dataCache);
	}

	/**
	 * Get Frame which represents the Date
	 * @param date
	 * @return Frame or null if there is no frame for the requested date
	 */
	public PfssFrame getFrame(Date date) {
		// not initialized
		if (lastIndex == currentIndex)
			return null;
		
		//outside of loaded frames
		if(!descriptorManager.isDateInRange(date))
			return null;
		
		//still the same frame
		//if (preloadQueue[currentIndex].getDescriptor().isDateInRange(date))
		if(true)
			return preloadQueue[currentIndex];

		// advance to next
		if (this.isNext(date)) {
			unload(currentIndex);
			currentIndex = ++currentIndex % preloadQueue.length;
			int secondToLast = lastIndex;
			lastIndex = ++lastIndex % preloadQueue.length;
			loadFollowing(lastIndex, secondToLast);
			
			//init next +1
			int nextIndex = (currentIndex+1) % preloadQueue.length;
			initQueue.add(preloadQueue[nextIndex]);

		} else {
			// user has skipped some frames
			this.invalidatePreloaded();
			this.initPreloaded(date);
		}

		return preloadQueue[currentIndex];
	}

	/**
	 * True if the requested Date is in Range of the next file in the preload queue
	 * @param d
	 * @return
	 */
	private boolean isNext(Date d) {
		int nextIndex = (currentIndex+1) % preloadQueue.length;
		PfssFrame f = preloadQueue[nextIndex];
		return f.getDescriptor().isDateInRange(d);
	}

	/**
	 * Queue an obsolete frame in for destruction
	 * @param index
	 */
	private void unload(int index) {
		destructionQueue.add(preloadQueue[index]);
	}

	/**
	 * Loads the following Date into the free space designated by "index"
	 * 
	 * @param destIndex destination index in the preloadQueue
	 * @param secondToLast index of the second to last PfssFrame
	 */
	private void loadFollowing(int destIndex, int secondToLast) {
		FileDescriptor second = preloadQueue[secondToLast].getDescriptor();
		FileDescriptor next = descriptorManager.getNext(second);
		preloadQueue[destIndex] = frameCache.get(next);
	}

	private void invalidatePreloaded() {
		for (int i = 0; i < preloadQueue.length; i++)
			unload(i);

		currentIndex = 0;
		lastIndex = 0;
	}

	/**
	 * Initializes the preloadQueue
	 * @param d
	 */
	private void initPreloaded(Date d) {
		int index  = descriptorManager.getFileIndex(d);
		index = index < 0 ? 0: index;
		
		if(descriptorManager.getNumberOfFiles() > 0)
		{
			currentIndex = 0;
			lastIndex = preloadQueue.length-1;
			FileDescriptor descriptor = descriptorManager.getFileDescriptor(index);
			
			for(int i = 0; i < preloadQueue.length;i++) {
				preloadQueue[i] = frameCache.get(descriptor);
				
				descriptor = descriptorManager.getNext(descriptor);
			}
		}
	}

	/**
	 * sets what date range the manager should handle 
	 * @param start start date inclusive
	 * @param end end date inclusive
	 * @throws IOException if the requested dates could not be found
	 */
	public void setDateRange(Date start, Date end) throws IOException {
		descriptorManager.readFileDescriptors(start,end);
		initPreloaded(start);
	}

	/**
	 * gives the manager a chance to initialize or destroy the frames before they are needed.
	 * @param gl OpenGL object
	 */
	public void preInitFrames(GL2 gl) {
		PfssFrame f = null;
		
		//init
		while((f = initQueue.poll()) != null)
			f.init(gl);

		// destroy
		while ((f = destructionQueue.poll()) != null)
			f.clear(gl);
	}

}
