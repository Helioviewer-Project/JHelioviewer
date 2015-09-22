package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.io.IOException;

import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.data.caching.Cacheable;
import org.helioviewer.jhv.plugins.plugin.Plugins;

import com.github.junrar.Archive;
import com.github.junrar.Volume;
import com.github.junrar.VolumeManager;
import com.github.junrar.io.IReadOnlyAccess;
import com.github.junrar.io.ReadOnlyAccessByteArray;

/**
 * Represents the raw pfss data. This class is able to download the data asynchronously
 * 
 * This class is threadsafe
 */
public class PfssCompressed implements Cacheable
{
    private volatile boolean isLoading = false;
	private volatile boolean isLoaded = false;
	private volatile byte[] rawData;
	private final FileDescriptor descriptor;
	private final HTTPRequest httpRequest;
	private final PfssPlugin parent;
	/**
	 * 
	 * @param descriptor File Descriptor representing the file on the server
	 * @param url file url to load
	 */
	public PfssCompressed(FileDescriptor descriptor, String url, PfssPlugin parent)
	{
		this.descriptor = descriptor;
		httpRequest = Plugins.generateAndStartHTPPRequest(url, DownloadPriority.MEDIUM);
		this.parent = parent;
	}
	
	/**
	 * Load the data into memory. this method signals all who are waiting on the condition "loaded"
	 */
	public synchronized void loadData()
	{
	    if(isLoaded)
	    {
	        isLoading=false;
	        return;
	    }
	    
	    try
	    {
			rawData = httpRequest.getData();
		    isLoaded = true;
		}
	    catch (IOException e)
	    {
			parent.addBadRequest(httpRequest);
		}
	    catch(InterruptedException _ie)
	    {
	    }
	}
	
	/**
	 * 
	 * @return true if data has finished loading into memory
	 */
	public boolean isLoaded()
	{
		return isLoaded;
	}
	
    /**
     * 
     * @return true if data is loading
     */
    public boolean isLoading()
    {
        return isLoading;
    }
	
	/**
	 * Check if it is loaded completely before accessing this method.
	 * @return the loaded data 
	 */
	public byte[] getData()
	{
		return rawData;
	}
	
	public VolumeManager getVolumeManage(){
		return new ByteArrayVolumeManager(this.getData());
	}

	@Override
	public FileDescriptor getDescriptor()
	{
		return this.descriptor;
	}

    public void loadDataAsync()
    {
        if(isLoading || isLoaded)
            return;
        
        isLoading=true;
        
        PfssPlugin.pool.execute(new Runnable()
        {
            @Override
            public void run()
            {
                loadData();
                Plugins.repaintMainPanel();
            }
        });   
    }
    
    public class ByteArrayVolumeManager implements VolumeManager {
        private byte[] bytes;

        public ByteArrayVolumeManager(byte [] bytes) {
            this.bytes = bytes;
        }

        @Override
        public Volume nextArchive(Archive archive, Volume last)
                throws IOException {
            return new ByteArrayVolume( archive, bytes );
        }
    }
    
    public static class ByteArrayVolume implements Volume {
        private final Archive archive;
        private final byte [] bytes;

        /**
         * @param file
         */
        public ByteArrayVolume(Archive archive, byte [] bytes) {
            this.archive = archive;
            this.bytes = bytes;
        }

        @Override
        public IReadOnlyAccess getReadOnlyAccess() throws IOException {
            return new ReadOnlyAccessByteArray(bytes);
        }

        @Override
        public long getLength() {
            return bytes.length;
        }

        @Override
        public Archive getArchive() {
            return archive;
        }

    }
}
