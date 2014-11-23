package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Class Describing a pfss fits file on the server
 * 
 * this class is immutable
 * @author Jonas Schwammberger
 *
 */
public class FileDescriptor implements Comparable<Date> {
	private final Calendar endCal;
	private final Date startDate;
	private final Date endDate;
	private final String fileName;
	
	public FileDescriptor(Date start, Date end, String fileName) {
		this.startDate = (Date) start.clone();
		this.endDate = (Date)end.clone();
		this.endCal = GregorianCalendar.getInstance();
		endCal.setTime(end);
		this.fileName = fileName;
	}
	
	/**
	 * 
	 * @param d
	 * @return true if date is after or equals the startdate and before or equals enddate
	 */
	public boolean isDateInRange(Date d) {
		return (startDate.before(d) & endDate.after(d)) |  startDate.equals(d) | endDate.equals(d);
	}
	
	public Date getStartDate() {
		return this.startDate;
	}
	
	public int getYear() {
		return endCal.get(Calendar.YEAR);
	}
	
	public int getMonth() {
		return endCal.get(Calendar.MONTH);
	}
	
	public String getFileName() {
		return this.fileName;
	}

	@Override
	public int compareTo(Date o) {
		if(this.isDateInRange(o))
			return 0;
		else
			return this.endDate.compareTo(o);
	}
	
	
}
