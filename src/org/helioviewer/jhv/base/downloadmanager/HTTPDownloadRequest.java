package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HTTPDownloadRequest extends HTTPRequest{
	
	private final String fileName;
	
	public HTTPDownloadRequest(String url, DownloadPriority priority, String fileName) {
		super(url, priority, 60000, -1);
		this.fileName = fileName;
	}

	@Override
	public void execute() throws IOException {
		super.execute();
		if (finished){
			File file = new File(fileName);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(this.getData());
			fos.close();
			file = null;
			rawData = null;
		}
	}
	
}
