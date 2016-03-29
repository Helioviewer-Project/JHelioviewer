package org.helioviewer.jhv.plugins.pfssplugin.data.caching;

import java.util.ArrayList;
import java.util.HashMap;

import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;

/**
 * Implementation of a simple First-in-First-out Cache. In this usecase this cache uses the LRU algorithm.
 * 
 * This class is not threadsafe
 */
class LRUCache<T extends Cacheable> {
	private final HashMap<FileDescriptor, Integer> cacheIndices;
	private final ArrayList<T> cache;
	private int oldestIndex;
	
	/**
	 * 
	 * @param size total size of the cache
	 */
	public LRUCache(int size) {
		oldestIndex = 0;
		cacheIndices = new HashMap<>(size,1);
		cache = new ArrayList<>(size);
		
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
		}
		
		//move index
		cacheIndices.put(key, oldestIndex);
		cache.set(oldestIndex, value);
		
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
