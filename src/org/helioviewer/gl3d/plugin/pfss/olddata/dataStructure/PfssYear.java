package org.helioviewer.gl3d.plugin.pfss.olddata.dataStructure;

import java.util.HashMap;


/**
 * Bean for year
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssYear {
	private int year;
	private HashMap<Integer, PfssMonth> months;

	public PfssYear(int year) {
		this.year = year;
		this.months = new HashMap<Integer, PfssMonth>();
	}

	public synchronized PfssDayAndTime addMonth(int year, int month, int dayAndTime,
			String url) {
	  
		if (!months.containsKey(month))
			months.put(month, new PfssMonth(month));
		return months.get(month).addDayAndTime(year, month, dayAndTime, url);
	}

	public synchronized PfssDayAndTime findData(int month, int dayAndTime) {
		if (months.containsKey(month)) {
			return months.get(month).findData(dayAndTime);
		}
		return null;
	}

	@Override
	public String toString() {
		return year + "";
	}
}
