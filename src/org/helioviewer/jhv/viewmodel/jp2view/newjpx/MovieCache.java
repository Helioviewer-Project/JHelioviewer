package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.helioviewer.jhv.layers.Movie;

//FIXME: clean out cache eventually
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

	public static Movie get(int _sourceId, LocalDateTime _currentDate)
	{
		List<Movie> al=cache.get(_sourceId);
		if(al==null)
			return null;
		
		for (Movie m : al)
			if (m.contains(_sourceId, _currentDate))
				return m;
		
		return null;
	}
}
