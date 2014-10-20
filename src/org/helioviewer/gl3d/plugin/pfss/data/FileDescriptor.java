package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Class Representing a range of time
 * 
 * this class is immutable
 * @author Jonas Schwammberger
 *
 */
public class FileDescriptor {
	private final Calendar startCal;
	private final Date startDate;
	private final Date endDate;
	private final String fileName;
	
	public FileDescriptor(Date start, Date end, String fileName) {
		this.startDate = (Date) start.clone();
		this.endDate = (Date)end.clone();
		this.startCal = GregorianCalendar.getInstance();
		startCal.setTime(start);
		this.fileName = fileName;
	}
	
	public boolean isDateInRange(Date d) {
		return (startDate.before(d) & endDate.after(d)) |  startDate.equals(d) | endDate.equals(d);
	}
	
	public int getYear() {
		return startCal.get(Calendar.YEAR);
	}
	
	public int getMonth() {
		return startCal.get(Calendar.MONTH);
	}
	
	public int getDay() {
		return startCal.get(Calendar.DAY_OF_MONTH);
	}
	
	public int getHour() {
		return startCal.get(Calendar.HOUR_OF_DAY);
	}
	
	public String getFileName() {
		return this.fileName;
	}
	
	
}
