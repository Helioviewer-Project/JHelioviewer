package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

/**
 * Responsible for creating PfssData objects. This Class will load PfssData objects asynchronously
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssDataCreator {
	private final ExecutorService pool = Executors.newCachedThreadPool();
	private final FileDescriptorManager manager;
	
	public PfssDataCreator(FileDescriptorManager manager) {
		this.manager = manager;
	}
		
	public PfssData getDataAsync(int fileIndex) {
		FileDescriptor desc = manager.getFileDescriptor(fileIndex);
		PfssData d = new PfssData(desc,this.createURL(desc));
		pool.execute(d);
		return d;
	}
	
	private static String createURL(FileDescriptor file) {
		StringBuilder b = new StringBuilder(PfssSettings.INFOFILE_URL);
		b.append(file.getYear());
		b.append("/");
		b.append(file.getMonth());
		if(file.getMonth() <= 9)
			b.append("0");
		b.append(file.getMonth());
		b.append("/");
		
		//filename
		b.append(file.getFileName());
		
		return b.toString();
	}
}
