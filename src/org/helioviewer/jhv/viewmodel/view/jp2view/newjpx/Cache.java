package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedDeque;

import kdu_jni.KduException;

import org.helioviewer.jhv.layers.CacheableImageData;

public class Cache {
	private static final int MAX_RAM_CACHE = 4 * 1000000;
	private static ConcurrentLinkedDeque<CacheableImageData> ramCache;
	private static int cacheSize = 0;

	static {
		ramCache = new ConcurrentLinkedDeque<CacheableImageData>();
	}

	public static void addCacheElement(CacheableImageData cacheableImageData) {
		ramCache.add(cacheableImageData);
	}

	public static CacheableImageData getCacheElement(int id,
			LocalDateTime currentDate) {
		for (CacheableImageData cacheableImageData : ramCache) {
			if (cacheableImageData.contains(id, currentDate)) {
				return cacheableImageData;

			}
		}
		return null;

	}

	public static boolean checkSize() {
		long size = 0;
		for (CacheableImageData cacheableImageData : ramCache){
			if (cacheableImageData.getImageFile() == null){
			try {
				size += cacheableImageData.getImageData().Get_peak_cache_memory();
			} catch (KduException e) {
				System.out.println("no cache is avaible");
			}
			}
		}
		System.out.println("size : " + size);
		System.out.println("size : " + (size > MAX_RAM_CACHE));
		// TODO Auto-generated method stub
		return size > MAX_RAM_CACHE;
	}	
}
