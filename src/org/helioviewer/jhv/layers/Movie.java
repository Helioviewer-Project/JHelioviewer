package org.helioviewer.jhv.layers;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.opengl.Texture;
import org.helioviewer.jhv.viewmodel.TimeLine.DecodeQualityLevel;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KakaduUtils;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataFactory;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.w3c.dom.Document;

import com.google.common.io.Files;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_codestream_source;
import kdu_jni.Jpx_input_box;
import kdu_jni.Jpx_layer_source;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_channel_mapping;
import kdu_jni.Kdu_codestream;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_quality_limiter;
import kdu_jni.Kdu_region_decompressor;

//TODO: manage, cache, ... individual frames (i.e. code streams) instead of whole movies

public abstract class Movie
{
	private final ConcurrentLinkedQueue<Jpx_input_box> openInputBoxes=new ConcurrentLinkedQueue<>();
	private ThreadLocal<Jpx_input_box> tlsJpx_input_box=new ThreadLocal<>();
	
	private final ArrayList<Kdu_region_decompressor> openKdu_region_decompressors = new ArrayList<Kdu_region_decompressor>(1);
	private ThreadLocal<Kdu_region_decompressor> tlsKdu_region_decompressor=ThreadLocal.withInitial(() ->
	{
		try
		{
			Kdu_region_decompressor decompressor = new Kdu_region_decompressor();
			decompressor.Set_interpolation_behaviour(0, 0);
			Kdu_quality_limiter q=new Kdu_quality_limiter(4f/256);
			decompressor.Set_quality_limiting(q, 0, 0);
			
			synchronized(openKdu_region_decompressors)
			{
				openKdu_region_decompressors.add(decompressor);
			}

			return decompressor;
		}
		catch (KduException e)
		{
			throw new RuntimeException(e);
		}
	});
	
	//FIXME: cache movies KduCache to disk
	//FIXME: clear KduCache on memory pressure, reload from disk
	//FIXME: index movies from disk on startup, reloading should happen on-demand
	//TODO: switch from localDateTime in metadata to long (unix epoch)

	private final ArrayList<Kdu_codestream> openKdu_codestreams = new ArrayList<Kdu_codestream>(1);
	private ThreadLocal<Kdu_codestream> tlsKdu_codestream=ThreadLocal.withInitial(() ->
		{
			try
			{
				Kdu_codestream codestream = new Kdu_codestream();
				synchronized(openKdu_codestreams)
				{
					openKdu_codestreams.add(codestream);
				}
				
				return codestream;
			}
			catch (Throwable e)
			{
				throw new RuntimeException(e);
			}
		});
	
	private final ArrayList<Jpx_source> openJpx_sources = new ArrayList<Jpx_source>(1);
	private ThreadLocal<Jpx_source> tlsJpx_source=ThreadLocal.withInitial(new Supplier<Jpx_source>()
	{
		@Override
		public Jpx_source get()
		{
			try
			{
				Jpx_source s = new Jpx_source();
				s.Open(family_src, true);
				
				synchronized(openJpx_sources)
				{
					openJpx_sources.add(s);
				}
				return s; 
			}
			catch (KduException e)
			{
				throw new RuntimeException(e);
			}
		}
	});
	
	protected MetaData[] metaDatas;
	
	public final int sourceId;
	
	public Movie(int _sourceId)
	{
		sourceId=_sourceId;
	}

	protected final Jp2_threadsafe_family_src family_src = new Jp2_threadsafe_family_src();
	private boolean disposed;
	
	public void dispose()
	{
		disposed=true;
		metaDatas=null;
		
		if(disposed)
			return;
		
		synchronized(openJpx_sources)
		{
			for(Jpx_source s:openJpx_sources)
			{
				try
				{
					s.Close();
				}
				catch (KduException e)
				{
					Telemetry.trackException(e);
				}
				s.Native_destroy();
			}
			openJpx_sources.clear();
		}
		
		synchronized(openKdu_codestreams)
		{
			for(Kdu_codestream c:openKdu_codestreams)
				try
				{
					c.Destroy();
				}
				catch (KduException e)
				{
					Telemetry.trackException(e);
				}
			openKdu_codestreams.clear();
			
		}
		
		synchronized(openKdu_region_decompressors)
		{
			for(Kdu_region_decompressor c:openKdu_region_decompressors)
				c.Native_destroy();
			openKdu_region_decompressors.clear();
		}
		
		for(;;)
		{
			Jpx_input_box box = openInputBoxes.poll();
			if(box==null)
				break;
			
			try
			{
				box.Close();
			}
			catch (KduException e)
			{
				Telemetry.trackException(e);
			}
			box.Native_destroy();
		}
		
		try
		{
			family_src.Close();
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
		family_src.Native_destroy();
	}
	
	public abstract boolean isBetterQualityThan(Movie _other);
	
	
	public abstract boolean isFullQuality();

	
	private synchronized void loadMetaData(int i)
	{
		if(metaDatas[i]!=null)
			return;
		
		try
		{
			if(!family_src.Is_codestream_main_header_complete(i))
				return;
			
			metaDatas[i]=MetaDataFactory.getMetaData(readMetadataDocument(i+1));
			if(metaDatas[i]==null)
				Telemetry.trackException(new UnsuitableMetaDataException("Cannot find metadata class for:\n"+KakaduUtils.getXml(family_src, i+1)));
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
	}
	
	public class Match
	{
		public final int index;
		public final long timeDifferenceSeconds;
		public final Movie movie;
		
		Match(int _index, long _timeDifferenceNanos)
		{
			index=_index;
			timeDifferenceSeconds=_timeDifferenceNanos;
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
			return index ^ Long.hashCode(timeDifferenceSeconds);
		}
		
		public boolean decodeImage(DecodeQualityLevel _quality, float _zoomFactor, Rectangle _requiredPixels, Texture _target)
		{
			return movie.decodeImage(index, _quality, _zoomFactor, _requiredPixels, _target);
		}
		
		public MetaData getMetaData()
		{
			return movie.getMetaData(index);
		}
	}
	
	@Nullable public Match findClosestIdx(@Nonnull LocalDateTime _currentDateTime)
	{
		int bestI=-1;
		long minDiff = Long.MAX_VALUE;
		
		for (int i = 0; i < metaDatas.length; i++)
		{
			loadMetaData(i);
			MetaData md=metaDatas[i];
			if(md==null)
				continue;
			
			LocalDateTime ldt=md.localDateTime;
			
			long curDiff = Math.abs(ChronoUnit.SECONDS.between(ldt, _currentDateTime));
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
	
	@Nullable public MetaData getAnyMetaData()
	{
		for(int i=0;i<metaDatas.length;i++)
		{
			loadMetaData(i);
			if(metaDatas[i]!=null)
				return metaDatas[i];
		}
		
		return null;
	}
	
	@Nullable
	public MetaData getMetaData(int idx)
	{
		loadMetaData(idx);
		return metaDatas[idx];
	}
	
	@Nullable
	public Document readMetadataDocument(int _index)
	{
		try
		{
			String xmlText = KakaduUtils.getXml(family_src, _index);
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
	
	public boolean decodeImage(int _index, DecodeQualityLevel _quality, float _zoomPercent, Rectangle _requiredRegion, Texture _target)
	{
		if(disposed)
			throw new IllegalStateException();
		
		_target.prepareUploadBuffer(_requiredRegion.width, _requiredRegion.height);
		
		try
		{
	        Jpx_input_box previousBox = tlsJpx_input_box.get();
	        if(previousBox!=null)
	        {
	        	previousBox.Close();
	        	previousBox.Native_destroy();
	        	tlsJpx_input_box.set(null);
	        	openInputBoxes.remove(previousBox);
	        }
	        
			Jpx_source jpxSrc=tlsJpx_source.get();
			
			Kdu_dims requestedBufferedRegion = new Kdu_dims();
			requestedBufferedRegion.Access_pos().Set_x(_requiredRegion.x);
			requestedBufferedRegion.Access_pos().Set_y(_requiredRegion.y);
			requestedBufferedRegion.Access_size().Set_x(_requiredRegion.width);
			requestedBufferedRegion.Access_size().Set_y(_requiredRegion.height);
			
			Kdu_region_decompressor decompressor = tlsKdu_region_decompressor.get();
			
	        Jpx_layer_source xlayer = jpxSrc.Access_layer(_index);
	        if(!xlayer.Exists())
	        	return false;
	        
	        Jpx_codestream_source xstream = jpxSrc.Access_codestream(xlayer.Get_codestream_id(0));
	        Jpx_input_box inputBox = xstream.Open_stream();
        	tlsJpx_input_box.set(inputBox);
        	openInputBoxes.add(inputBox);
        	
			Kdu_codestream codestream = tlsKdu_codestream.get();
        	if(!codestream.Exists())
        	{
				codestream.Create(inputBox);
				codestream.Set_persistent();
				codestream.Enable_restart();
        	}
        	else
        		codestream.Restart(inputBox);
			
			int discardLevels=(int)Math.round(-Math.log(_zoomPercent)/Math.log(2));
			
			Kdu_coords expand_numerator = new Kdu_coords(1,1);
			Kdu_coords expand_denominator = new Kdu_coords((int)Math.round(1/_zoomPercent/(1<<discardLevels)),(int)Math.round(1/_zoomPercent/(1<<discardLevels)));
			
			Kdu_channel_mapping mapping = new Kdu_channel_mapping();
			mapping.Configure(1 /* CHANNELS */, 8 /* BIT DEPTH */, false /* IS_SIGNED */);
			
			switch(_quality)
			{
				case QUALITY:
					decompressor.Set_quality_limiting(new Kdu_quality_limiter(1f/256), 300f*_zoomPercent, 300f*_zoomPercent);
					break;
				case PLAYBACK:
					decompressor.Set_quality_limiting(new Kdu_quality_limiter(4f/256), 300f*_zoomPercent, 300f*_zoomPercent);
					break;
				case SPEED:
					decompressor.Set_quality_limiting(new Kdu_quality_limiter(7f/256), 300f*_zoomPercent, 300f*_zoomPercent);
					break;
				case HURRY:
					decompressor.Set_quality_limiting(new Kdu_quality_limiter(10f/256), 300f*_zoomPercent, 300f*_zoomPercent);
					break;
				default:
					throw new RuntimeException("Unsupported quality");
			}
			
			decompressor.Start(codestream,
					mapping, /* MAPPING */
					0,
					discardLevels,
					16384 /* MAX LAYERS */,
					requestedBufferedRegion,
					expand_numerator,
					expand_denominator,
					false, /* PRECISE */
					Kdu_global.KDU_WANT_OUTPUT_COMPONENTS,
					true /* FASTEST */);
			
			Kdu_dims incompleteRegion = new Kdu_dims();
			incompleteRegion.Assign(requestedBufferedRegion);
			Kdu_dims new_region = new Kdu_dims();
			
			int position=0;
			while(decompressor.Process(_target.uploadBuffer.array(),
					new int[]{position} /* CHANNEL OFFSETS */,
					1 /* PIXEL GAP */,
					new Kdu_coords() /* BUFFER ORIGIN */,
					0 /* ROW GAP */,
					0 /* SUGGESTED INCREMENT */,
					_target.uploadBuffer.capacity()-position,
					incompleteRegion,
					new_region,
					8 /* PRECISION BITS */,
					true /* MEASURE ROW GAP IN PIXELS */,
					0 /* EXPAND MONOCHROME */,
					0 /* FILL ALPHA */,
					0 /* MAX COLOUR CHANNELS (0=no limit) */))
			{
				position+=new_region.Access_size().Get_x() * new_region.Access_size().Get_y();
				if(incompleteRegion.Access_size().Get_y() == 0)
					break;
			}
			
			decompressor.Finish();
			return true;
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
		return false;
	}


	public int getFrameCount()
	{
		return metaDatas.length;
	}
}
