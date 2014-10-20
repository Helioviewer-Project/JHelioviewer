package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Responsible for creating PfssData objects. This Class will load PfssData objects asynchronously
 * @author Jonas Schwammberger
 *
 */
public class PfssDataCreator {
	private final ExecutorService pool = Executors.newCachedThreadPool();
	
	public PfssDataCreator() {
		
	}
	
	public PfssData getDataAsync(Date d) {
		
		return null;
	}
	
	public PfssData[] getBulkDataAsync(Date d, int nextCount) {
		return null;
	}
}
