package org.helioviewer.jhv.plugins.pfssplugin.data.caching;

import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.data.creators.PfssDataCreator;
import org.helioviewer.jhv.plugins.pfssplugin.data.managers.FileDescriptorManager;
import org.helioviewer.jhv.plugins.pfssplugin.settings.PfssSettings;

/**
 * Represents the DataCache responsible for caching, loading and preloading PfssData 
 * objects
 * @author Jonas Schwammberger
 *
 */
public class DataCache {
	private final FileDescriptorManager descriptorManager;
	private final PfssDataCreator dataCreator;
	
	private final LRUCache<PfssData> readAheadCache;
	private final LRUCache<PfssData> cache;
	
	
	public DataCache(FileDescriptorManager descriptors) {
		this.descriptorManager = descriptors;
		
		this.dataCreator = new PfssDataCreator();
		this.cache = new LRUCache<>(PfssSettings.DATA_CACHE_SIZE);
		this.readAheadCache = new LRUCache<>(PfssSettings.DATA_PRELOAD_SIZE);
	}
	
	public PfssData get(FileDescriptor d) {
		PfssData out = null;
		if(cache.contains(d))
		{
			out = cache.get(d);
		}
		if(readAheadCache.contains(d))
		{
			PfssData data = readAheadCache.get(d);
			cache.put(d, data);
			readAhead(d);
			out = data;
			
		} else {
			//cache miss
			PfssData data = dataCreator.getDataAsync(d);
			cache.put(d, data);
			readAhead(d);
			out = data;
		}
		
		return out;
	}
	
	/**
	 * Fills the read ahead cache with PfssData objects which are very likely to be needed next
	 * @param d
	 */
	private void readAhead(FileDescriptor d) {
		FileDescriptor next = descriptorManager.getNext(d);
		
		for(int i = 0; i < readAheadCache.size();i++)
		{
			if(!readAheadCache.contains(next))
			{
				PfssData read = null;
				if(cache.contains(next)) {
					read = cache.get(next);
				} else {
					read = dataCreator.getDataAsync(next);
				}

				readAheadCache.put(next, read);
			}
		}
	}
	
	
}
