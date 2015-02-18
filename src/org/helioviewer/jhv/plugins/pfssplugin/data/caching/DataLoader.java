package org.helioviewer.jhv.plugins.pfssplugin.data.caching;

import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;

/**
 * Responsible for creating  PfssData objects. The PfssData objects will load asynchronously via threadpools
 * 
 * @author Jonas Schwammberger
 *
 */
public class DataLoader
{
	public DataLoader()
	{
	}
	
	/**
	 * Get PfssData Asynchronously
	 * @param desc
	 * @return PfssData object which will be loaded in the future
	 */
	public PfssData getDataAsync(FileDescriptor desc)
	{
		final PfssData d = new PfssData(desc,createURL(desc));
		PfssPlugin.pool.execute(new Runnable()
		{
            @Override
            public void run()
            {
                d.loadData();
            }
        });
		return d;
	}
	
	private static String createURL(FileDescriptor file)
	{
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
