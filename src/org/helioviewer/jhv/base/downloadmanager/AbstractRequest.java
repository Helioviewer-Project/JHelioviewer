package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;

/**
 * AbstractRequest is used with the UltimateDownloadmanager
 * That request would be downloaded with a blockingpriorityqueue
 * 
 * @author stefanmeier
 *
 */
public abstract class AbstractRequest {
	public static final int INFINITE = -1;
	protected boolean finished = false;
	protected int retries = 3;
	protected IOException ioException = null;
	protected int timeOut = 20000;
	protected final String url;
	protected int totalLength = -1;
	protected int receivedLength = 0;
	
	/**
	 * PRIORITY are used for the Downloadmanager
	 * The ordinal of the enum would be used to set the priority
	 * lower ordinal int = higher priority
	 * 
	 * @author stefanmeier
	 *
	 */
	public enum PRIORITY{
		URGENT, HIGH, MEDIUM, LOW, TIMEDEPEND;
	}

	private PRIORITY priority;

	public AbstractRequest(String url, PRIORITY priority) {
		this.url = url;
		this.priority = priority;
	}
	
	public AbstractRequest(String url, PRIORITY priority, int retries) {
		this.url = url;
		this.priority = priority;
	}
	
	public PRIORITY getPriority(){
		return priority;
	}
	
	public boolean isFinished(){
		return finished;
	}
		
	public boolean hasRetry(){
		return !(--retries > 0 ? false : retries < 0 ? false : true); 
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

	public void setPriority(PRIORITY priority) {
		this.priority = priority;
	}
	
	public int getTotalLength(){
		return totalLength;
	}
	
	public int getReceivedLength(){
		return receivedLength;
	}
	
	public void checkException() throws IOException{
		if (ioException != null) throw ioException;
	}

	public void setRetries(int i) {
		this.ioException = null;
		retries = 3;
		finished = false;
	}
}
