package org.helioviewer.jhv.plugins.pfssplugin.data.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;

/**
 * Manages loading and accessing of FileDescriptor Objects
 */
public class FileDescriptorManager
{
	private ArrayList<FileDescriptor> descriptors=new ArrayList<>();
	private LocalDateTime firstDate;
	private LocalDateTime endDate;
	private volatile int epoch=0;
	
    private volatile String errorMessage;
    private LocalDateTime loadingFrom;
    private LocalDateTime loadingTo;
    
    private PfssPlugin parent;
	
	public FileDescriptorManager(PfssPlugin _parent)
	{
	    parent=_parent;
	}
	
	/**
	 * checks if Date is in Range of the FileDescriptor Manager
	 * @param currentLocalDateTime
	 * @return true if it is in range
	 */
	public synchronized boolean isDateInRange(LocalDateTime currentLocalDateTime)
	{
	    if(firstDate==null || endDate==null)
	        return false;
	    
		return (firstDate.isBefore(currentLocalDateTime) & endDate.isAfter(currentLocalDateTime)) |  firstDate.isEqual(currentLocalDateTime) | endDate.isEqual(currentLocalDateTime);
	}
	
	/**
	 * Reads the file descriptions on the server from a range of dates
	 * @param from date of first file description to read
	 * @param to date of last file description to read
	 */
	public synchronized void readFileDescriptors(final LocalDateTime from, final LocalDateTime to)
	{
	    epoch++;
	    final int curEpoch = epoch;
	    errorMessage=null;
	    
	    this.loadingFrom = from;
	    this.loadingTo = to;
	    
	    this.firstDate = from;
	    this.endDate = to;
	    
		LocalDateTime currentDate = from;
		DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM");
		DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("YYYY");
	    
		synchronized(descriptors)
        {
	        descriptors.clear();
        }
		
		while(currentDate.isBefore(to))
		{
			final String url = PfssSettings.SERVER_URL + currentDate.format(yearFormatter) +"/"+currentDate.format(monthFormatter)+"/list.txt";
			System.out.println("url : " + url);
			final LocalDateTime current = currentDate;
			PfssPlugin.pool.execute(new Runnable()
	        {
	            @Override
	            public void run()
	            {
	                try
                    {
	                    if(curEpoch==epoch && errorMessage==null)
	                    {
	                        readDescription(url, current, to, curEpoch);
	                        MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();;
	                    }	                    
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                        if(curEpoch==epoch && errorMessage==null)
                        {
                            errorMessage="There was no PFSS data available for the selected time range.";
                            SwingUtilities.invokeLater(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    showErrorMessages();
                                }
                            });
                            
                        }
                    }
	            }
	        });
			
			currentDate = currentDate.plusMonths(1);
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
    private void readDescription(String url,LocalDateTime from, LocalDateTime to, int _curEpoch) throws IOException
    {
        if(_curEpoch!=epoch)
            return;
        System.out.println(url);
    	try(BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream())))
    	{
			String dateString = null;
			String fileName= null;
			String line = null;
			
			final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

			while((line = in.readLine()) != null)
			{
				int split = line.indexOf(' ');
				dateString = line.substring(0,split);
				fileName = line.substring(split+1, line.length());

				LocalDateTime end = LocalDateTime.parse(dateString, dateTimeFormatter);
				LocalDateTime start = end.minusHours(PfssSettings.FITS_FILE_D_HOUR).minusMinutes(PfssSettings.FITS_FILE_D_MINUTES).plusNanos(1);
					
				if(!(end.isBefore(from) || start.isAfter(to)))
				{
				    synchronized(descriptors)
				    {
				        if(_curEpoch!=epoch)
				            return;
				        
				        descriptors.add(new FileDescriptor(start, end, fileName));
				    }
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
    		throw new IOException("Unable to find data for: "+from.getYear() +"/"+from.getMonthValue(),e);
		}
	}
    
    /**
     * Returns the Descriptor at Index
     * @param index
     * @return
     */
    public FileDescriptor getFileDescriptor(LocalDateTime localDateTime)
    {
        synchronized(descriptors)
        {
            for(FileDescriptor fd:descriptors)
                if(fd.isDateInRange(localDateTime))
                    return fd;
            
            return null;
            
            //cannot use binary search since descriptors might be unordered
            /*int index=Collections.binarySearch(descriptors, d);
            if(index<0)
                return null;
            return descriptors.get(index);*/
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
	
    void showErrorMessages()
    {
        if(!parent.isVisible())
            return;
        
        if(errorMessage==null)
            return;
        
        Object[] options={"Retry","Cancel"};
        Object[] params={errorMessage};
        int n=JOptionPane.showOptionDialog(MainFrame.SINGLETON,params,"PFSS data",JOptionPane.YES_NO_CANCEL_OPTION,JOptionPane.WARNING_MESSAGE,null,options,options[1]);

        if(n==0)
            readFileDescriptors(loadingFrom,loadingTo);
    }
}
