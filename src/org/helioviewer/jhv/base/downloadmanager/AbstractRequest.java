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
	protected int timeOut = 10000;
	protected final String url;
	
	/**
	 * PRIORITY are used for the Downloadmanager
	 * The ordinal of the enum would be used to set the priority
	 * lower ordinal int = higher priority
	 * 
	 * @author stefanmeier
	 *
	 */
	public enum PRIORITY{
		HIGH, MEDIUM, LOW, TIMEDEPEND;
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
		finished = --retries > 0 ? false : retries < 0 ? false : true;
		return !finished; 
	}
	
	abstract void execute() throws IOException;

	public void addError(IOException ioException) {
		this.ioException = ioException;
	}
	
	@Override
	public String toString() {
		return url;
	}
}
