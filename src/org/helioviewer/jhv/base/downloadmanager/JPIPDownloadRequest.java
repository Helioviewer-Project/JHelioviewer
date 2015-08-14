package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.helioviewer.jhv.Directories;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.CacheableImageData;

public class JPIPDownloadRequest extends HTTPRequest {

	private static final String CACHE_PATH = Directories.CACHE.getPath();

	private final CacheableImageData cacheableImageData;
	private final ArrayList<AbstractRequest> requests;
	private final HTTPRequest httpRequest;
	public JPIPDownloadRequest(String url, PRIORITY priority,
			CacheableImageData cacheableImageData,
			ArrayList<AbstractRequest> requests, HTTPRequest httpRequest) {
		super(url, priority, 60000, 3);
		this.cacheableImageData = cacheableImageData;
		this.requests = requests;
		this.httpRequest = httpRequest;
	}

	@Override
	public void execute() throws IOException {
		super.execute();
		if (finished) {
			System.out.println("cachePath : " + CACHE_PATH);
			String fileName = CACHE_PATH + url.substring(url.lastIndexOf("/?")+2).replace(':', '.').replace('?', '_').replace('/', '-').replace('\\', '-').replace('*', '-').replace('<', '-').replace('>', '-').replace('|', '-');
			File file = new File(fileName);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(this.getData());
			fos.close();
			cacheableImageData.setFile(fileName);
			requests.remove(this);
			file = null;
			rawData = null;
			MainFrame.MOVIE_PANEL.repaintSlider();
		}
	}

	public CacheableImageData getCachaableImageData() {
		return cacheableImageData;
	}
	
	public HTTPRequest getEqualJPIPRequest(){
		return httpRequest;
	}
}
