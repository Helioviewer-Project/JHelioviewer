package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.JHVUncaughtExceptionHandler;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.w3c.dom.Document;

import com.google.common.io.Files;

public class MovieCache
{
	private static HashMap<Integer,List<Movie>> cache= new HashMap<>();

	private static final long MAX_CACHE_SIZE = 1024*1024*1024;
	
	private static final File CACHE_DIR = new File(System.getProperty("java.io.tmpdir"), "jhv-movie-cache");
	
	static
	{
		try
		{
			Files.createParentDirs(CACHE_DIR);
			CACHE_DIR.mkdir();
			if(!CACHE_DIR.exists() || !CACHE_DIR.isDirectory())
				throw new IOException("Could not create cache in "+CACHE_DIR.getAbsolutePath());
		}
		catch (IOException e)
		{
			JHVUncaughtExceptionHandler.SINGLETON.uncaughtException(Thread.currentThread(), e);
		}
		
		limitCacheSize();
		ExitProgramAction.addShutdownHook(new Runnable()
		{
			@Override
			public void run()
			{
				limitCacheSize();
			}
		});
		
		if(Settings.getBoolean(Settings.BooleanKey.CACHE_LOADING_CRASHED))
		{
			try
			{
				for(File f:CACHE_DIR.listFiles())
					f.delete();
			}
			catch(Throwable _t)
			{
				Telemetry.trackException(_t);
			}
		}
		else
		{
			Settings.setBoolean(Settings.BooleanKey.CACHE_LOADING_CRASHED, true);
			for(File f:CACHE_DIR.listFiles())
			{
				String[] parts=f.getName().split(",");
				if(parts.length>=2)
					try
					{
						add(new Movie(Integer.parseInt(parts[0]),f.getAbsolutePath()));
					}
					catch(UnsuitableMetaDataException e)
					{
						Telemetry.trackException(new IOException("Cache: Could not load "+f.getName(),e));
						f.delete();
					}
					catch(NumberFormatException e)
					{
						Telemetry.trackException(new IOException("Cache: Unexpected file found "+f.getName(),e));
						f.delete();
					}
				else
				{
					Telemetry.trackException(new IOException("Cache: Unexpected file found "+f.getName()));
					f.delete();
				}
			}
		}
		Settings.setBoolean(Settings.BooleanKey.CACHE_LOADING_CRASHED, false);
	}
	
	private static void limitCacheSize()
	{
		File[] files=CACHE_DIR.listFiles();
		if(files==null)
			return;
		
		long cacheSize = 0;
		for(File f:files)
			cacheSize += f.length();
		
		if(cacheSize<=MAX_CACHE_SIZE)
			return;
		
		System.out.println("Cache: Too big, purging");

		//try to close all open movies, can't delete files otherwise
		for(List<Movie> movies:cache.values())
			for(Movie m:movies)
				m.dispose();
		
		Arrays.sort(files,0,files.length,new Comparator<File>()
		{
			@Override
			public int compare(@Nullable File _a, @Nullable File _b)
			{
				if(_a==null && _b==null)
					return 0;
				if(_a==null)
					return -1;
				if(_b==null)
					return 1;
				
				return Long.compare(_a.lastModified(), _b.lastModified());
			}
		});
		
		int toRemove = 0;
		while(cacheSize>MAX_CACHE_SIZE)
		{
			for(List<Movie> lm:cache.values())
				for(Movie m:lm)
					if(m.getBackingFile()!=null && new File(m.getBackingFile()).equals(files[toRemove]))
					{
						m.dispose();
						lm.remove(m);
						break;
					}
			
			cacheSize-=files[toRemove].length();
			if(!files[toRemove].delete())
				Telemetry.trackException(new IOException("Cache: Could not delete "+files[toRemove].getAbsolutePath()));
			else
				System.out.println("Cache: Removed "+files[toRemove].getAbsolutePath());
			
			toRemove++;
		}
	}
	
	public static void remove(Movie _movie)
	{
		List<Movie> al=cache.get(_movie.sourceId);
		if(al==null)
			return;
		
		if(!al.contains(_movie))
			System.out.println("Cache does not contain this movie.");
		else
			al.remove(_movie);
		
		_movie.dispose();
	}
	
	
	public static void add(Movie _movie)
	{
		List<Movie> al=cache.get(_movie.sourceId);
		if(al==null)
			cache.put(_movie.sourceId, al= new ArrayList<>());
		
		if(al.contains(_movie))
			System.out.println("Cache already contains this movie.");
		else
			al.add(_movie);
	}

	public static @Nullable Match findBestFrame(int _sourceId, LocalDateTime _currentDate)
	{
		List<Movie> al=cache.get(_sourceId);
		if(al==null)
			return null;
		
		//TODO: use more efficient data structures
		Match bestMatch=null;
		for (Movie m : al)
		{
			Match curMatch=m.findClosestIdx(_currentDate);
			if(curMatch==null)
				continue;
			
			if(bestMatch==null)
				bestMatch=curMatch;
			else if(curMatch.timeDifferenceSeconds<bestMatch.timeDifferenceSeconds)
				bestMatch=curMatch;
			else if(curMatch.timeDifferenceSeconds==bestMatch.timeDifferenceSeconds && curMatch.movie.quality.ordinal()>bestMatch.movie.quality.ordinal())
				bestMatch=curMatch;
		}
		
		if(bestMatch!=null && bestMatch.movie.getBackingFile()!=null)
			bestMatch.movie.touch();
		
		return bestMatch;
	}

	@Nullable
	public static ByteBuffer decodeImage(int _sourceId, LocalDateTime _localDateTime, int _quality, float _zoomFactor, Rectangle _requiredPixels)
	{
		Match bestMatch=findBestFrame(_sourceId, _localDateTime);
		if(bestMatch==null)
			return null;
		
		return bestMatch.movie.decodeImage(bestMatch.index, _quality, _zoomFactor, _requiredPixels);
	}

	@Nullable
	public static MetaData getMetaData(int _sourceId, LocalDateTime _currentDateTime)
	{
		Match match = findBestFrame(_sourceId, _currentDateTime);
		if (match == null)
			return null;
		
		return match.movie.getMetaData(match.index);
	}
	
	@Nullable
	public static Document getMetaDataDocument(int _sourceId, LocalDateTime _currentDateTime)
	{
		Match match = findBestFrame(_sourceId, _currentDateTime);
		if (match == null)
			return null;
		
		return match.movie.readMetadataDocument(match.index);
	}

	public static String generateFilename(int _sourceId)
	{
		return new File(CACHE_DIR, _sourceId+","+UUID.randomUUID().toString()).getAbsolutePath();
	}

	public static void init()
	{
	}
}
