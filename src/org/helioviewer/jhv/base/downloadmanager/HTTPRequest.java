package org.helioviewer.jhv.base.downloadmanager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HTTPRequest extends AbstractDownloadRequest
{
	private static final int DEFAULT_BUFFER_SIZE = 16384;
	
	protected byte[] rawData;

	public HTTPRequest(String uri, DownloadPriority priority, int timeOut, int retries)
	{
		this(uri, priority);
		this.retries.set(retries);
		this.timeOut = timeOut;
	}

	public HTTPRequest(String uri, DownloadPriority priority, int retries)
	{
		this(uri, priority);
		this.retries.set(retries);
	}

	public HTTPRequest(String url, DownloadPriority priority)
	{
		super(url, priority);
	}

	public void execute() throws IOException, InterruptedException
	{
		HttpURLConnection httpURLConnection = null;
		InputStream inputStream = null;
		ByteArrayOutputStream byteArrayOutputStream = null;
		URL url;
		url = new URL(this.url);
		httpURLConnection = (HttpURLConnection) url.openConnection();
		httpURLConnection.setReadTimeout(timeOut);
		httpURLConnection.setRequestMethod("GET");
		//TODO: accept-encoding: GZIP && GZIPInputStream
		httpURLConnection.connect();
		int response = httpURLConnection.getResponseCode();
		totalLength = httpURLConnection.getContentLength();
		if (response == HttpURLConnection.HTTP_OK) {
			inputStream = httpURLConnection.getInputStream();
			int receivedLength = 0;
			byteArrayOutputStream = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
			byte[] buf = new byte[DEFAULT_BUFFER_SIZE];

			while ((receivedLength = inputStream.read(buf)) > 0)
			{
				byteArrayOutputStream.write(buf, 0, receivedLength);
				this.receivedLength += receivedLength;
			}
			rawData = byteArrayOutputStream.toByteArray();
			byteArrayOutputStream.close();
		}
		else
			throw new IOException();

		if (inputStream != null)
		{
			byteArrayOutputStream.close();
			inputStream.close();
			httpURLConnection.disconnect();
		}
		finished = true;
	}

	public String getDataAsString() throws IOException, InterruptedException
	{
		byte[] data = getData();
		if (data == null)
			throw new IOException();
		
		return new String(data,StandardCharsets.UTF_8);
	}

	public byte[] getData() throws IOException, InterruptedException
	{
		//TODO: proper synchronization
		while(!isFinished())
			Thread.sleep(20);
		
		if (ioException != null)
			throw ioException;
		
		return rawData;
	}
}
