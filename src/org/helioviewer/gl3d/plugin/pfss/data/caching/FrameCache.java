package org.helioviewer.gl3d.plugin.pfss.data.caching;

import org.helioviewer.gl3d.plugin.pfss.data.FileDescriptor;
import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.plugin.pfss.data.PfssFrame;
import org.helioviewer.gl3d.plugin.pfss.data.creators.PfssFrameCreator;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * Responsible for caching and loading of PFSSFrames
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
			System.out.println("FRAME MISS: "+ key.getStartDate().toString());
			cache.put(key, f);
			return f;
		}
	}
	
	private PfssFrame load(FileDescriptor key) {
		PfssData data = dataCache.get(key);
		return frameCreator.createFrameAsync(data);
	}
}
