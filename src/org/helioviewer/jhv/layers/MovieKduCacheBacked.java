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

public class MovieKduCacheBacked extends Movie
{
	public final @Nullable URI jpipURI;
	public final @Nullable Kdu_cache kduCache;
	public final AtomicInteger areaLimit;
	public final AtomicInteger qualityLayersLimit;

	//FIXME: add additional constructor to initialize a movie from cache, with correct downscaled & qualityLayers
	
	public MovieKduCacheBacked(int _sourceId, int _frameCount, URI _jpipURI)
	{
		super(_sourceId);
		kduCache = new Kdu_cache();
		
		try
		{
			family_src.Open(kduCache);
		}
		catch (KduException _e)
		{
			Telemetry.trackException(_e);
		}

		areaLimit = new AtomicInteger(0);
		qualityLayersLimit = new AtomicInteger(0);
		jpipURI = _jpipURI;
		metaDatas = new MetaData[_frameCount];
	}
	
	public boolean isFullQuality()
	{
		if(areaLimit.get()==Integer.MAX_VALUE && qualityLayersLimit.get()==Integer.MAX_VALUE)
			return true;
		
		MetaData md = getAnyMetaData();
		if(areaLimit.get() >= md.resolution.x*md.resolution.y)
			areaLimit.set(Integer.MAX_VALUE);
		
		return areaLimit.get()==Integer.MAX_VALUE && qualityLayersLimit.get()==Integer.MAX_VALUE;
	}
	
	public void notifyOfUpgradedQuality(int _area, int _qualityLayers)
	{
		for(;;)
		{
			int curArea = areaLimit.get();
			if(curArea>=_area)
				break;
			
			if(areaLimit.compareAndSet(curArea, _area))
				break;
		}
		
		for(;;)
		{
			int curQL = qualityLayersLimit.get();
			if(curQL>=_qualityLayers)
				break;
			
			if(qualityLayersLimit.compareAndSet(curQL, _qualityLayers))
				break;
		}
	}
	
	
	@Override
	public void dispose()
	{
		super.dispose();
		
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
	
	public boolean isBetterQualityThan(Movie _other)
	{
		if(!(_other instanceof MovieKduCacheBacked))
			return false;
		
		if(areaLimit.get()==((MovieKduCacheBacked)_other).areaLimit.get())
			return qualityLayersLimit.get()>((MovieKduCacheBacked)_other).qualityLayersLimit.get();
		
		return areaLimit.get()>((MovieKduCacheBacked)_other).areaLimit.get();
	}
	
	/*
	
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
	}*/
}
