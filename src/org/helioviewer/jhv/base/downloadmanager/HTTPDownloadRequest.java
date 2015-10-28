package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;

import com.google.common.io.ByteSource;

public class HTTPDownloadRequest extends HTTPRequest
{
	private final String fileName;
	
	public HTTPDownloadRequest(String url, DownloadPriority priority, String fileName)
	{
		super(url, priority, 60000, -1);
		this.fileName = fileName;
	}

	@Override
	public void execute() throws Throwable
	{
		super.execute();
		
		ByteSource data = getData();
		try(FileOutputStream fos = new FileOutputStream(new File(fileName)))
		{
			data.copyTo(fos);
		}
	}
}
