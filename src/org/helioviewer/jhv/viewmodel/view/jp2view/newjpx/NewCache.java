package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.io.File;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.FutureTask;

public class NewCache {

	public static NewCache singelton = new NewCache();
	
	private ConcurrentLinkedDeque<FutureTask<JHVCachable>> ramCache;
	
	public NewCache() {
		ramCache = new ConcurrentLinkedDeque<FutureTask<JHVCachable>>();
		File file;
				file = new File("/Users/binchu/Documents/FHNW/tmp");
				file.mkdir();
	}

	public void test(){
		System.out.println("test");
	}
	
	public void addCacheElement(FutureTask<JHVCachable> cacheElement){
		this.ramCache.add(cacheElement);
	}
	
	
	
	public static void main(String[] args) {
		NewCache.singelton.test();
	}

	public FutureTask<JHVCachable> getCacheElement(LocalDateTime currentDate) {
		return ramCache.poll();
	}
}
