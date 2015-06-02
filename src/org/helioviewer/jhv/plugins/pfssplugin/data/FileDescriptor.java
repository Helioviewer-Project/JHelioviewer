package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.time.LocalDateTime;

/**
 * Class Describing a pfss fits file on the server
 * 
 * this class is immutable
 */
public class FileDescriptor
{
	private final LocalDateTime startLocalDateTime;
	private final LocalDateTime endLocalDateTime;
	private final String fileName;
	
	public FileDescriptor(LocalDateTime startLocalDateTime, LocalDateTime endLocalDateTime, String fileName)
	{
		this.startLocalDateTime = startLocalDateTime;
		this.endLocalDateTime = endLocalDateTime;
		this.fileName = fileName;
	}
	
	/**
	 * 
	 * @param d
	 * @return true if date is after or equals the startdate and before or equals enddate
	 */
	public boolean isDateInRange(LocalDateTime currentLocalDateTime)
	{
		return (startLocalDateTime.isBefore(currentLocalDateTime) && endLocalDateTime.isAfter(currentLocalDateTime)) || startLocalDateTime.isEqual(currentLocalDateTime) || endLocalDateTime.isEqual(currentLocalDateTime);
	}
	
	public LocalDateTime getStartDate() {
		return this.startLocalDateTime;
	}
	
	public LocalDateTime getDateTime(){
		return endLocalDateTime;
	}
	
	public String getFileName() {
		return this.fileName;
	}
	
	@Override
	public int hashCode() {
		return endLocalDateTime.hashCode();
	}
	
	
}
