package org.helioviewer.jhv.layers;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
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

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.AbstractImageLayer.CacheStatus;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KakaduUtils;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataFactory;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.w3c.dom.Document;

public class Movie
{
	private MetaData[] metaDatas;
	
	public final int sourceId;
	private String filename;

	private CacheStatus cacheStatus = CacheStatus.NONE;

	private Jp2_threadsafe_family_src family_src;
	private Jpx_source jpxSrc;
	private final KakaduLayer ul;

	public Movie(KakaduLayer _ul, int _sourceId)
	{
		sourceId = _sourceId;
		ul=_ul;
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
			if(!containsValidFrames())
				throw new UnsuitableMetaDataException();

			cacheStatus = CacheStatus.FULL;
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
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
			
			if(!containsValidFrames())
				throw new UnsuitableMetaDataException();
			
			cacheStatus = CacheStatus.PREVIEW;
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
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
			metaDatas = new MetaData[framecount];
			for (int i = 0; i < framecount; i++)
			{
				metaDatas[i]=MetaDataFactory.getMetaData(readMetadataDocument(i+1));
				
				//FIXME: should invalidate textureCache
				//TextureCache.invalidate(sourceId, metaDatas[i].getLocalDateTime());
			}
			
			if(ul.getLUT()==null)
				for(MetaData md:metaDatas)
					if(md!=null)
					{
						final Lut l=md.getDefaultLUT();
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								ul.setLUT(l);
							}
						});
						break;
					}
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
	}
	
	public boolean containsValidFrames()
	{
		for(MetaData md:metaDatas)
			if(md!=null)
				return true;
		return false;
	}
	
	public String getBackingFile()
	{
		return filename;
	}

	public class Match
	{
		public final int index;
		public final long timeDifferenceNanos;
		public final Movie movie;
		
		Match(int _index, long _timeDifferenceNanos)
		{
			index=_index;
			timeDifferenceNanos=_timeDifferenceNanos;
			movie=Movie.this;
		}
		
		@Override
		public boolean equals(Object _obj)
		{
			if(!(_obj instanceof Match))
				return false;
			
			Match o=(Match)_obj;
			return index==o.index && movie==o.movie;
		}
		
		@Override
		public int hashCode()
		{
			return index ^ Long.hashCode(timeDifferenceNanos);
		}
	}
	
	public Match findClosestIdx(LocalDateTime _currentDateTime)
	{
		if(metaDatas==null)
			return null;
		
		int bestI=-1;
		long minDiff = Long.MAX_VALUE;
		
		for (int i = 0; i < metaDatas.length; i++)
		{
			MetaData md=metaDatas[i];
			if(md==null)
				continue;
			
			LocalDateTime ldt=md.getLocalDateTime();
			if(ldt==null)
				continue;
			
			long curDiff = Math.abs(ChronoUnit.NANOS.between(ldt, _currentDateTime));
			if(curDiff<minDiff)
			{
				minDiff=curDiff;
				bestI=i;
			}
		}
		
		if(bestI==-1)
			return null;
		else
			return new Match(bestI,minDiff);
	}
	
	public CacheStatus getCacheStatus()
	{
		return cacheStatus;
	}
	
	public MetaData getMetaData(int idx)
	{
		if (metaDatas == null)
			return null;
		return metaDatas[idx];
	}
	
	@Nullable
	public Document readMetadataDocument(int index)
	{
		try
		{
			String xmlText = KakaduUtils.getXml(family_src, index);
			if (xmlText == null)
				return null;
			xmlText = xmlText.trim().replace("&", "&amp;").replace("$OBS", "");
			
			try(InputStream in = new ByteArrayInputStream(xmlText.getBytes("UTF-8")))
			{
				DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = builder.parse(in);
				doc.getDocumentElement().normalize();
				
				return doc;
			}
		}
		catch (Exception ex)
		{
			Telemetry.trackException(ex);
		}
		return null;
	}
	
	
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
			Telemetry.trackException(e);
		}
	}*/

	public ByteBuffer decodeImage(int _index, int quality, float zoomPercent, Rectangle _requiredRegion)
	{
		try
		{
			Jpx_source jpxSrc2 = new Jpx_source();
			jpxSrc2.Open(family_src, true);
			
			Kdu_region_compositor compositor = new Kdu_region_compositor(jpxSrc2);
			//TODO: perhaps enable multii-threaded decoding?
			//compositor.Set_thread_env(threadEnviroment, null);
			
			Kdu_dims dimsRef1 = new Kdu_dims();
			Kdu_dims dimsRef2 = new Kdu_dims();

			compositor.Add_ilayer(_index, dimsRef1, dimsRef2);

			//FIXME: downgrade quality first, before resolution when having speed problems
			compositor.Set_max_quality_layers(quality);
			
			compositor.Set_scale(false, false, false, zoomPercent);
			Kdu_dims requestedBufferedRegion = KakaduUtils.rectangleToKdu_dims(_requiredRegion);
			compositor.Set_buffer_surface(requestedBufferedRegion);

			Kdu_dims actualBufferedRegion = new Kdu_dims();
			Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(actualBufferedRegion);
			Kdu_coords actualOffset = new Kdu_coords();
			actualOffset.Assign(actualBufferedRegion.Access_pos());

			//TODO: don't reallocate buffers all the time
			//TODO: decode 8bit grayscale instead of 32bit
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(_requiredRegion.width * _requiredRegion.height * 4);
			IntBuffer intBuffer = byteBuffer.asIntBuffer();

			Kdu_dims newRegion = new Kdu_dims();
			while (compositor.Process(128000 /* MAX_RENDER_SAMPLES */, newRegion))
			{
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(actualOffset);

				int newPixels = newSize.Get_x() * newSize.Get_y();
				if (newPixels == 0)
					continue;
				if (newPixels > 0)
				{
					//TODO: don't reallocate int-array
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
			Telemetry.trackException(e);
		}
		return null;
	}
}
