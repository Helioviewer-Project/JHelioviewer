package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;

/**
 * Manages loading and accessing of FileDescriptor Objects
 */
public class FileDescriptorManager
{
	private final ArrayList<FileDescriptor> descriptors = new ArrayList<>();
	private @Nullable LocalDateTime firstDate;
	private @Nullable LocalDateTime endDate;
	private volatile int epoch = 0;

	private PfssPlugin parent;

	public FileDescriptorManager(PfssPlugin _parent)
	{
		parent = _parent;
	}

	/**
	 * checks if Date is in Range of the FileDescriptor Manager
	 * 
	 * @param currentLocalDateTime
	 * @return true if it is in range
	 */
	public synchronized boolean isDateInRange(LocalDateTime currentLocalDateTime)
	{
		if (firstDate == null || endDate == null)
			return false;

		return (firstDate.isBefore(currentLocalDateTime) & endDate.isAfter(currentLocalDateTime))
				| firstDate.isEqual(currentLocalDateTime)
				| endDate.isEqual(currentLocalDateTime);
	}

	/**
	 * Reads the file descriptions on the server from a range of dates
	 * 
	 * @param _from
	 *            date of first file description to read
	 * @param _to
	 *            date of last file description to read
	 */
	public synchronized void readFileDescriptors(final @Nullable LocalDateTime _from, final @Nullable LocalDateTime _to)
	{
		epoch++;
		final int curEpoch = epoch;

		firstDate = _from;
		endDate = _to;

		synchronized (descriptors)
		{
			descriptors.clear();
		}
		
		if(firstDate==null || endDate==null)
			return;

		Thread thread = new Thread(() ->
			{
				LocalDateTime currentDate = _from;
				DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM");
				DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("YYYY");
				DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

				ArrayList<HTTPRequest> httpRequests = new ArrayList<>();
				while (currentDate.isBefore(_to))
				{
					final String url = PfssSettings.SERVER_URL + currentDate.format(yearFormatter) + "/" + currentDate.format(monthFormatter) + "/list.txt";
					httpRequests.add(Plugins.startHTPPRequest(url, DownloadPriority.MEDIUM));
					currentDate = currentDate.plusMonths(1);
				}

				for (HTTPRequest httpRequest : httpRequests)
				{
					try
					{
						String[] lines = httpRequest.getDataAsString().split("\\r?\\n");
						for (String line : lines)
						{
							int split = line.indexOf(' ');
							String dateString = line.substring(0, split);
							String fileName = line.substring(split + 1, line.length());

							LocalDateTime end = LocalDateTime.parse(dateString, dateTimeFormatter);
							LocalDateTime start = end.minusHours(PfssSettings.FITS_FILE_D_HOUR).minusMinutes(PfssSettings.FITS_FILE_D_MINUTES).plusNanos(1);

							if (!(end.isBefore(_from) || start.isAfter(_to)))
							{
								synchronized (descriptors)
								{
									if (curEpoch != epoch)
										return;

									descriptors.add(new FileDescriptor(start, end, fileName));
								}
							}
						}
					}
					catch (InterruptedException _ie)
					{
						return;
					}
					catch (Throwable e)
					{
						parent.failedDownloads.add(httpRequest);
					}
					finally
					{
						for (HTTPRequest r : httpRequests)
							Plugins.cancelHTTPRequest(r);
					}
					Plugins.repaintMainPanel();
				}
			}, "PFSS-DESCRIPTION-LOADER");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Returns the Descriptor at Index
	 * 
	 * @param index
	 * @return
	 */
	public @Nullable FileDescriptor getFileDescriptor(LocalDateTime localDateTime)
	{
		synchronized (descriptors)
		{
			for (FileDescriptor fd : descriptors)
				if (fd.isDateInRange(localDateTime))
					return fd;

			return null;

			// cannot use binary search since descriptors might be unordered
			/*
			 * int index=Collections.binarySearch(descriptors, d); if(index<0)
			 * return null; return descriptors.get(index);
			 */
		}
	}

	/**
	 * Returns the following FileDescriptor
	 * 
	 * @param current
	 * @return
	 */
	public FileDescriptor getNext(FileDescriptor current)
	{
		synchronized (descriptors)
		{
			int index = descriptors.indexOf(current);
			index = ++index % descriptors.size();
			return descriptors.get(index);
		}
	}

	void showErrorMessages()
	{
		if (!parent.isVisible())
			return;

		// TODO: add proper error handling
		/*
		 * if (errorMessage == null) return;
		 * 
		 * Object[] options = { "Retry", "Cancel" }; Object[] params = {
		 * errorMessage }; int n =
		 * JOptionPane.showOptionDialog(MainFrame.SINGLETON, params, "PFSS data"
		 * , JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
		 * null, options, options[1]);
		 * 
		 * if (n == 0) readFileDescriptors(loadingFrom, loadingTo);
		 */
	}

	public synchronized @Nullable LocalDateTime getStartDate()
	{
		return firstDate;
	}

	public synchronized @Nullable LocalDateTime getEndDate()
	{
		return endDate;
	}
}
