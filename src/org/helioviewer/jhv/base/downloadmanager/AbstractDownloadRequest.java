package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

public abstract class AbstractDownloadRequest
{
	public static final int INFINITE_TIMEOUT = -1;
	protected volatile boolean finished = false;
	protected final AtomicInteger retries = new AtomicInteger(3);
	protected volatile @Nullable IOException ioException = null;
	protected volatile int timeOut = 20000;
	protected final String url;
	protected volatile int totalLength = -1;
	protected volatile int receivedLength = 0;
	
	public final DownloadPriority priority;

	public AbstractDownloadRequest(String _url, DownloadPriority _priority)
	{
		url = _url;
		priority = _priority;
	}
	
	public AbstractDownloadRequest(String _url, DownloadPriority _priority, int _retries)
	{
		url = _url;
		priority = _priority;
		retries.set(_retries);
	}
	
	public boolean isFinished()
	{
		return finished;
	}
		
	public boolean justTriedShouldTryAgain()
	{
		return retries.decrementAndGet()>0;
	}
	
	abstract void execute() throws IOException, InterruptedException;

	public void setError(IOException ioException)
	{
		this.ioException = ioException;
		finished = true;
	}
	
	@Override
	public String toString()
	{
		return url;
	}

	public int getTotalLength()
	{
		return totalLength;
	}
	
	public int getReceivedLength()
	{
		return receivedLength;
	}
	
	public void checkException() throws IOException
	{
		if (ioException != null)
			throw ioException;
	}

	public void setRetries(int i)
	{
		this.ioException = null;
		retries.set(i);
		finished = false;
	}
}
