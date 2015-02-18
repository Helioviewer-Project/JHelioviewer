package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.helioviewer.jhv.plugins.pfssplugin.data.caching.Cacheable;

/**
 * Represents the raw pfss data. This class is able to download the data asynchronously
 * 
 * This class is threadsafe
 * @author Jonas Schwammberger
 *
 */
public class PfssCompressed implements Cacheable
{
	private volatile boolean isLoaded = false;
	private volatile byte[] rawData;
	private final String url;
	private final FileDescriptor descriptor;
	
	/**
	 * 
	 * @param descriptor File Descriptor representing the file on the server
	 * @param url file url to load
	 */
	public PfssCompressed(FileDescriptor descriptor, String url)
	{
		this.descriptor = descriptor;
		this.url = url;
	}
	
	/**
	 * Load the data into memory. this method signals all who are waiting on the condition "loaded"
	 */
	public synchronized void loadData() {
		InputStream in = null;
		try {
			URL u = new URL(url);
			URLConnection uc = u.openConnection();
			int contentLength = uc.getContentLength();
			InputStream raw = uc.getInputStream();
			in = new BufferedInputStream(raw);

			rawData = new byte[contentLength];

			int bytesRead = 0;
			int offset = 0;
			while (offset < contentLength) {
				bytesRead = in.read(rawData, offset, rawData.length
						- offset);
				if (bytesRead == -1)
					break;
				offset += bytesRead;
			}
			isLoaded = true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @return true if data has finished loading into memory
	 */
	public boolean isLoaded()
	{
		return isLoaded;
	}
	
	/**
	 * Check if it is loaded completely before accessing this method.
	 * @return the loaded data 
	 */
	public byte[] getData()
	{
		return rawData;
	}

	@Override
	public FileDescriptor getDescriptor()
	{
		return this.descriptor;
	}
}
