package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;

import com.google.common.io.ByteSource;

public class JPIPDownloadRequest extends HTTPRequest
{
	public final String filename;
	
	public JPIPDownloadRequest(String _url, String _filename, DownloadPriority _priority)
	{
		super(_url, _priority, 3);
		filename=_filename;
	}

	@Override
	public void execute() throws Throwable
	{
		super.execute();
		
		ByteSource data = getData();
		try(FileOutputStream fos = new FileOutputStream(new File(filename)))
		{
			data.copyTo(fos);
		}
		
		//don't waste memory by keeping the downloaded data in memory
		rawData = null;
	}
}
