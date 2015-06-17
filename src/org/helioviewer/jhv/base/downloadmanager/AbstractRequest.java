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
	
	protected boolean finished = false;
	private int retries = 3;
	
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

	public AbstractRequest(PRIORITY priority) {
		this.priority = priority;
	}
	
	public AbstractRequest(PRIORITY priority, int retries) {
		this.priority = priority;
	}
	
	public PRIORITY getPriority(){
		return priority;
	}
	
	public boolean isFinished(){
		return finished;
	}
	
	public int decrementRetries(){
		return retries--;
	}
	
	abstract void execute() throws IOException;
}
