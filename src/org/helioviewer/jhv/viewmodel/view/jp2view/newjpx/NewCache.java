package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.helioviewer.jhv.layers.CacheableImageData;

public class NewCache {

	public static NewCache singelton = new NewCache();
	
	private ConcurrentLinkedDeque<CacheableImageData> ramCache;
	
	private NewCache() {
		ramCache = new ConcurrentLinkedDeque<CacheableImageData>();
		File file;
				file = new File("/Users/binchu/Documents/FHNW/tmp");
				file.mkdir();
	}

	public void test(){
		System.out.println("test");
	}
	
	public void addCacheElement(CacheableImageData cacheableImageData){
		this.ramCache.add(cacheableImageData);
	}
	
	
	
	public static void main(String[] args) {
		NewCache.singelton.test();
	}

	public CacheableImageData getCacheElement(int id, LocalDateTime currentDate){
		for (CacheableImageData cacheableImageData : ramCache){
			if (cacheableImageData.contains(id, currentDate))
				return cacheableImageData;
		}
		return null;
		
	}
}
