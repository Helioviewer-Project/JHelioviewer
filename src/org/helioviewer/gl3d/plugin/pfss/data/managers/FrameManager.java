package org.helioviewer.gl3d.plugin.pfss.data.managers;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.plugin.pfss.data.FileDescriptor;
import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.plugin.pfss.data.PfssFrame;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * This class is responsible for managing frames. it Tries to have all frames
 * pre-loaded and pre-initialized before they are requested
 * 
 * @author Jonas Schwammberger
 */
public class FrameManager {
	private final PfssDataCreator dataCreator;
	private final PfssFrameCreator frameCreator;
	private final FileDescriptorManager descriptorManager;

	private final PfssFrameInitializer initializer;
	private final ConcurrentLinkedQueue<PfssFrame> destructionQueue = new ConcurrentLinkedQueue<>();

	private final PfssFrame[] preloadQueue;
	private int currentIndex = 0;
	private int lastIndex = 0;
	private int nextFileIndex;

	public FrameManager() {
		descriptorManager = new FileDescriptorManager();
		dataCreator = new PfssDataCreator();
		initializer = new PfssFrameInitializer();
		frameCreator = new PfssFrameCreator(initializer);
		preloadQueue = new PfssFrame[PfssSettings.PRELOAD];
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
		if (preloadQueue[currentIndex].getDescriptor().isDateInRange(date))
			return preloadQueue[currentIndex];

		// advance to next
		if (this.isNext(date)) {
			unload(currentIndex);
			currentIndex = ++currentIndex % preloadQueue.length;
			lastIndex = ++lastIndex % preloadQueue.length;
			loadFollowing(lastIndex);

			// user has skipped some frames
		} else {
			// Improvement:check if it has to invalidate the whole preloaded
			// queue
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
	 * @param index
	 */
	private void loadFollowing(int index) {
		FileDescriptor descriptor = descriptorManager.getFileDescriptor(nextFileIndex);
		PfssData d = dataCreator.getDataAsync(descriptor);
		nextFileIndex = ++nextFileIndex % descriptorManager.getNumberOfFiles();
		preloadQueue[index] = frameCreator.createFrameAsync(d);
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
		nextFileIndex = descriptorManager.getFileIndex(d);
		nextFileIndex = nextFileIndex < 0 ? 0: nextFileIndex;
		
		if(descriptorManager.getNumberOfFiles() > 0)
		{
			currentIndex = 0;
			lastIndex = preloadQueue.length-1;
			
			for(int i = 0; i < preloadQueue.length;i++) {
				FileDescriptor descriptor = descriptorManager.getFileDescriptor(nextFileIndex);
				PfssData data = dataCreator.getDataAsync(descriptor);
				preloadQueue[i] = frameCreator.createFrameAsync(data);
				
				nextFileIndex = ++nextFileIndex % descriptorManager.getNumberOfFiles();
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
		initializer.init(gl);

		// destroy
		PfssFrame f = null;
		while ((f = destructionQueue.poll()) != null)
			f.clear(gl);
	}

}
