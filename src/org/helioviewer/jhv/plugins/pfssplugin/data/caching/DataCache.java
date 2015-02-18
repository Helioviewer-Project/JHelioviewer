package org.helioviewer.jhv.plugins.pfssplugin.data.caching;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssCompressed;
import org.helioviewer.jhv.plugins.pfssplugin.data.managers.FileDescriptorManager;

/**
 * Represents the DataCache responsible for caching, loading and preloading PfssData 
 * objects
 * @author Jonas Schwammberger
 *
 */
public class DataCache
{
	private final FileDescriptorManager descriptorManager;
	
	private final LRUCache<PfssCompressed> readAheadCache;
	private final LRUCache<PfssCompressed> cache;
	
	
	public DataCache(FileDescriptorManager descriptors)
	{
		this.descriptorManager = descriptors;
		this.cache = new LRUCache<>(PfssSettings.DATA_CACHE_SIZE);
		this.readAheadCache = new LRUCache<>(PfssSettings.DATA_READ_AHEAD_SIZE);
	}
	
	public PfssCompressed get(FileDescriptor d)
	{
		if(cache.contains(d))
			return cache.get(d);
		
		if(readAheadCache.contains(d))
		{
			PfssCompressed data = readAheadCache.get(d);
			cache.put(d, data);
			readAhead(d);
			return data;
		}
		
		//cache miss
		PfssCompressed data = getDataAsync(d);
		cache.put(d, data);
		readAhead(d);
		return data;
	}
	
	/**
	 * Fills the read ahead cache with PfssData objects which are likely to be needed next
	 * @param d
	 */
	private void readAhead(FileDescriptor d)
	{
		FileDescriptor next = descriptorManager.getNext(d);
		
		for(int i = 0; i < readAheadCache.size();i++)
		{
			if(!cache.contains(next))
	            if(!readAheadCache.contains(next))
	                readAheadCache.put(next, getDataAsync(next));
			
			next=descriptorManager.getNext(next);
		}
	}
	
	   /**
     * Get PfssData Asynchronously
     * @param desc
     * @return PfssData object which will be loaded in the future
     */
    public static PfssCompressed getDataAsync(FileDescriptor desc)
    {
        String url = PfssSettings.SERVER_URL
                + desc.getYear() + "/"
                + (desc.getMonth()<9?"0":"") + (desc.getMonth()+1)
                + "/" + desc.getFileName();
        
        final PfssCompressed d = new PfssCompressed(desc,url);
        PfssPlugin.pool.execute(new Runnable()
        {
            @Override
            public void run()
            {
                d.loadData();
                GuiState3DWCS.mainComponentView.getComponent().repaint();
            }
        });
        return d;
    }
}
