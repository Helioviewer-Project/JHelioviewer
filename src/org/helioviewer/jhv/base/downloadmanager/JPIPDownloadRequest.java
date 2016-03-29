package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;

import com.google.common.io.ByteSource;

public class JPIPDownloadRequest extends HTTPRequest
{
	private final String filename;
	private boolean isEmpty;
	
	public JPIPDownloadRequest(String _url, String _filename, DownloadPriority _priority)
	{
		super(_url, _priority, 3);
		filename=_filename;
	}
	
	public boolean isEmpty()
	{
		return isEmpty;
	}
	
	public String getFilename()
	{
		if(!isFinished())
			throw new IllegalStateException("Download not yet finished.");
		
		if(isEmpty)
			throw new IllegalStateException("Download was empty. Check isEmpty() beforehand.");
		
		return filename;
	}

	@Override
	public void execute() throws Throwable
	{
		super.execute();
		
		ByteSource data = getData();
		isEmpty = data.size()==0;
		if(!isEmpty)
			try(FileOutputStream fos = new FileOutputStream(new File(filename)))
			{
				data.copyTo(fos);
			}
		
		//don't waste memory by keeping the downloaded data in memory
		rawData = null;
		System.gc();
		System.runFinalization();
	}
}
