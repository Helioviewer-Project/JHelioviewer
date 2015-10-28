package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.w3c.dom.Document;

//FIXME: clean out cache eventually
//FIXME: back with database to remember files from previous sessions
public class MovieCache
{
	private static HashMap<Integer,List<Movie>> cache=new HashMap<Integer, List<Movie>>();

	public static void add(Movie _movie)
	{
		List<Movie> al=cache.get(_movie.sourceId);
		if(al==null)
			cache.put(_movie.sourceId, al=new ArrayList<Movie>());
		
		if(al.contains(_movie))
			System.out.println("Cache already contains this movie.");
		else
			al.add(_movie);
	}

	@Nullable
	public static Match findBestFrame(int _sourceId, LocalDateTime _currentDate)
	{
		List<Movie> al=cache.get(_sourceId);
		if(al==null)
			return null;
		
		//TODO: use more efficient data structures
		Match bestMatch=null;
		for (Movie m : al)
		{
			Match curMatch=m.findClosestIdx(_currentDate);
			if(bestMatch==null)
				bestMatch=curMatch;
			else if(curMatch.timeDifferenceNanos<bestMatch.timeDifferenceNanos)
				bestMatch=curMatch;
			else if(curMatch.timeDifferenceNanos==bestMatch.timeDifferenceNanos && curMatch.movie.quality.ordinal()>bestMatch.movie.quality.ordinal())
				bestMatch=curMatch;
		}
		
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
}
