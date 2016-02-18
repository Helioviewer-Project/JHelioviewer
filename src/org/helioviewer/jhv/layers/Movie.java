package org.helioviewer.jhv.layers;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
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

public class Movie
{
	//TODO: LEAKING private final ArrayList<Jpx_input_box> openJpx_input_box = new ArrayList<Jpx_input_box>(1);
	private ThreadLocal<Jpx_input_box> tlsJpx_input_box=new ThreadLocal<>();
	
	private final ArrayList<Kdu_region_decompressor> openKdu_region_decompressors = new ArrayList<Kdu_region_decompressor>(1);
	private ThreadLocal<Kdu_region_decompressor> tlsKdu_region_decompressor=ThreadLocal.withInitial(new Supplier<Kdu_region_decompressor>()
	{
		@Override
		public Kdu_region_decompressor get()
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
		}
	});

	private final ArrayList<Kdu_codestream> openKdu_codestreams = new ArrayList<Kdu_codestream>(1);
	private ThreadLocal<Kdu_codestream> tlsKdu_codestream=ThreadLocal.withInitial(new Supplier<Kdu_codestream>()
	{
		@Override
		public Kdu_codestream get()
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
	
	/*
	private final ArrayList<Kdu_region_compositor> openCompositors = new ArrayList<Kdu_region_compositor>(1);
	private ThreadLocal<Kdu_region_compositor> tlsCompositors=ThreadLocal.withInitial(new Supplier<Kdu_region_compositor>()
	{
		@Override
		public Kdu_region_compositor get()
		{
			try
			{
				Kdu_region_compositor compositor = new Kdu_region_compositor(tlsJpx_source.get());
				compositor.Set_surface_initialization_mode(false);
				
				//T-ODO: enable multi-threaded decoding?
				//Kdu_thread_env env = new Kdu_thread_env();
				//env.Create();
				//for(int i=0;i<Runtime.getRuntime().availableProcessors();i++)
				//	env.Add_thread();
				//
				//compositor.Set_thread_env(env, null);
				
				synchronized(openCompositors)
				{
					openCompositors.add(compositor);
				}
				return compositor; 
			}
			catch (KduException e)
			{
				throw new RuntimeException(e);
			}
		}
	});*/
	
	@Nullable private MetaData[] metaDatas;
	
	public final int sourceId;
	@Nullable private final String filename;
	
	public enum Quality
	{
		PREVIEW, FULL
	}
	
	public final Quality quality;
	
	private final @Nullable Kdu_cache kduCache;
	private final Jp2_threadsafe_family_src family_src;
	private boolean disposed;
	
	private long lastTouched;
	
	private static final ExecutorService lruFileToucher = Executors.newSingleThreadExecutor(new ThreadFactory()
	{
		@Override
		public Thread newThread(@Nullable Runnable r)
		{
			Thread t = new Thread(r);
			t.setName("LRU file toucher");
			t.setDaemon(true);
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		}
	});
	
	public void touch()
	{
		if(filename==null)
			throw new IllegalArgumentException("filename == null");
		
		if(System.currentTimeMillis()<=lastTouched+5000)
			return;
		
		lastTouched = System.currentTimeMillis();
		
		lruFileToucher.submit(new Callable<Object>()
		{
			@Override
			public @Nullable Object call() throws Exception
			{
				try
				{
					Files.touch(new File(filename));
				}
				catch (IOException e)
				{
					Telemetry.trackException(e);
				}
				
				return null;
			}
		});
	}
	
	
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
		
		Jpx_input_box box = tlsJpx_input_box.get();
		if(box!=null)
		{
			try
			{
				tlsJpx_input_box.get().Close();
			}
			catch (KduException e)
			{
				Telemetry.trackException(e);
			}
			tlsJpx_input_box.get().Native_destroy();
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
		
		if(kduCache!=null)
		{
			try
			{
				kduCache.Close();
			}
			catch (KduException e)
			{
				Telemetry.trackException(e);
			}
			kduCache.Native_destroy();
		}
	}
	
	public Movie(int _sourceId, String _filename)
	{
		sourceId = _sourceId;
		quality = Quality.FULL;
		family_src = new Jp2_threadsafe_family_src();
		filename = _filename;
		kduCache = null;
		
		try
		{
			family_src.Open(filename);
			processFamilySrc();
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
		
		if(getAnyMetaData()==null)
			throw new UnsuitableMetaDataException();
	}
	
	public Movie(int _sourceId, Kdu_cache _kduCache)
	{
		sourceId = _sourceId;
		quality = Quality.PREVIEW;
		family_src = new Jp2_threadsafe_family_src();
		kduCache = _kduCache;
		filename = null;
		
		try
		{
			family_src.Open(kduCache);
			processFamilySrc();
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
		
		if(getAnyMetaData()==null)
			throw new UnsuitableMetaDataException();
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
				
				if(metaDatas[i]==null)
					Telemetry.trackException(new UnsuitableMetaDataException("Cannot find metadata class for:\n"+KakaduUtils.getXml(family_src, i+1)));
				
				//TODO: should invalidate textureCache
				//TextureCache.invalidate(sourceId, metaDatas[i].getLocalDateTime());
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
	
	public int getFrameCount()
	{
		if(metaDatas!=null)
			return metaDatas.length;
		
		return 0;
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
			
			//TODO: limit discard levels to actually available DWT levels (codestream.Get_min_dwt_levels() ?)
			
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
			
			/*
			Kdu_region_compositor compositor = tlsCompositors.get();
			
			//F-IXME: downgrade quality first, before resolution when having speed problems
			compositor.Set_max_quality_layers(_quality);
			compositor.Set_scale(false, false, false, _zoomPercent);
			compositor.Set_buffer_surface(requestedBufferedRegion);
			
			compositor.Remove_ilayer(new Kdu_ilayer_ref(), false);
			
			//T-ODO: save more memory
			compositor.Cull_inactive_ilayers(800);
			compositor.Add_ilayer(_index, new Kdu_dims(), new Kdu_dims());
			
			Kdu_dims actualBufferedRegion = new Kdu_dims();
			Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(actualBufferedRegion);
			Kdu_coords actualOffset = new Kdu_coords();
			actualOffset.Assign(actualBufferedRegion.Access_pos());
			
			int[] region_buf = tlsBuffer.get();
			Kdu_dims newRegion = new Kdu_dims();
			while (compositor.Process(INITIAL_BUFFER_LENGTH-_requiredRegion.width, newRegion))
			{
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(actualOffset);
				
				int newPixels = newSize.Get_x() * newSize.Get_y();
				if (newPixels > 0)
				{
					if(newPixels > region_buf.length)
					{
						System.err.println("Kakadu acting up: Spec of Compositor.Process() guarantees less pixels.");
						tlsBuffer.set(region_buf=new int[newPixels]);
					}
					
					compositorBuf.Get_region(newRegion, region_buf);
					for(int i=0;i<newPixels;i++)
						_result.put((byte)region_buf[i]);
				}
			}
			
			//compositorBuf.Native_destroy();
			_result.position(0);
			return true;*/
		}
		catch (KduException e)
		{
			Telemetry.trackException(e);
		}
		return false;
	}
}
