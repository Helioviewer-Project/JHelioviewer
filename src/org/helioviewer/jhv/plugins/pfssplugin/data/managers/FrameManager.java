package org.helioviewer.jhv.plugins.pfssplugin.data.managers;

import java.io.IOException;
import java.util.Date;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssFrame;
import org.helioviewer.jhv.plugins.pfssplugin.data.caching.DataCache;

/**
 * This class is responsible for managing frames. it Tries to have all frames
 * pre-loaded and pre-initialized before they are requested
 * 
 * @author Jonas Schwammberger
 */
public class FrameManager {
	private final FileDescriptorManager descriptorManager;

	private PfssFrame cur;
	private DataCache dataCache;

	public FrameManager() {
		descriptorManager = new FileDescriptorManager();
		dataCache = new DataCache(descriptorManager);
	}

	/**
	 * Get Frame which represents the Date
	 * @param date
	 * @return Frame or null if there is no frame for the requested date
	 */
	public PfssFrame getFrame(GL2 _gl, Date date)
	{
		//outside of loaded frames
		if(!descriptorManager.isDateInRange(date))
			return null;
		
		//still the same frame
		if (cur!=null && cur.getDescriptor().isDateInRange(date))
			return cur;

		if(cur!=null)
		    cur.clear(_gl);
		
		FileDescriptor fd=descriptorManager.getFileDescriptor(date);
		
		cur = new PfssFrame(fd);
        PfssDecompressor r = new PfssDecompressor(dataCache.get(fd),cur);
        r.run();
        
		cur.init(_gl);
		return cur;
	}
	
    /**
     * sets what date range the manager should handle 
     * @param start start date inclusive
     * @param end end date inclusive
     * @throws IOException if the requested dates could not be found
     */
    public void setDateRange(Date start, Date end) throws IOException {
        descriptorManager.readFileDescriptors(start,end);
    }
}
