package org.helioviewer.jhv.plugins.pfssplugin.data.caching;

import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssFrame;
import org.helioviewer.jhv.plugins.pfssplugin.data.creators.PfssFrameCreator;
import org.helioviewer.jhv.plugins.pfssplugin.settings.PfssSettings;

/**
 * Responsible for caching and loading of PfssFrames. Each PfssFrame in this cache has no resources allocated outside of the JVM
 * @author Jonas Schwammberger
 *
 */
public class FrameCache {

	private final LRUCache<PfssFrame> cache;
	private final DataCache dataCache;
	private final PfssFrameCreator frameCreator;
	
	public FrameCache(DataCache dataCache) {
		cache =new LRUCache<>(PfssSettings.FRAME_CACHE);
		this.dataCache = dataCache;
		frameCreator = new PfssFrameCreator();
	}
	
	public PfssFrame get(FileDescriptor key) {
		if(cache.contains(key))
			return cache.get(key);
		else {
			PfssFrame f = load(key);
			cache.put(key, f);
			return f;
		}
	}
	
	private PfssFrame load(FileDescriptor key) {
		PfssData data = dataCache.get(key);
		return frameCreator.getFrameAsync(data);
	}
}
