package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.data.caching.Cacheable;

import com.github.junrar.Archive;
import com.github.junrar.Volume;
import com.github.junrar.VolumeManager;
import com.github.junrar.exception.RarException;
import com.github.junrar.exception.RarException.RarExceptionType;
import com.github.junrar.io.IReadOnlyAccess;
import com.github.junrar.io.ReadOnlyAccessByteArray;

/**
 * Represents the raw pfss data. This class is able to download the data
 * asynchronously
 * 
 * This class is threadsafe
 */
public class PfssCompressed implements Cacheable
{
	private volatile boolean isLoading = false;
	private volatile boolean isLoaded = false;
	private @Nullable volatile byte[] rawData;
	private final FileDescriptor descriptor;
	private final HTTPRequest httpRequest;
	private final PfssPlugin parent;

	/**
	 * 
	 * @param _descriptor
	 *            File Descriptor representing the file on the server
	 * @param url
	 *            file url to load
	 */
	public PfssCompressed(FileDescriptor _descriptor, String url, PfssPlugin _parent)
	{
		descriptor = _descriptor;
		httpRequest = Plugins.generateAndStartHTPPRequest(url, DownloadPriority.MEDIUM);
		parent = _parent;
	}

	/**
	 * Load the data into memory. this method signals all who are waiting on the
	 * condition "loaded"
	 */
	public synchronized void loadData()
	{
		if (isLoaded)
		{
			isLoading = false;
			return;
		}

		try(ByteArrayOutputStream bos=new ByteArrayOutputStream(65536))
		{
			httpRequest.getData().copyTo(bos);
			rawData = bos.toByteArray();
			isLoaded = true;
		}
		catch (IOException e)
		{
			parent.failedDownloads.add(httpRequest);
		}
		catch (InterruptedException _ie)
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

	@SuppressWarnings("null")
	public VolumeManager getVolumeManage() throws RarException
	{
		if(rawData==null)
			throw new RarException(RarExceptionType.unkownError);
		
		return new ByteArrayVolumeManager(rawData);
	}

	@Override
	public FileDescriptor getDescriptor()
	{
		return this.descriptor;
	}

	public void loadDataAsync()
	{
		if (isLoading || isLoaded)
			return;
		
		isLoading = true;

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

	public static class ByteArrayVolumeManager implements VolumeManager
	{
		private byte[] bytes;

		public ByteArrayVolumeManager(byte[] _bytes)
		{
			bytes = _bytes;
		}

		@Override
		public @Nullable Volume nextArchive(@Nullable Archive archive, @Nullable Volume last) throws IOException
		{
			if(archive==null)
				return null;
			
			return new ByteArrayVolume(archive, bytes);
		}
	}

	public static class ByteArrayVolume implements Volume
	{
		private final Archive archive;
		private final byte[] bytes;

		public ByteArrayVolume(Archive _archive, byte[] _bytes)
		{
			archive = _archive;
			bytes = _bytes;
		}

		@Override
		public IReadOnlyAccess getReadOnlyAccess() throws IOException
		{
			return new ReadOnlyAccessByteArray(bytes);
		}

		@Override
		public long getLength()
		{
			return bytes.length;
		}

		@Override
		public Archive getArchive()
		{
			return archive;
		}
	}
}
