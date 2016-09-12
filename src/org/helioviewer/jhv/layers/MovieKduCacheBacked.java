package org.helioviewer.jhv.layers;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.IntervalStore;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPDatabinClass;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.MovieCache;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_compressed_source_nonnative;
import kdu_jni.Kdu_global;
import sun.misc.Cleaner;
import sun.nio.ch.DirectBuffer;

public class MovieKduCacheBacked extends Movie
{
	private static final int HEADER_MARKER = 0x01100a0f;
	private static final int VALID_MARKER = 0x00011a0f;
	
	private final Kdu_cache kduCache = new Kdu_cache();
	
	public final @Nullable URI jpipURI;
	public final AtomicInteger areaLimit;
	public final AtomicInteger qualityLayersLimit;
	public final File backingFile;
	private final HashMap<Long,List<Integer>> codestreamPositions=new HashMap<>();
	private MappedByteBuffer map;
	private int usedSize;
	private final LinkedHashMap<Long,LinkedHashMap<Long,List<Bin>>> bins=new LinkedHashMap<>();

	private boolean updateBinInfo(int _kduClassId, long _codestreamId, long _binId, int _offset, int _length, boolean _isFinal)
	{
		if(_length==0 && !_isFinal)
			return false;
		
		LinkedHashMap<Long,List<Bin>> codestreamBins;
		if(bins.containsKey(_codestreamId))
			codestreamBins=bins.get(_codestreamId);
		else
			bins.put(_codestreamId,codestreamBins=new LinkedHashMap<>());
		
		List<Bin> idBins;
		if(codestreamBins.containsKey(_binId))
			idBins=codestreamBins.get(_binId);
		else
			codestreamBins.put(_binId, idBins=new ArrayList<>(1));
		
		Bin b=null;
		for(Bin c:idBins)
			if(c.Class==_kduClassId)
			{
				b=c;
				break;
			}
		
		boolean somethingChanged=false;
		
		if(b==null)
		{
			idBins.add(b=new Bin(_kduClassId,_codestreamId,_binId));
			somethingChanged=true;
		}
		
		if(_length==0 && b.complete)
			return false;
		
		if(b.bufLength<_offset+_length)
		{
			b.bufLength=_offset+_length;
			somethingChanged=true;
		}
		
		
		if(!b.available.fullyContains(_offset, _offset+_length))
		{
			//System.out.println("Adding interval "+_offset+" - "+(_offset+_length)+"   to "+_codestreamId+"/"+_kduClassId+"/"+_binId);
			b.available.addInterval(_offset, _offset+_length);
			somethingChanged=true;
		}
		
		if(_isFinal && !b.complete)
		{
			somethingChanged=true;
			b.complete=true;
		}
		
		return somethingChanged;
	}
	

	public MovieKduCacheBacked(int _sourceId, int _frameCount, URI _jpipURI) throws IOException
	{
		super(_sourceId);
		
		areaLimit = new AtomicInteger(0);
		qualityLayersLimit = new AtomicInteger(0);
		jpipURI = _jpipURI;
		metaDatas = new MetaData[_frameCount];
		
		timeMS = new long[_frameCount];
		
		backingFile = File.createTempFile(sourceId+"-jhv", null, MovieCache.CACHE_DIR);
		map=Files.map(backingFile, MapMode.READ_WRITE);
		usedSize=0;
		
		try
		{
			family_src.Open(kduCache);
		}
		catch (KduException _e)
		{
			Telemetry.trackException(_e);
		}

		updateHeader();
	}
	
	
	
	private static final ScheduledExecutorService codestreamCacheReaper = Executors.newScheduledThreadPool(0,new ThreadFactory()
	{
		@Override
		public Thread newThread(@Nullable Runnable r)
		{
			Thread t = new Thread(r);
			t.setName("Codestream cache reaper");
			t.setDaemon(true);
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		}
	});
	
	private HashMap<Long,ScheduledFuture<?>> removeCachedCodestream=new HashMap<>();
	private HashMap<Long,Integer> cachedCodestreams=new HashMap<>();
	
	protected synchronized void unloadCodestreamFromCache(final long _codestreamId) throws Exception
	{
		int newCounter = cachedCodestreams.get(_codestreamId)-1;
		cachedCodestreams.put(_codestreamId, newCounter);
		
		if(newCounter>0)
			return;
		
		if(newCounter<0)
			throw new Exception();
		
		if(removeCachedCodestream.containsKey(_codestreamId))
			removeCachedCodestream.get(_codestreamId).cancel(false);
		
		removeCachedCodestream.put(_codestreamId, 
			codestreamCacheReaper.schedule(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized(MovieKduCacheBacked.this)
					{
						if(!removeCachedCodestream.containsKey(_codestreamId))
							return;
						
						//System.out.println("Removing stream "+_codestreamId);
						cachedCodestreams.remove(_codestreamId);
						removeCachedCodestream.remove(_codestreamId);
						try
						{
							kduCache.Delete_stream_class(JPIPDatabinClass.PRECINCT_DATABIN.getKakaduClassID(), _codestreamId);
							//kduCache.Trim_to_preferred_memory_limit();
						}
						catch(KduException _e)
						{
							Telemetry.trackException(_e);
						}
					}
				}
			}, 20, TimeUnit.SECONDS));
	}
	
	protected synchronized void loadCodestreamIntoCache(long _codestreamId) throws Exception
	{
		if(cachedCodestreams.getOrDefault(_codestreamId,0)>0)
		{
			cachedCodestreams.put(_codestreamId, cachedCodestreams.get(_codestreamId)+1);
			return;
		}
		
		cachedCodestreams.put(_codestreamId, 1);
		
		if(removeCachedCodestream.containsKey(_codestreamId))
		{
			boolean cancelled=removeCachedCodestream.get(_codestreamId).cancel(false);
			if(cancelled)
				return;
		}
		
		if(codestreamPositions.containsKey(_codestreamId))
			synchronized(backingFile)
			{
				try
				{
					//System.out.println("Loading "+_codestreamId+"  with "+codestreamPositions.get(_codestreamId).size()+" positions");
					for(int pos:codestreamPositions.get(_codestreamId))
					{
						map.position(pos);
						
						int kduClassId = map.getInt();
						long codestreamId = map.getLong();
						if(codestreamId!=_codestreamId || kduClassId!=JPIPDatabinClass.PRECINCT_DATABIN.getKakaduClassID())
							continue;
						
						long binId = map.getLong();
						byte[] data = new byte[map.getInt()];
						if(data.length>0)
							map.get(data);
						int offset = map.getInt();
						boolean isFinal = map.get()!=0;
						//System.out.println("           Len "+data.length+"     offset "+offset+"    id "+binId+"      Way 1: "+Arrays.toString(data));
						
						if(map.getInt()!=VALID_MARKER)
							throw new Exception("Invalid marker");
						
						kduCache.Add_to_databin(kduClassId, codestreamId, binId, data, offset, data.length, isFinal, true, false);
					}
				}
				catch (KduException _e)
				{
					System.err.println("Cache file: Premature end");
					Telemetry.trackEvent("Cache file: Premature end");
				}
			}
	}
	
	public MovieKduCacheBacked(File _backingFile) throws IOException
	{
		super(Integer.parseInt(_backingFile.getName().split("-")[0]));
		
		backingFile = _backingFile;
		map=Files.map(backingFile, MapMode.READ_WRITE);
		
		try
		{
			int header=map.getInt();
			if(header!=HEADER_MARKER)
				throw new IOException("Invalid header");
			
			if(sourceId!=map.getInt())
				throw new IOException("Invalid source id");
			
			usedSize=map.getInt();
			if(usedSize>map.limit())
				throw new IOException("Invalid size in header");
			
			try
			{
				byte[] strBytes = new byte[map.getShort()];
				map.get(strBytes);
				jpipURI = new URI(new String(strBytes, Charsets.UTF_8));
			}
			catch (URISyntaxException _e)
			{
				throw new IOException("Invalid URI",_e);
			}
			
			metaDatas = new MetaData[map.getInt()];
			areaLimit = new AtomicInteger(map.getInt());
			qualityLayersLimit = new AtomicInteger(map.getInt());
			
			timeMS = new long[metaDatas.length];
			for(int i=0;i<timeMS.length;i++)
				timeMS[i]=map.getLong();
			
			for(;;)
			{
				if(map.position()==usedSize)
					break;
				
				int startPos = map.position();
				
				
				int kduClassId = map.getInt();
				long codestreamId = map.getLong();
				long binId = map.getLong();
				int dataLength = map.getInt();
				byte[] data = null;
				if(dataLength>0)
				{
					if(kduClassId!=JPIPDatabinClass.PRECINCT_DATABIN.getKakaduClassID())
					{
						data = new byte[dataLength];
						map.get(data);
					}
					else
					{
						map.position(map.position()+dataLength);
					}
				}
				int offset = map.getInt();
				boolean isFinal = map.get()!=0;
				
				if(map.getInt()!=VALID_MARKER)
				{
					Telemetry.trackException(new Exception("Invalid marker - shortening file from "+usedSize+" by "+(usedSize-startPos)));
					usedSize=startPos;
					updateHeader();
					break;
				}
				
				if(kduClassId!=JPIPDatabinClass.PRECINCT_DATABIN.getKakaduClassID())
					try
					{
						kduCache.Add_to_databin(kduClassId, codestreamId, binId, data, offset, dataLength, isFinal, true, false);
					}
					catch(KduException _e)
					{
						Telemetry.trackException(new Exception("KduException "+_e+" - shortening file from "+usedSize+" by "+(usedSize-startPos)));
						usedSize=startPos;
						updateHeader();
						break;
					}
				else
				{
					List<Integer> positions=codestreamPositions.get(codestreamId);
					if(positions==null)
						codestreamPositions.put(codestreamId, positions=new ArrayList<Integer>());
					
					positions.add(startPos);
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
		catch(Exception _e)
		{
			unmap();
			throw _e;
		}
	}
	
	
	
	
	static class Bin
	{
		int Class;
		long Codestream;
		long Id;
		int bufLength;
		boolean complete;
		IntervalStore<Integer> available=new IntervalStore<>();
		
		Bin(int _class,long _codestream,long _id)
		{
			Class=_class;
			Codestream=_codestream;
			Id=_id;
		}
	}
	
	public LinkedHashMap<Long, ArrayList<String>> getCachedDatabins() throws KduException
	{
		LinkedHashMap<Long,ArrayList<String>> cacheContents = new LinkedHashMap<>();
		for(long codestreamId:bins.keySet())
		{
			cacheContents.put(codestreamId, new ArrayList<>());
			
			for(List<Bin> codestreamBins:bins.get(codestreamId).values())
				for(Bin b:codestreamBins)
				{
					JPIPDatabinClass c=JPIPDatabinClass.fromKduClassID(b.Class);
					String element = c.getJpipString();
					if(c!=JPIPDatabinClass.MAIN_HEADER_DATABIN)
						element += String.valueOf(b.Id);
					
					if(!b.complete)
						element += ":"+b.bufLength;
					
					cacheContents.get(codestreamId).add(element);
				}
		}
		return cacheContents;
	}
	
	private void ensureSize(int newSize) throws IOException
	{
		if(newSize<=usedSize)
			return;
		
		usedSize=newSize;
		if(usedSize<=map.limit())
			return;
		
		try(RandomAccessFile raf=new RandomAccessFile(backingFile, "rw"))
		{
			raf.setLength(usedSize+(usedSize>>1)+128*1024);
		}
		
		map=Files.map(backingFile,MapMode.READ_WRITE);
	}
	
	private void updateHeader() throws IOException
	{
		byte[] strBytes=jpipURI.toString().getBytes(Charsets.UTF_8);
		synchronized(backingFile)
		{
			int reqSpace = 4+4+4+2+strBytes.length+4+4+4+timeMS.length*8;
			ensureSize(reqSpace);
			
			map.position(0);
			map.putInt(HEADER_MARKER); //10af header
			map.putInt(sourceId);
			
			map.putInt(usedSize);
			
			map.putShort((short)strBytes.length);
			map.put(strBytes);
			
			map.putInt(metaDatas.length); //frames
			map.putInt(areaLimit.get());
			map.putInt(qualityLayersLimit.get());
			
			for(int i=0;i<timeMS.length;i++)
				map.putLong(timeMS[i]);
		}
	}
	
	public boolean isFullQuality()
	{
		if(areaLimit.get()==Integer.MAX_VALUE && qualityLayersLimit.get()==Integer.MAX_VALUE)
			return true;
		
		MetaData md = getAnyMetaData();
		if(md==null)
			return false;
		
		if(areaLimit.get() >= md.resolution.x*md.resolution.y && areaLimit.get()!=Integer.MAX_VALUE)
		{
			areaLimit.set(Integer.MAX_VALUE);
			try
			{
				updateHeader();
			}
			catch(IOException _ioe)
			{
				Telemetry.trackException(_ioe);
			}
		}
		
		return areaLimit.get()==Integer.MAX_VALUE && qualityLayersLimit.get()==Integer.MAX_VALUE;
	}
	
	public synchronized void addToDatabin(int _kduClassId, long _codestreamId, long _binId, byte[] _data, int _offset, int _length, boolean _isFinal)
	{
		if(disposed)
			throw new IllegalStateException("Disposed");
		
		try
		{
			if(_kduClassId!=JPIPDatabinClass.PRECINCT_DATABIN.getKakaduClassID() || cachedCodestreams.containsKey(_codestreamId))
				kduCache.Add_to_databin(_kduClassId, _codestreamId, _binId, _data, _offset, _length, _isFinal, true, false);
			
			if(timeMS[(int)_codestreamId]==0)
			{
				loadMetaData((int)_codestreamId);
				if(timeMS[(int)_codestreamId]!=0)
					updateHeader();
			}
		}
		catch(Exception _e)
		{
			Telemetry.trackException(_e);
			return;
		}
		
		if(!updateBinInfo(_kduClassId, _codestreamId, _binId, _offset, _length, _isFinal))
			return;
		
		try(ByteArrayOutputStream baos = new ByteArrayOutputStream(_length+4+8+8+4+4+1+1))
		{
			try(DataOutputStream dos = new DataOutputStream(baos))
			{
				dos.writeInt(_kduClassId);
				dos.writeLong(_codestreamId);
				dos.writeLong(_binId);
				dos.writeInt(_length);
				if(_length>0)
					dos.write(_data, 0, _length);
				dos.writeInt(_offset);
				dos.writeByte(_isFinal?(byte)1:(byte)0);
				dos.writeInt(VALID_MARKER);
			}
			
			byte[] data=baos.toByteArray();
			
			
			//TODO: defragment/compact periodically
			synchronized(backingFile)
			{
				if(_kduClassId==JPIPDatabinClass.PRECINCT_DATABIN.getKakaduClassID())
				{
					List<Integer> positions=codestreamPositions.get(_codestreamId);
					if(positions==null)
						codestreamPositions.put(_codestreamId, positions=new ArrayList<Integer>());
					
					positions.add(usedSize);
				}
				
				int oldLimit=usedSize;
				ensureSize(oldLimit+data.length);
				map.position(oldLimit);
				map.put(data);
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
		
		try
		{
			updateHeader();
		}
		catch(IOException _ioe)
		{
			Telemetry.trackException(_ioe);
		}
	}
	
	private void unmap()
	{
		synchronized(backingFile)
		{
			//see also https://sourceforge.net/p/tuer/code/HEAD/tree/pre_beta/src/main/java/engine/misc/DeallocationHelper.java
			@Nullable Cleaner c=((DirectBuffer)map).cleaner();
			if(c!=null)
				c.clean();
		}
	}
	
	
	@Override
	public void dispose()
	{
		if(disposed)
			return;
		
		super.dispose();
		
		unmap();
		synchronized(this)
		{
			for(long codestreamId:removeCachedCodestream.keySet())
				removeCachedCodestream.get(codestreamId).cancel(false);
			
			removeCachedCodestream.clear();
		}
		
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
	
	private volatile long lastTouched;
	
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
	
	private AtomicBoolean touching=new AtomicBoolean();
	
	public void touch()
	{
		if(System.currentTimeMillis()<=lastTouched+60000)
			return;
		
		if(touching.compareAndSet(false, true))
			lruFileToucher.submit(() ->
				{
					map.force();
					backingFile.setLastModified(System.currentTimeMillis());
					lastTouched = System.currentTimeMillis();
					touching.lazySet(false);
					return null;
				});
	}
}
