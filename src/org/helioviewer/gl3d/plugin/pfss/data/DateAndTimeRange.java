package org.helioviewer.gl3d.plugin.pfss.data;

import java.util.Date;

/**
 * Class Representing a range of time
 * 
 * this class is immutable
 * @author Jonas Schwammberger
 *
 */
public class DateAndTimeRange {

	private final Date startDate;
	private final Date endDate;
	
	public DateAndTimeRange(Date start, Date end) {
		this.startDate = (Date) start.clone();
		this.endDate = (Date)end.clone();
	}
	
	public boolean isInRange(Date d) {
		return startDate.before(d) & endDate.after(d);
	}
	
	
}
