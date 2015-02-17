package org.helioviewer.jhv.plugins.pfssplugin.data.creators;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.settings.PfssSettings;

/**
 * Responsible for creating  PfssData objects. The PfssData objects will load asynchronously via threadpools
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssDataCreator {
	private final ExecutorService pool = Executors.newCachedThreadPool();
	
	public PfssDataCreator() {
	}
	
	/**
	 * Get PfssData Asynchronously
	 * @param desc
	 * @return PfssData object which will be loaded in the future
	 */
	public PfssData getDataAsync(FileDescriptor desc) {
		PfssData d = new PfssData(desc,createURL(desc));
		pool.execute(d);
		return d;
	}
	
	private static String createURL(FileDescriptor file) {
		StringBuilder b = new StringBuilder(PfssSettings.SERVER_URL);
		b.append(file.getYear());
		b.append("/");
		if(file.getMonth() < 9)
			b.append("0");
		b.append(file.getMonth()+1);
		b.append("/");
		
		//filename
		b.append(file.getFileName());
		
		return b.toString();
	}
}
