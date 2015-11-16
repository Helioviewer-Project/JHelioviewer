package org.helioviewer.jhv.base;

import java.time.LocalDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;

public class IntervalStore
{
	private TreeMap<LocalDateTime, Boolean> times=new TreeMap<>();
	
	public IntervalStore()
	{
	}
	
	public void addInterval(LocalDateTime _from, LocalDateTime _to)
	{
		times.subMap(_from, _to).clear();
		times.put(_from, true);
		times.put(_to, false);
	}
	
	public boolean contains(LocalDateTime _query)
	{
		Entry<LocalDateTime,Boolean> e=times.floorEntry(_query);
		if(e==null)
			return false;
		
		return e.getValue();
	}
}
