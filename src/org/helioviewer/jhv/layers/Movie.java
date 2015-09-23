package org.helioviewer.jhv.layers;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_region_compositor;

import org.helioviewer.jhv.layers.AbstractImageLayer.CacheStatus;
import org.helioviewer.jhv.opengl.TextureCache;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KakaduUtils;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataContainer;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataFactory;
import org.w3c.dom.Document;

public class Movie
{
	private LocalDateTime[] localDateTimes;

	private LocalDateTime firstDate = LocalDateTime.MAX;
	private LocalDateTime lastDate = LocalDateTime.MIN;

	public final int sourceId;
	private String filename;

	private CacheStatus cacheStatus = CacheStatus.NONE;

	private Jp2_threadsafe_family_src family_src;
	private Jpx_source jpxSrc;
	private MetaData[] metaDatas;

	public Movie(int _sourceId)
	{
		sourceId = _sourceId;
	}
	
	public synchronized void setFile(String _filename)
	{
		if(filename != null)
			throw new IllegalStateException();
		
		try
		{
			filename = _filename;
			family_src = new Jp2_threadsafe_family_src();
			family_src.Open(filename);
			processFamilySrc();
			cacheStatus = CacheStatus.FILE_FULL;
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
			family_src = new Jp2_threadsafe_family_src();
			family_src.Open(kduCache);
			processFamilySrc();
			cacheStatus = CacheStatus.KDU_PREVIEW;
		}
		catch (KduException e)
		{
			e.printStackTrace();
		}
	}

	
	private void processFamilySrc()
	{
		try
		{
			jpxSrc = new Jpx_source();
			jpxSrc.Open(family_src, true);
			
			//count frames
			int[] tempVar = new int[1];
			jpxSrc.Count_compositing_layers(tempVar);
			int framecount = tempVar[0];
			
			//load all metadata
			LocalDateTime[] newLocalDateTimes = new LocalDateTime[framecount];
			metaDatas = new MetaData[framecount];
			for (int i = 1; i <= framecount; i++)
			{
				metaDatas[i - 1] = readMetadata(i, family_src);
				newLocalDateTimes[i - 1] = metaDatas[i-1].getLocalDateTime();
			}
			
			//don't overwrite times if we already parsed them from another source. usually
			//the times from different sources differ by a couple of milliseconds. updating
			//the localDateTimes would mean that later searches for an exact time wouldn't
			//succeed
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
		return sourceId == _sourceId && cacheStatus!=CacheStatus.NONE && !_currentDate.isBefore(firstDate) && !_currentDate.isAfter(lastDate);
	}

	public String getBackingFile()
	{
		return filename;
	}

	public int getClosestIdx(LocalDateTime _localDateTime)
	{
		int bestI=-1;
		long minDiff = Long.MAX_VALUE; 
		
		for (int i = 0; i < localDateTimes.length; i++)
		{
			long curDiff = Math.abs(ChronoUnit.NANOS.between(localDateTimes[i], _localDateTime));
			if(curDiff<minDiff)
			{
				minDiff=i;
				bestI=i;
			}
		}
		
		return bestI;
	}

	public CacheStatus getCacheStatus()
	{
		return cacheStatus;
	}

	@Deprecated
	public MetaData getMetaData(int idx)
	{
		if (metaDatas == null)
			return null;
		return metaDatas[idx];
	}
	
	public MetaData getMetaData(LocalDateTime _ldt)
	{
		return getMetaData(getClosestIdx(_ldt));
	}
	
	private MetaData readMetadata(int index, Jp2_threadsafe_family_src family_src) throws KduException
	{
		String xmlText = KakaduUtils.getXml(family_src, index);
		if (xmlText == null)
			return null;
		xmlText = xmlText.trim().replace("&", "&amp;").replace("$OBS", "");

		InputStream in = null;
		try
		{
			in = new ByteArrayInputStream(xmlText.getBytes("UTF-8"));
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(in);
			doc.getDocumentElement().normalize();

			MetaData metaData = MetaDataFactory.getMetaData(new MetaDataContainer(doc));
			return metaData;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	private static final int MAX_RENDER_SAMPLES = 128000;
	
	/*private Kdu_thread_env threadEnviroment;
	public KakaduRender()
	{
		int numberThreads;
		try
		{
			numberThreads = Kdu_global.Kdu_get_num_processors();
			this.threadEnviroment = new Kdu_thread_env();
			threadEnviroment.Create();
			for (int i = 0; i < numberThreads; i++)
				threadEnviroment.Add_thread();
		}
		catch (KduException e)
		{
			e.printStackTrace();
		}
	}*/

	public ByteBuffer getImage(LocalDateTime _localDateTime, int quality, float zoomPercent, Rectangle imageSize)
	{
		try
		{
			Jpx_source jpxSrc2 = new Jpx_source();
			jpxSrc2.Open(family_src, true);
			
			Kdu_region_compositor compositor = new Kdu_region_compositor(jpxSrc2);
			//compositor.Set_thread_env(threadEnviroment, null);
			
			/*compositor.Refresh();
			compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);*/

			Kdu_dims dimsRef1 = new Kdu_dims();
			Kdu_dims dimsRef2 = new Kdu_dims();

			compositor.Add_ilayer(getClosestIdx(_localDateTime), dimsRef1, dimsRef2);

			//FIXME: downgrade quality first, before resolution when having speed problems
			compositor.Set_max_quality_layers(quality);
			
			compositor.Set_scale(false, false, false, zoomPercent);
			Kdu_dims requestedBufferedRegion = KakaduUtils.rectangleToKdu_dims(imageSize);
			compositor.Set_buffer_surface(requestedBufferedRegion);

			Kdu_dims actualBufferedRegion = new Kdu_dims();
			Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(actualBufferedRegion);
			Kdu_coords actualOffset = new Kdu_coords();
			actualOffset.Assign(actualBufferedRegion.Access_pos());

			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(imageSize.height * imageSize.width * 4);
			IntBuffer intBuffer = byteBuffer.asIntBuffer();

			Kdu_dims newRegion = new Kdu_dims();
			while (compositor.Process(MAX_RENDER_SAMPLES, newRegion))
			{
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(actualOffset);

				int newPixels = newSize.Get_x() * newSize.Get_y();
				if (newPixels == 0)
					continue;
				if (newPixels > 0)
				{
					//FIXME: don't reallocate int-array
					int[] region_buf = new int[newPixels];
					compositorBuf.Get_region(newRegion, region_buf);
					intBuffer.put(region_buf);
				}
			}

			intBuffer.flip();
			compositor.Native_destroy();

			return byteBuffer;
		}
		catch (KduException e)
		{
			
			e.printStackTrace();
		}
		return null;
	}
}
