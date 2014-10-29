package org.helioviewer.gl3d.plugin.pfss.data.managers;

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

import org.helioviewer.gl3d.plugin.pfss.data.FileDescriptor;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;


public class FileDescriptorManager {
	private ArrayList<FileDescriptor> descriptors;
	
	public FileDescriptorManager() {
		
	}
	
	/**
	 * Reads the file descriptions on the server from a range of dates
	 * @param from date of first file description to read
	 * @param to date of last file description to read
	 */
	public void readFileDescriptors(Date from, Date to) throws IOException {
		Calendar currentCal = GregorianCalendar.getInstance();
		Calendar endCal = GregorianCalendar.getInstance();
		currentCal.setTime(from);
		endCal.setTime(to);

		int endYear = endCal.get(Calendar.YEAR);
		int endMonth = endCal.get(Calendar.MONTH);
		
		int currentYear = currentCal.get(Calendar.YEAR);
		int currentMonth = currentCal.get(Calendar.MONTH);
		
		descriptors = new ArrayList<>((endMonth-currentMonth)+1* 125); //heuristic: for each month, there are about 125 fits files.
		
		while(currentYear <= endYear && currentMonth <= endMonth) {
			String m = (currentMonth) < 9 ? "0" + (currentMonth + 1)
					: (currentMonth + 1) + "";
			String url = PfssSettings.SERVER_URL + currentYear +"/"+m+"/list.txt";
			this.readDescription(url, from, to,currentYear,currentMonth);
			
			currentCal.add(Calendar.MONTH, 1);
			currentYear = currentCal.get(Calendar.YEAR);
			currentMonth = currentCal.get(Calendar.MONTH);
		}
	}
	
    private void readDescription(String url,Date from, Date to,int currentYear, int currentMonth) throws IOException {
		try {
			URL u = new URL(url);
			BufferedReader in = new BufferedReader(
					new InputStreamReader(u.openStream()));
			
			String dateString = null;
			String fileName= null;
			String line = null;
			while((line = in.readLine()) != null) {
				int split = line.indexOf(' ');
				dateString = line.substring(0,split);
				fileName = line.substring(split+1, line.length()-1);
				
				//make dateString to date
				int t = dateString.indexOf('T');
				String[] date = dateString.substring(0,t).split("-");
				String[] time = dateString.substring(t+1,dateString.length()-1).split(":");
				Calendar cal = new GregorianCalendar(currentYear, currentMonth, Integer.parseInt(date[2]), Integer.parseInt(time[0]), Integer.parseInt(time[1]));
				Date startDate = cal.getTime();
				cal.add(Calendar.HOUR, PfssSettings.FITS_FILE_D_HOUR);
				cal.add(Calendar.MINUTE, PfssSettings.FITS_FILE_D_MINUTES);
				cal.add(Calendar.MILLISECOND, -1);// so there is exactly one millisecond difference between this and the next file descriptor
				if(startDate.after(from) && startDate.before(to))
					descriptors.add(new FileDescriptor(startDate, cal.getTime(),fileName));
			}
		} 
		catch (MalformedURLException e) {
			//programming error
			e.printStackTrace();
		} catch (IOException e) {
			throw new IOException("Unable to find data for: "+currentYear +"/"+currentMonth+1,e);
			//todo throw new exception explaining that resources are not available
		}
	}
   
	
	public int getFileIndex(Date d) {
		return Collections.binarySearch(descriptors, d);
	}
	
	public FileDescriptor getFileDescriptor(int index) {
		return descriptors.get(index);
	}
	
	public int getNumberOfFiles() {
		return descriptors.size();
	}
	
	public static void main(String[] args) {
		FileDescriptorManager man = new FileDescriptorManager();
		GregorianCalendar cal = new GregorianCalendar();
		cal.set(2014, 02, 15, 6, 3);
		Date start = cal.getTime();
		cal.set(2014, 03, 30, 12, 3,59);
		Date end = cal.getTime();
		try {
			man.readFileDescriptors(start, end);
			System.out.println(man.getFileIndex(start));
			System.out.println(man.getFileIndex(end));
			cal.set(2014, 02, 15, 6, 5);
			System.out.println(man.getFileIndex(cal.getTime()));
			cal.set(2014, 02, 15, 12, 4);
			System.out.println(man.getFileIndex(cal.getTime()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
