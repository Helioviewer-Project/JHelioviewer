package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class HTTPDownloadRequest extends HTTPRequest
{
	private final String fileName;
	
	public HTTPDownloadRequest(String url, DownloadPriority priority, String fileName)
	{
		super(url, priority, 60000, -1);
		this.fileName = fileName;
	}

	@Override
	public void execute() throws IOException, InterruptedException
	{
		super.execute();
		
		byte[] data = this.getData();
		try(FileOutputStream fos = new FileOutputStream(new File(fileName)))
		{
			fos.write(data);
		}
	}
}
