package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.layers.AbstractImageLayer.CacheStatus;
import org.helioviewer.jhv.opengl.TextureCache;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduRender;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class Movie
{
	protected LocalDateTime[] localDateTimes;

	protected LocalDateTime firstDate = LocalDateTime.MAX;
	protected LocalDateTime lastDate = LocalDateTime.MIN;

	public final int sourceId;
	private String filename;

	private CacheStatus cacheStatus = CacheStatus.NONE;

	private Jp2_threadsafe_family_src family_src = new Jp2_threadsafe_family_src();
	private Jpx_source jpxSrc = new Jpx_source();

	private Kdu_region_compositor compositor = new Kdu_region_compositor();

	private static final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;

	private MetaData[] metaDatas;

	public Movie(int _sourceId)
	{
		sourceId = _sourceId;
	}
	
	public synchronized void setFile(String _filename)
	{
		if(filename != null)
			throw new IllegalStateException();
		
		filename = _filename;
		
		try
		{
			family_src.Open(filename);
			processInput();
		}
		catch (KduException e)
		{
			e.printStackTrace();
		}
	}
	
	public synchronized void setKDUCache(Kdu_cache kduCache)
	{
		if (filename != null)
			return;
		
		try
		{
			family_src.Open(kduCache);
			processInput();
			cacheStatus = CacheStatus.KDU_PREVIEW;
		}
		catch (KduException e)
		{
			e.printStackTrace();
		}
	}

	private void processInput()
	{
		try
		{
			jpxSrc.Open(family_src, true);
			compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
			
			KakaduRender kakaduRender = new KakaduRender();
			kakaduRender.openImage(getSource());
			int framecount = getFrameCount();
			LocalDateTime[] newLocalDateTimes = new LocalDateTime[framecount];
			metaDatas = new MetaData[framecount];
			for (int i = 1; i <= framecount; i++)
			{
				metaDatas[i - 1] = kakaduRender.getMetadata(i, getFamilySrc());
				newLocalDateTimes[i - 1] = metaDatas[i-1].getLocalDateTime();
			}
			
			kakaduRender.closeImage();
			
			if(localDateTimes==null)
				setLocalDateTimes(newLocalDateTimes);
		}
		catch (KduException e)
		{
			e.printStackTrace();
		}
		
		TextureCache.invalidate(sourceId, localDateTimes);
	}
	
	public void setLocalDateTimes(LocalDateTime[] _localDateTimes)
	{
		localDateTimes = _localDateTimes;
		
		firstDate = LocalDateTime.MAX;
		lastDate = LocalDateTime.MIN;
		for (LocalDateTime localDateTime : localDateTimes)
		{
			firstDate = localDateTime.isBefore(firstDate) ? localDateTime : firstDate;
			lastDate = localDateTime.isAfter(lastDate) ? localDateTime : lastDate;
		}
	}
	
	public boolean contains(int _sourceId, LocalDateTime _currentDate)
	{
		if (this.sourceId != _sourceId || _currentDate.isBefore(firstDate) || _currentDate.isAfter(lastDate))
			return false;
		
		for (LocalDateTime localDateTime : localDateTimes)
			if (localDateTime.isEqual(_currentDate))
				return true;
		
		return false;
	}

	public Kdu_region_compositor getSource()
	{
		return compositor;
	}

	public String getBackingFile()
	{
		return filename;
	}

	public int getIdx(LocalDateTime localDateTime)
	{
		for (int i = 0; i < localDateTimes.length; i++)
			if (localDateTime.isEqual(localDateTimes[i]))
				return i;

		return -1;
	}

	public CacheStatus getCacheStatus()
	{
		if (filename != null)
			return CacheStatus.FILE_FULL;
		return cacheStatus;
	}

	public Jp2_threadsafe_family_src getFamilySrc()
	{
		return family_src;
	}

	private int getFrameCount() throws KduException
	{
		int[] tempVar = new int[1];
		jpxSrc.Count_compositing_layers(tempVar);
		return tempVar[0];
	}

	public MetaData getMetaData(int idx)
	{
		if (metaDatas == null)
			return null;
		return metaDatas[idx];
	}
}
