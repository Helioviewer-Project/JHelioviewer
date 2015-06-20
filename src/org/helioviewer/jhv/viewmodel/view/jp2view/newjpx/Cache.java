package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedDeque;

import kdu_jni.KduException;

import org.helioviewer.jhv.layers.CacheableImageData;

public class Cache {
	private static final int MAX_RAM_CACHE = 5000 * 1000000;
	private static ConcurrentLinkedDeque<CacheableImageData> ramCache;
	private static int cacheSize = 0;
	
	static {
		ramCache = new ConcurrentLinkedDeque<CacheableImageData>();
	}
	
	public static void addCacheElement(CacheableImageData cacheableImageData){
		ramCache.add(cacheableImageData);
	}	

	public static CacheableImageData getCacheElement(int id, LocalDateTime currentDate){
		for (CacheableImageData cacheableImageData : ramCache){
			if (cacheableImageData.contains(id, currentDate)){
				return cacheableImageData;

			}
		}
		return null;
		
	}
	
	public static void checkCacheSize(){
		long size = 0;
		for (CacheableImageData cacheableImageData : ramCache){
			try {
				size += cacheableImageData.getImageData().Get_peak_cache_memory();
			} catch (KduException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		while (size > MAX_RAM_CACHE){
			CacheableImageData cacheableImageData = ramCache.poll();
			try {
				size -= cacheableImageData.getImageData().Get_peak_cache_memory();
			} catch (KduException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
