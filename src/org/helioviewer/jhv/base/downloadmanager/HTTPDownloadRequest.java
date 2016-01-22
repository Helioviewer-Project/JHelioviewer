package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;

import com.google.common.io.ByteSource;

public class HTTPDownloadRequest extends HTTPRequest
{
	private final String fileName;
	
	public HTTPDownloadRequest(String url, DownloadPriority priority, String _fileName)
	{
		super(url, priority, -1);
		fileName = _fileName;
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
