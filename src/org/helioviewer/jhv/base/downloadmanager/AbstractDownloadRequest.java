package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;

public abstract class AbstractDownloadRequest
{
	public static final int INFINITE_TIMEOUT = -1;
	protected boolean finished = false;
	protected int retries = 3;
	protected IOException ioException = null;
	protected int timeOut = 20000;
	protected final String url;
	protected int totalLength = -1;
	protected int receivedLength = 0;
	
	private DownloadPriority priority;

	public AbstractDownloadRequest(String url, DownloadPriority priority) {
		this.url = url;
		this.priority = priority;
	}
	
	public AbstractDownloadRequest(String url, DownloadPriority priority, int retries) {
		this.url = url;
		this.priority = priority;
	}
	
	public DownloadPriority getPriority(){
		return priority;
	}
	
	public boolean isFinished(){
		return finished;
	}
		
	public void justRetried()
	{
		retries--;
	}
	
	public boolean shouldRetry()
	{
		return retries > 0; 
	}
	
	abstract void execute() throws IOException;

	public void addError(IOException ioException) {
		this.ioException = ioException;
		finished = true;
	}
	
	@Override
	public String toString() {
		return url;
	}

	public void setPriority(DownloadPriority priority) {
		this.priority = priority;
	}
	
	public int getTotalLength(){
		return totalLength;
	}
	
	public int getReceivedLength(){
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
		retries = 3;
		finished = false;
	}
}
