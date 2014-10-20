package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * This class is responsible for managing frames. it Tries to have all frames
 * pre-loaded and pre-initialized before they are requested
 * 
 * @author Jonas Schwammberger
 */
public class FrameManager {
	private final PfssDataCreator loader;
	private final PfssFrameCreator frameCreator;
	private final FileDescriptorManager manager;

	private final PfssFrameInitializer initializer;
	private final ConcurrentLinkedQueue<PfssFrame> destructionQueue = new ConcurrentLinkedQueue<>();

	private final PfssFrame[] preloadQueue;
	private int currentIndex = 0;
	private int lastIndex = 0;
	private int nextDateIndex;

	public FrameManager() {
		manager = new FileDescriptorManager();
		loader = new PfssDataCreator(manager);
		initializer = new PfssFrameInitializer();
		frameCreator = new PfssFrameCreator(initializer);
		preloadQueue = new PfssFrame[PfssSettings.PRELOAD];
	}

	public PfssFrame getFrame(Date date) {
		// still the same frame
		if (lastIndex == currentIndex)
			return null;
		
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

	private boolean isNext(Date d) {
		int nextIndex = (currentIndex+1) % preloadQueue.length;
		PfssFrame f = preloadQueue[nextIndex];
		return f.getDescriptor().isDateInRange(d);
	}

	private void unload(int index) {
		destructionQueue.add(preloadQueue[index]);
	}

	/**
	 * Loads the following Date into the free space designated by "index"
	 * 
	 * @param index
	 */
	private void loadFollowing(int index) {
		PfssData d = loader.getDataAsync(nextDateIndex);
		nextDateIndex = ++nextDateIndex % manager.getNumberOfFiles();
		preloadQueue[index] = frameCreator.createFrameAsync(d);
	}

	private void invalidatePreloaded() {
		for (int i = 0; i < preloadQueue.length; i++)
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

		// destroy
		PfssFrame f = null;
		while ((f = destructionQueue.poll()) != null)
			f.clear(gl);
	}

}
