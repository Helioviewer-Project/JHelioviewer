package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;

/**
 * Represents the raw pfss data. This class is able to download the data asynchronously
 * 
 * This class is threadsafe
 * @author Jonas Schwammberger
 *
 */
public class PfssData implements Runnable {
	private volatile boolean isLoaded = false;
	private volatile byte[] rawData;
	private final Lock lock = new ReentrantLock();
	private final Condition loaded = lock.newCondition();
	private final String url;
	private final FileDescriptor descriptor;
	
	/**
	 * 
	 * @param descriptor File Descriptor representing the file on the server
	 * @param url file url to load
	 */
	public PfssData(FileDescriptor descriptor, String url) {
		this.descriptor = descriptor;
		this.url = url;
	}
	
	/**
	 * Load the data into memory. this method signals all who are waiting on the condition "loaded"
	 */
	public void loadData() {
		InputStream in = null;
		lock.lock();
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
			loaded.signalAll();
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
			lock.unlock();
		}
	}
	
	/**
	 * 
	 * @return true if data has finished loading into memory
	 */
	public boolean isLoaded() {
		return isLoaded;
	}
	
	/**
	 * Wait for Data to load.
	 * @throws InterruptedException
	 */
	public void awaitLoaded() throws InterruptedException{
		lock.lock();
		try{
			while(!isLoaded) loaded.await();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Check if it is loaded completely before accessing this method.
	 * @return the loaded data 
	 */
	public byte[] getData() {
		return rawData;
	}

	@Override
	public void run() {
		loadData();
		
	}
	
	public FileDescriptor getDateRange() {
		return this.descriptor;
	}
}
