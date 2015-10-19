package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.io.IOException;
import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.data.caching.DataCache;
import org.helioviewer.jhv.plugins.pfssplugin.data.decompression.PfssDecompressor;

import com.jogamp.opengl.GL2;

/**
 * This class is responsible for managing frames. it Tries to have all frames
 * pre-loaded and pre-initialized before they are requested
 */
public class FrameManager
{
	private final FileDescriptorManager descriptorManager;

	private @Nullable PfssDecompressed curFrame;
	private final DataCache dataCache;

	public FrameManager(PfssPlugin _parent)
	{
		descriptorManager = new FileDescriptorManager(_parent);
		dataCache = new DataCache(descriptorManager, _parent);
	}

	/**
	 * Get Frame which represents the Date
	 * @param date
	 * @return Frame or null if there is no frame for the requested date
	 */
	@SuppressWarnings({ "null" })
	public @Nullable PfssDecompressed getFrame(GL2 _gl, LocalDateTime date)
	{
		//outside of loaded frames
		if(!descriptorManager.isDateInRange(date))
		{
			System.out.println("Not in range");
			return null;
		}
		
		//still the same frame
		if (curFrame!=null && curFrame.getDescriptor().isDateInRange(date))
		{
		    if(!curFrame.isDataAssigned())
		    {
		        FileDescriptor fd=descriptorManager.getFileDescriptor(date);
		        if(fd!=null)
		        {
		            PfssCompressed comp = dataCache.get(fd);
		            comp.loadDataAsync();
		            
		            try
		            {
		            	PfssDecompressor.decompress(comp,curFrame);
		            }
		            catch(NullPointerException _npe)
		            {
		            	Telemetry.trackException(_npe);
		            	return null;
		            }
		        }
		    }
			return curFrame;
		}

		if(curFrame!=null)
		    curFrame.dispose(_gl);
		
        FileDescriptor fd=descriptorManager.getFileDescriptor(date);
        if(fd!=null)
        {
            curFrame = new PfssDecompressed(fd);
            
            PfssCompressed comp = dataCache.get(fd);
            comp.loadDataAsync();
            PfssDecompressor.decompress(comp,curFrame);
            return curFrame;
        }
        
        return null;
	}
	
    /**
     * sets what date range the manager should handle 
     * @param start start date inclusive
     * @param end end date inclusive
     * @throws IOException if the requested dates could not be found
     */
    public void setDateRange(LocalDateTime start, LocalDateTime end)
    {
        descriptorManager.readFileDescriptors(start,end);
    }

    public void showErrorMessages()
    {
        descriptorManager.showErrorMessages();
    }
    
    public @Nullable LocalDateTime getStartDate()
    {
    	return descriptorManager.getStartDate();
    }
    
    public @Nullable LocalDateTime getEndDate()
    {
    	return descriptorManager.getEndDate();
    }

	@SuppressWarnings("null")
	public void retry()
	{
		descriptorManager.readFileDescriptors(descriptorManager.getStartDate(), descriptorManager.getEndDate());
	}
}
