package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Responsible for creating PfssData objects. This Class will load PfssData objects asynchronously
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssDataCreator {
	private final ExecutorService pool = Executors.newCachedThreadPool();
	private final DateRangeManager manager;
	
	public PfssDataCreator(DateRangeManager manager) {
		this.manager = manager;
	}
		
	public PfssData getDataAsync(int dateIndex) {
		DateAndTimeRange range = manager.getDateAndtime(dateIndex);
		PfssData d = new PfssData(range,this.createURL(range));
		pool.execute(d);
		return d;
	}
	
	private static String createURL(DateAndTimeRange range) {
		return "";
	}
}
