package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;

public class HTTPRequest extends AbstractDownloadRequest
{
	private static final int DEFAULT_BUFFER_SIZE = 16384;
	
	protected @Nullable ByteSource rawData;
	
	public HTTPRequest(String _uri, DownloadPriority _priority, int _timeOut, int _retries)
	{
		this(_uri, _priority);
		retries.set(_retries);
		timeOut = _timeOut;
	}

	public HTTPRequest(String _uri, DownloadPriority _priority, int _retries)
	{
		this(_uri, _priority);
		retries.set(_retries);
	}

	public HTTPRequest(String _url, DownloadPriority _priority)
	{
		super(_url, _priority);
	}

	public void execute() throws Throwable
	{
		HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(this.url).openConnection();
		try
		{
			httpURLConnection.setReadTimeout(timeOut);
			httpURLConnection.setRequestMethod("GET");
			//TODO: accept-encoding: GZIP && GZIPInputStream
			httpURLConnection.connect();
			int response = httpURLConnection.getResponseCode();
			totalLength = httpURLConnection.getContentLength();
			if (response != HttpURLConnection.HTTP_OK)
				throw new IOException("Response code "+response);
			
			try(InputStream inputStream = httpURLConnection.getInputStream())
			{
				try(FileBackedOutputStream byteArrayOutputStream = new FileBackedOutputStream(1024*1024*4,true))
				{
					byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
					int read;
					while ((read = inputStream.read(buf)) > 0)
					{
						byteArrayOutputStream.write(buf, 0, read);
						receivedLength += read;
					}
					rawData = byteArrayOutputStream.asByteSource();
					byteArrayOutputStream.close();
				}
			}
		}
		finally
		{
			try
			{
				httpURLConnection.disconnect();
			}
			catch(Exception _e)
			{
			}
			finished = true;
		}
	}

	public String getDataAsString() throws Throwable
	{
		return getData().asCharSource(StandardCharsets.UTF_8).read();
	}
	
	@SuppressWarnings("null")
	public ByteSource getData() throws Throwable
	{
		//TODO: proper synchronization
		while(!isFinished())
			Thread.sleep(20);
		
		if (exception != null)
			throw exception;
		
		return rawData;
	}
}
