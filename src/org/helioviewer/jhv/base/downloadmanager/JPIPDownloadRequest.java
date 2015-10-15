package org.helioviewer.jhv.base.downloadmanager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.helioviewer.jhv.Directories;

public class JPIPDownloadRequest extends HTTPRequest
{
	private static final String CACHE_PATH = Directories.CACHE.getPath();

	public final String filename;
	
	public JPIPDownloadRequest(String _url, DownloadPriority _priority)
	{
		super(_url, _priority, 60000, 3);
		filename = CACHE_PATH + url.substring(url.lastIndexOf("/?")+2).replace(':', '.').replace('?', '_').replace('/', '-').replace('\\', '-').replace('*', '-').replace('<', '-').replace('>', '-').replace('|', '-');
	}

	@Override
	public void execute() throws IOException, InterruptedException
	{
		super.execute();
		
		byte[] data = getData();
		try(FileOutputStream fos = new FileOutputStream(new File(filename)))
		{
			fos.write(data);
		}
		
		//don't waste memory by keeping the downloaded data in memory
		rawData = null;
	}
}
