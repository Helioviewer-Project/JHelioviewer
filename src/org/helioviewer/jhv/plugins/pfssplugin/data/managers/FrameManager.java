package org.helioviewer.jhv.plugins.pfssplugin.data.managers;

import java.io.IOException;
import java.util.Date;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin3dRenderer;
import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressed;
import org.helioviewer.jhv.plugins.pfssplugin.data.caching.DataCache;
import org.helioviewer.jhv.plugins.pfssplugin.data.decompression.PfssDecompressor;

/**
 * This class is responsible for managing frames. it Tries to have all frames
 * pre-loaded and pre-initialized before they are requested
 * 
 * @author Jonas Schwammberger
 */
public class FrameManager
{
	private final FileDescriptorManager descriptorManager;

	private PfssDecompressed curFrame;
	private DataCache dataCache;

	public FrameManager(PfssPlugin3dRenderer _parent)
	{
		descriptorManager = new FileDescriptorManager(_parent);
		dataCache = new DataCache(descriptorManager);
	}

	/**
	 * Get Frame which represents the Date
	 * @param date
	 * @return Frame or null if there is no frame for the requested date
	 */
	public PfssDecompressed getFrame(GL2 _gl, Date date)
	{
		//outside of loaded frames
		if(!descriptorManager.isDateInRange(date))
			return null;
		
		//still the same frame
		if (curFrame!=null && curFrame.getDescriptor().isDateInRange(date))
		{
		    if(!curFrame.isDataAssigned())
		    {
		        FileDescriptor fd=descriptorManager.getFileDescriptor(date);
	            PfssDecompressor.decompress(dataCache.get(fd),curFrame);
		    }
			return curFrame;
		}

		if(curFrame!=null)
		    curFrame.dispose(_gl);
		
        FileDescriptor fd=descriptorManager.getFileDescriptor(date);
        curFrame = new PfssDecompressed(fd);
        PfssDecompressor.decompress(dataCache.get(fd),curFrame);
        
		return curFrame;
	}
	
    /**
     * sets what date range the manager should handle 
     * @param start start date inclusive
     * @param end end date inclusive
     * @throws IOException if the requested dates could not be found
     */
    public void setDateRange(Date start, Date end)
    {
        descriptorManager.readFileDescriptors(start,end);
    }

    public void showErrorMessages()
    {
        descriptorManager.showErrorMessages();
    }
}
