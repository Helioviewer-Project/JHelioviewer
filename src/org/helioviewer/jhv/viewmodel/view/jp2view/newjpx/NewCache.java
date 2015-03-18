package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.concurrent.ConcurrentLinkedDeque;

public class NewCache {

	public static NewCache singelton = new NewCache();
	
	private ConcurrentLinkedDeque<JHVCachable> ramCache;
	
	public NewCache() {
		ramCache = new ConcurrentLinkedDeque<JHVCachable>();
		File file;
				file = new File("/Users/binchu/Documents/FHNW/tmp");
				file.mkdir();
	}

	public void test(){
		System.out.println("test");
	}
	
	public void addCacheElement(JHVCachable cacheElement){
		this.ramCache.add(cacheElement);
	}
	
	
	
	public static void main(String[] args) {
		NewCache.singelton.test();
	}
}
