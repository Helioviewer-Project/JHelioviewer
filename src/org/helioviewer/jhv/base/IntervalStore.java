package org.helioviewer.jhv.base;

import java.time.LocalDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;

public class IntervalStore<T>
{
	private TreeMap<T, Boolean> times=new TreeMap<>();
	
	public IntervalStore()
	{
	}
	
	public void addInterval(T _from, T _to)
	{
		times.subMap(_from, _to).clear();
		times.put(_from, true);
		times.put(_to, false);
	}
	
	public boolean fullyContains(T _from, T _to)
	{
		if(!contains(_from))
			return false;
		
		return !times.subMap(_from, _to).containsValue(false);
	}

	
	public boolean contains(T _query)
	{
		Entry<T,Boolean> e=times.floorEntry(_query);
		if(e==null)
			return false;
		
		return e.getValue();
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		boolean open=false;
		for(Entry<T,Boolean> t:times.entrySet())
			if(t.getValue()!=open)
			{
				sb.append(t.getKey());
				if(!open)
					sb.append("-");
				else
					sb.append("\n");
				
				open=t.getValue();
			}
	
		return sb.toString();
	}
}
