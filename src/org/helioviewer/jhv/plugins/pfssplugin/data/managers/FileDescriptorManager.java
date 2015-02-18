package org.helioviewer.jhv.plugins.pfssplugin.data.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;

/**
 * Manages loading and accessing of FileDescriptor Objects
 * @author Jonas Schwammberger
 *
 */
public class FileDescriptorManager {
	private ArrayList<FileDescriptor> descriptors=new ArrayList<>();
	private Date firstDate;
	private Date endDate;
	
	public FileDescriptorManager()
	{
	}
	
	/**
	 * checks if Date is in Range of the FileDescriptor Manager
	 * @param d
	 * @return true if it is in range
	 */
	public boolean isDateInRange(Date d)
	{
	    if(firstDate==null || endDate==null)
	        return false;
	    
		return (firstDate.before(d) & endDate.after(d)) |  firstDate.equals(d) | endDate.equals(d);
	}
	
	/**
	 * Reads the file descriptions on the server from a range of dates
	 * @param from date of first file description to read
	 * @param to date of last file description to read
	 */
	public void readFileDescriptors(final Date from, final Date to) throws IOException
	{
		this.firstDate = null;
		this.endDate = null;
		
		Calendar currentCal = GregorianCalendar.getInstance();
		Calendar endCal = GregorianCalendar.getInstance();
		currentCal.setTime(from);
		endCal.setTime(to);

		int endYear = endCal.get(Calendar.YEAR);
		int endMonth = endCal.get(Calendar.MONTH);
		
		int currentYear = currentCal.get(Calendar.YEAR);
		int currentMonth = currentCal.get(Calendar.MONTH);
		
		synchronized(descriptors)
        {
	        descriptors.clear();
        }
		
		while(currentYear <= endYear && currentMonth <= endMonth) {
			String m = (currentMonth) < 9 ? "0" + (currentMonth + 1) : (currentMonth + 1) + "";
			final String url = PfssSettings.SERVER_URL + currentYear +"/"+m+"/list.txt";
			
			final int finalCurrentYear = currentYear;
            final int finalCurrentMonth = currentMonth;
			
			PfssPlugin.pool.execute(new Runnable()
	        {
	            @Override
	            public void run()
	            {
	                //readDescription(url, from, to, finalCurrentYear, finalCurrentMonth);
	            }
	        });
			
			readDescription(url, from, to, finalCurrentYear, finalCurrentMonth);
			
			currentCal.add(Calendar.MONTH, 1);
			currentYear = currentCal.get(Calendar.YEAR);
			currentMonth = currentCal.get(Calendar.MONTH);
		}
	}
	
	/**
	 * Reads the description of one month
	 * @param url
	 * @param from
	 * @param to
	 * @param currentYear
	 * @param currentMonth
	 * @throws IOException
	 */
    private void readDescription(String url,Date from, Date to,int currentYear, int currentMonth) throws IOException {
    	try {
			URL u = new URL(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			
			String dateString = null;
			String fileName= null;
			String line = null;
			while((line = in.readLine()) != null) {
				int split = line.indexOf(' ');
				dateString = line.substring(0,split);
				fileName = line.substring(split+1, line.length());
				
				//make dateString to date
				int t = dateString.indexOf('T');
				String[] date = dateString.substring(0,t).split("-");
				String[] time = dateString.substring(t+1,dateString.length()-1).split(":");
				Calendar cal = new GregorianCalendar(currentYear, currentMonth, Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]));
				Date endTime = cal.getTime();
				cal.add(Calendar.HOUR, -PfssSettings.FITS_FILE_D_HOUR);
				cal.add(Calendar.MINUTE, -PfssSettings.FITS_FILE_D_MINUTES);
				cal.add(Calendar.MILLISECOND, 1);// so there is exactly one millisecond difference between this and the next file descriptor
				Date startTime = cal.getTime();
					
				if(!(endTime.before(from) || startTime.after(to)))
				{
				    synchronized(descriptors)
				    {
				        descriptors.add(new FileDescriptor(startTime, endTime, fileName));
				    }
					
					if(this.firstDate == null)
					    this.firstDate = startTime;
					
					this.endDate = endTime;
				}
			}
		} 
		catch (MalformedURLException e)
		{
			//programming error
			e.printStackTrace();
		}
    	catch (IOException e)
		{
			throw new IOException("Unable to find data for: "+currentYear +"/"+(currentMonth+1),e);
		}
	}
    
    /**
     * Returns the Descriptor at Index
     * @param index
     * @return
     */
    public FileDescriptor getFileDescriptor(Date d)
    {
        synchronized(descriptors)
        {
            return descriptors.get(Collections.binarySearch(descriptors, d));
        }
    }
    
	/**
	 * Returns the following FileDescriptor
	 * @param current 
	 * @return
	 */
	public FileDescriptor getNext(FileDescriptor current)
	{
	    synchronized(descriptors)
	    {
    		int index = descriptors.indexOf(current);
    		index = ++index % descriptors.size();
    		return descriptors.get(index);
	    }
	}
	
}
