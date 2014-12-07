package org.helioviewer.gl3d.plugin.pfss.data.creators;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.gl3d.plugin.pfss.data.FileDescriptor;
import org.helioviewer.gl3d.plugin.pfss.data.PfssData;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * Responsible for creating  PfssData objects. The PfssData objects will load asynchronously via Threadpools
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssDataCreator {
	private final ExecutorService pool = Executors.newCachedThreadPool();
	
	public PfssDataCreator() {
	}
		
	public PfssData getDataAsync(FileDescriptor desc) {
		PfssData d = new PfssData(desc,this.createURL(desc));
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
