package org.helioviewer.jhv.layers;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.layers.AbstractImageLayer.CacheStatus;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KakaduUtils;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataFactory;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.w3c.dom.Document;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_region_compositor;

public class Movie
{
	@Nullable private MetaData[] metaDatas;
	
	public final int sourceId;
	@Nullable private String filename;

	private CacheStatus cacheStatus = CacheStatus.NONE;

	private @Nullable Jp2_threadsafe_family_src family_src;
	private final KakaduLayer kakaduLayer;

	public Movie(KakaduLayer _kakaduLayer, int _sourceId)
	{
		sourceId = _sourceId;
		kakaduLayer=_kakaduLayer;
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
			if(!(getAnyMetaData()!=null))
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
			
			if(!(getAnyMetaData()!=null))
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
			Jpx_source jpxSrc = new Jpx_source();
			jpxSrc.Open(family_src, true);
			
			//count frames
			int[] tempVar = new int[1];
			jpxSrc.Count_compositing_layers(tempVar);
			int framecount = tempVar[0];
			
			jpxSrc.Close();
			jpxSrc.Native_destroy();
			
			//load all metadata
			metaDatas = new MetaData[framecount];
			for (int i = 0; i < framecount; i++)
			{
				metaDatas[i]=MetaDataFactory.getMetaData(readMetadataDocument(i+1));
				
				//FIXME: should invalidate textureCache
				//TextureCache.invalidate(sourceId, metaDatas[i].getLocalDateTime());
			}
			
			if(kakaduLayer.getLUT()==null)
			{
				MetaData md=getAnyMetaData();
				if(md!=null)
				{
					final Lut l=md.getDefaultLUT();
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							kakaduLayer.setLUT(l);
						}
					});
				}
			}
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
	}
	
	@Nullable public String getBackingFile()
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
		public boolean equals(@Nullable Object _obj)
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
	
	@SuppressWarnings("null")
	@Nullable public Match findClosestIdx(@Nonnull LocalDateTime _currentDateTime)
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
	
	@Nullable public MetaData getAnyMetaData()
	{
		if(metaDatas!=null)
			for(MetaData md:metaDatas)
				if(md!=null)
					return md;
		return null;
	}
	
	@Nullable
	public MetaData getMetaData(int idx)
	{
		if (metaDatas != null)
			return metaDatas[idx];
		return null;
	}
	
	@SuppressWarnings("null")
	public int getFrameCount()
	{
		if(metaDatas!=null)
			return metaDatas.length;
		
		return 0;
	}
	
	@Nullable
	public Document readMetadataDocument(int index)
	{
		try
		{
			if(family_src==null)
				return null;
			
			@SuppressWarnings("null")
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

	public @Nullable ByteBuffer decodeImage(int _index, int quality, float zoomPercent, Rectangle _requiredRegion)
	{
		try
		{
			Jpx_source jpxSrc = new Jpx_source();
			jpxSrc.Open(family_src, true);
			
			
			
			Kdu_dims requestedBufferedRegion = new Kdu_dims();
			requestedBufferedRegion.Access_pos().Set_x(_requiredRegion.x);
			requestedBufferedRegion.Access_pos().Set_y(_requiredRegion.y);
			requestedBufferedRegion.Access_size().Set_x(_requiredRegion.width);
			requestedBufferedRegion.Access_size().Set_y(_requiredRegion.height);
			
			
			
			/*Kdu_region_decompressor decompressor = new Kdu_region_decompressor();
			decompressor.Set_interpolation_behaviour(0, 0);
			
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(_requiredRegion.width * _requiredRegion.height);
			
			Kdu_codestream codestream = new Kdu_codestream();
	        Jpx_layer_source xlayer = jpxSrc.Access_layer(_index);
	        Jpx_codestream_source xstream = jpxSrc.Access_codestream(xlayer.Get_codestream_id(0));
			codestream.Create(xstream.Open_stream());
			
			Kdu_coords expand_nominator = new Kdu_coords(1,1);
			Kdu_coords expand_denominator = new Kdu_coords((int)Math.round(1/zoomPercent),(int)Math.round(1/zoomPercent));
			
			int discardLevels=0;
			//DOES NOT WORK... WHY?!
			//while(1d/discardLevels-0.0000001>zoomPercent)
			//	discardLevels++;
			//System.out.println(discardLevels +"  " +1d/discardLevels +"  "+zoomPercent);
			
			decompressor.Start(codestream, null, 0, discardLevels, quality, requestedBufferedRegion, expand_nominator, expand_denominator, false, Kdu_global.KDU_WANT_OUTPUT_COMPONENTS, true);
			
			int[] buf=new int[128000];
			Kdu_dims incompleteRegion = new Kdu_dims();
			incompleteRegion.Assign(requestedBufferedRegion);
			Kdu_dims new_region = new Kdu_dims();
			
			while(decompressor.Process(buf, requestedBufferedRegion.Access_pos(), 0, 0, buf.length, incompleteRegion, new_region))
				
				//DECOMPRESSING BYTES DOES NOT WORK. WHY?!?
			//while(  decompressor.Process(buf, new int[1], 0, requestedBufferedRegion.Access_pos(), 0, 0, buf.length, incompleteRegion, new_region, 8, true, 0, 0, 1))
			{
				//System.out.println("Decoded "+newRegion.Access_pos().Get_x()+", "+newRegion.Access_pos().Get_y()+"  ("+newRegion.Access_size().Get_x()+"x"+newRegion.Access_size().Get_y()+")");
				//System.out.println("Requesting "+incompleteRegion.Access_pos().Get_x()+", "+incompleteRegion.Access_pos().Get_y()+"  ("+incompleteRegion.Access_size().Get_x()+"x"+incompleteRegion.Access_size().Get_y()+")");
				
				for(int i=0;i<new_region.Access_size().Get_x() * new_region.Access_size().Get_y();i++)
					byteBuffer.put((byte)buf[i]);
				//intBuffer.put(buf,0,new_region.Access_size().Get_x() * new_region.Access_size().Get_y());
				
				if(incompleteRegion.Access_size().Get_y() == 0)
					break;
			}
			
			decompressor.Finish();
			decompressor.Native_destroy();
			codestream.Destroy();
			jpxSrc.Native_destroy();
			
			byteBuffer.flip();
			return byteBuffer;*/
			
			
			//FIXME: use kdu_region_decompressor instead
			Kdu_region_compositor compositor = new Kdu_region_compositor(jpxSrc);
			
			//FIXME: downgrade quality first, before resolution when having speed problems
			compositor.Set_max_quality_layers(quality);
			compositor.Set_scale(false, false, false, zoomPercent);
			compositor.Set_buffer_surface(requestedBufferedRegion);
			
			//CRASHES KDU. WHY?!
			//compositor.Set_surface_initialization_mode(false);
			
			//TODO: perhaps enable multi-threaded decoding?
			//compositor.Set_thread_env(threadEnviroment, null);
			
			compositor.Add_ilayer(_index, new Kdu_dims(), new Kdu_dims());
			
			
			Kdu_dims actualBufferedRegion = new Kdu_dims();
			Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(actualBufferedRegion);
			Kdu_coords actualOffset = new Kdu_coords();
			actualOffset.Assign(actualBufferedRegion.Access_pos());
			
			//TODO: don't reallocate buffers all the time
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(_requiredRegion.width * _requiredRegion.height);
			
			Kdu_dims newRegion = new Kdu_dims();
			int[] region_buf = new int[128000];
			while (compositor.Process(128000-_requiredRegion.width, newRegion))
			{
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(actualOffset);
				
				int newPixels = newSize.Get_x() * newSize.Get_y();
				if (newPixels == 0)
					continue;
				if (newPixels > 0)
				{
					compositorBuf.Get_region(newRegion, region_buf);
					
					for(int i=0;i<newPixels;i++)
						byteBuffer.put((byte)region_buf[i]);
				}
			}
			
			compositorBuf.Native_destroy();
			compositor.Native_destroy();
			
			byteBuffer.flip();
			return byteBuffer;
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
		return null;
	}
}
