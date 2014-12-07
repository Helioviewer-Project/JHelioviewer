package org.helioviewer.gl3d.plugin.pfss.data.caching;

import java.util.ArrayList;
import java.util.HashMap;

import org.helioviewer.gl3d.plugin.pfss.data.FileDescriptor;

/**
 * Implementation of a simple First-in-First-out Cache for PFSSData and PFSSFrame
 * 
 * This class is not threadsafe
 * @author Jonas Schwammberger
 *
 * @param <T>
 */
public class FiFoCache<T extends Cacheable> {
	private final HashMap<FileDescriptor, Integer> cacheIndices;
	private final ArrayList<T> cache;
	private int oldestIndex;
	
	/**
	 * 
	 * @param size total size of the cache
	 */
	public FiFoCache(int size) {
		oldestIndex = 0;
		cacheIndices = new HashMap<>(size,1);
		cache = new ArrayList<T>(size);
		
		//init cache
		for(int i = 0; i < size;i++) {
			cache.add(null);
		}
	}
	
	public void put(FileDescriptor key, T value) {
		T oldest = cache.get(oldestIndex);
		if(oldest != null) {
			//unload
			FileDescriptor d = oldest.getDescriptor();
			cacheIndices.remove(d);
			
			//load
			cacheIndices.put(key, oldestIndex);
			cache.set(oldestIndex, value);
		}
		
		//move index
		oldestIndex = ++oldestIndex % cache.size();
	}
	
	public T get(FileDescriptor key) {
		Integer index = cacheIndices.get(key);
		return cache.get(index);
	}
	
	public boolean contains(FileDescriptor key) {
		return this.cacheIndices.containsKey(key);
	}
	
	public int size() {
		return cache.size();
	}
}
