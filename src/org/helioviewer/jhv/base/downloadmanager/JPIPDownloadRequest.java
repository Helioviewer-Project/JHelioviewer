package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.CacheableImageData;

public class JPIPDownloadRequest extends HTTPRequest {

	private static final String CACHE_PATH = "/Users/stefanmeier/Documents/FHNW/JHelioviewer/screenies/";

	private final CacheableImageData cacheableImageData;
	private final ArrayList<AbstractRequest> requests;

	public JPIPDownloadRequest(String url, PRIORITY priority,
			CacheableImageData cacheableImageData,
			ArrayList<AbstractRequest> requests) {
		super(url, priority, 60000, -1);
		this.cacheableImageData = cacheableImageData;
		this.requests = requests;
	}

	@Override
	public void execute() throws IOException {
		super.execute();
		if (finished) {
			System.out.println("url : " + url);
			String fileName = CACHE_PATH + url.substring(url.lastIndexOf("/"));
			System.out.println(fileName);
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
}
