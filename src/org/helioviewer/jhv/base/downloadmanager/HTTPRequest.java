package org.helioviewer.jhv.base.downloadmanager;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.apache.http.client.entity.DeflateInputStream;
import org.helioviewer.jhv.base.math.MathUtils;

import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;

public class HTTPRequest extends AbstractDownloadRequest
{
	private static final int DEFAULT_BUFFER_SIZE = 16384;
	
	protected @Nullable ByteSource rawData;
	
	public HTTPRequest(String _uri, DownloadPriority _priority, int _retries)
	{
		this(_uri, _priority);
		retries.set(_retries);
	}

	public HTTPRequest(String _url, DownloadPriority _priority)
	{
		super(_url, _priority);
	}

	
	private volatile HttpURLConnection httpURLConnection;
	private volatile boolean downloadComplete;
	
	@SuppressWarnings("resource")
	public void execute() throws Throwable
	{
		//FIXME: add proxy support (settings, ...)
		//Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888));
		try
		{
			httpURLConnection = (HttpURLConnection) new URL(url).openConnection(); //proxy);
			RetryConnect:for(;;)
			{
				httpURLConnection.setReadTimeout(TIMEOUT);
				httpURLConnection.setRequestMethod("GET");
				httpURLConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
				
				httpURLConnection.connect();
				int response = httpURLConnection.getResponseCode();
				switch(response)
				{
					case HttpURLConnection.HTTP_OK:
						break RetryConnect;
					case HttpURLConnection.HTTP_MOVED_TEMP:
					case HttpURLConnection.HTTP_MOVED_PERM:
						System.out.println("Redirecting "+httpURLConnection.getURL().toString()+" to "+httpURLConnection.getHeaderField("Location"));
						httpURLConnection = (HttpURLConnection)new URL(httpURLConnection.getHeaderField("Location")).openConnection(); //proxy);
						continue;
					default:
						throw new IOException("HTTP response code "+response+" for "+httpURLConnection.getURL().toString());
				}
			}
			
			totalLength = httpURLConnection.getContentLength();
			InputStream is=new BufferedInputStream(httpURLConnection.getInputStream(),MathUtils.clip(totalLength,1024,65536));
			if(httpURLConnection.getContentEncoding()!=null)
				switch(httpURLConnection.getContentEncoding().toLowerCase())
				{
					case "gzip":
						is=new GZIPInputStream(is,8192);
						break;
					case "deflate":
						is=new DeflateInputStream(is);
						break;
					default:
						throw new IOException("Unknown encoding: "+httpURLConnection.getContentEncoding());
				}
			
			try(InputStream inputStream = is)
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
			downloadComplete=true;
			try
			{
				httpURLConnection.disconnect();
			}
			catch(Exception _e)
			{
			}
		}
	}

	public String getDataAsString() throws Throwable
	{
		return getData().asCharSource(StandardCharsets.UTF_8).read();
	}
	
	public ByteSource getData() throws Throwable
	{
		//TODO: proper synchronization
		while(!downloadComplete)
			Thread.sleep(20);
		
		if (exception != null)
			throw exception;
		
		return rawData;
	}

	@Override
	public void interrupt()
	{
		try
		{
			cancelled=true;
			httpURLConnection.disconnect();
		}
		catch(Throwable t)
		{
		}
	}
}
