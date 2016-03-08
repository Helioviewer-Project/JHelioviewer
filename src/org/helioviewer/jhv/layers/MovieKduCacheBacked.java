package org.helioviewer.jhv.layers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.MovieCache;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

import com.google.common.io.Files;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

public class MovieKduCacheBacked extends Movie
{
	private static final int HEADER_MARKER = 0x01000a0f;
	
	public final @Nullable URI jpipURI;
	public final @Nullable Kdu_cache kduCache = new Kdu_cache();
	public final AtomicInteger areaLimit;
	public final AtomicInteger qualityLayersLimit;
	public final File backingFile;

	public MovieKduCacheBacked(int _sourceId, int _frameCount, URI _jpipURI) throws IOException
	{
		super(_sourceId);
		
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
		
		//TODO: check whether this actually works, is kept up to date, serialized to file, etc.
		timeMS = new long[_frameCount];
		
		backingFile = File.createTempFile(sourceId+"-jhv", null, MovieCache.CACHE_DIR);
		
		updateHeader();
	}
	
	public MovieKduCacheBacked(File _backingFile) throws IOException
	{
		super(Integer.parseInt(_backingFile.getName().split("-")[0]));
		
		try(DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(backingFile = _backingFile),65536)))
		{
			int header=dis.readInt();
			if(header!=HEADER_MARKER)
				throw new IOException("Invalid header");
			
			if(sourceId!=dis.readInt())
				throw new IOException("Invalid source id");
			
			try
			{
				jpipURI = new URI(dis.readUTF());
			}
			catch (URISyntaxException _e)
			{
				throw new IOException("Invalid URI",_e);
			}
			
			metaDatas = new MetaData[dis.readInt()];
			areaLimit = new AtomicInteger(dis.readInt());
			qualityLayersLimit = new AtomicInteger(dis.readInt());
			
			timeMS = new long[metaDatas.length];
			for(int i=0;i<timeMS.length;i++)
				timeMS[i]=dis.readLong();
			
			try
			{
				//TODO: load databins on-demand
				for(;;)
				{
					int kduClassId;
					try
					{
						kduClassId = dis.readInt();
					}
					catch(EOFException _eof)
					{
						break;
					}
					long codestreamId = dis.readLong();
					long binId = dis.readLong();
					byte[] data = new byte[dis.readInt()];
					if(data.length>0)
						dis.readFully(data);
					int offset = dis.readInt();
					boolean isFinal = dis.readBoolean();
					
					kduCache.Add_to_databin(kduClassId, codestreamId, binId, data, offset, data.length, isFinal, true, false);
				}
			}
			catch (KduException|IOException _e)
			{
				System.err.println("Cache file: Premature end");
				Telemetry.trackEvent("Cache file: Premature end");
			}
		}
		
		try
		{
			family_src.Open(kduCache);
		}
		catch (KduException _e)
		{
			throw new IOException("Could not open with Kakadu",_e);
		}
	}
	
	private void updateHeader()
	{
		synchronized(backingFile)
		{
			try(RandomAccessFile raf= new RandomAccessFile(backingFile, "rw"))
			{
				raf.seek(0);
				raf.writeInt(HEADER_MARKER); //10af header
				raf.writeInt(sourceId);
				raf.writeUTF(jpipURI.toString());
				raf.writeInt(metaDatas.length); //frames
				raf.writeInt(areaLimit.get());
				raf.writeInt(qualityLayersLimit.get());
				
				for(int i=0;i<timeMS.length;i++)
					raf.writeLong(timeMS[i]);
			}
			catch (IOException _e)
			{
				Telemetry.trackException(_e);
			}
		}
	}
	
	public boolean isFullQuality()
	{
		if(areaLimit.get()==Integer.MAX_VALUE && qualityLayersLimit.get()==Integer.MAX_VALUE)
			return true;
		
		MetaData md = getAnyMetaData();
		if(areaLimit.get() >= md.resolution.x*md.resolution.y)
		{
			areaLimit.set(Integer.MAX_VALUE);
			updateHeader();
		}
		
		return areaLimit.get()==Integer.MAX_VALUE && qualityLayersLimit.get()==Integer.MAX_VALUE;
	}
	
	public void addToDatabin(int _kduClassId, long _codestreamId, long _binId, byte[] _data, int _offset, int _length, boolean _isFinal)
	{
		if(disposed)
			throw new IllegalStateException("Disposed");
		
		try
		{
			kduCache.Add_to_databin(_kduClassId, _codestreamId, _binId, _data, _offset, _length, _isFinal, true, false);
			
			if(timeMS[(int)_codestreamId]==0)
			{
				loadMetaData((int)_codestreamId);
				if(timeMS[(int)_codestreamId]!=0)
					updateHeader();
			}
		}
		catch(KduException _e)
		{
			Telemetry.trackException(_e);
			return;
		}

		try(ByteArrayOutputStream baos = new ByteArrayOutputStream(_length+4+8+8+4+4+1+1))
		{
			try(DataOutputStream raf = new DataOutputStream(baos))
			{
				raf.writeInt(_kduClassId);
				raf.writeLong(_codestreamId);
				raf.writeLong(_binId);
				raf.writeInt(_length);
				if(_length>0)
					raf.write(_data, 0, _length);
				raf.writeInt(_offset);
				raf.writeBoolean(_isFinal);
			}
			
			synchronized(backingFile)
			{
				try(FileOutputStream fos=new FileOutputStream(backingFile,true))
				{
					fos.write(baos.toByteArray());
				}
			}
		}
		catch (IOException _e)
		{
			Telemetry.trackException(_e);
		}
	}
	
	public void notifyAboutUpgradedQuality(int _area, int _qualityLayers)
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
		
		updateHeader();
	}
	
	
	@Override
	public void dispose()
	{
		if(disposed)
			return;
		
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
		
		MovieKduCacheBacked other = (MovieKduCacheBacked)_other;
		if(areaLimit.get()==other.areaLimit.get())
			return qualityLayersLimit.get()>other.qualityLayersLimit.get();
		
		return areaLimit.get()>other.areaLimit.get();
	}
	
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
		if(System.currentTimeMillis()<=lastTouched+5000)
			return;
		
		lastTouched = System.currentTimeMillis();
		
		lruFileToucher.submit(() ->
			{
				try
				{
					Files.touch(backingFile);
				}
				catch (IOException e)
				{
					Telemetry.trackException(e);
				}
				
				return null;
			});
	}
}
