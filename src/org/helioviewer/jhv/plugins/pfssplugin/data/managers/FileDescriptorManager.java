package org.helioviewer.jhv.plugins.pfssplugin.data.managers;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.pfssplugin.PfssSettings;
import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.plugin.UltimatePluginInterface;

/**
 * Manages loading and accessing of FileDescriptor Objects
 */
public class FileDescriptorManager {
	private ArrayList<FileDescriptor> descriptors = new ArrayList<>();
	private LocalDateTime firstDate;
	private LocalDateTime endDate;
	private volatile int epoch = 0;

	private volatile String errorMessage;
	private LocalDateTime loadingFrom;
	private LocalDateTime loadingTo;

	private PfssPlugin parent;

	public FileDescriptorManager(PfssPlugin _parent) {
		parent = _parent;
	}

	/**
	 * checks if Date is in Range of the FileDescriptor Manager
	 * 
	 * @param currentLocalDateTime
	 * @return true if it is in range
	 */
	public synchronized boolean isDateInRange(LocalDateTime currentLocalDateTime) {
		if (firstDate == null || endDate == null)
			return false;

		return (firstDate.isBefore(currentLocalDateTime) & endDate
				.isAfter(currentLocalDateTime))
				| firstDate.isEqual(currentLocalDateTime)
				| endDate.isEqual(currentLocalDateTime);
	}

	/**
	 * Reads the file descriptions on the server from a range of dates
	 * 
	 * @param from
	 *            date of first file description to read
	 * @param to
	 *            date of last file description to read
	 */
	public synchronized void readFileDescriptors(final LocalDateTime from,
			final LocalDateTime to) {
		epoch++;
		final int curEpoch = epoch;
		errorMessage = null;

		this.loadingFrom = from;
		this.loadingTo = to;

		this.firstDate = from;
		this.endDate = to;

		synchronized (descriptors) {
			descriptors.clear();
		}

		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				LocalDateTime currentDate = from;
				DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM");
				DateTimeFormatter yearFormatter = DateTimeFormatter
						.ofPattern("YYYY");
				DateTimeFormatter dateTimeFormatter = DateTimeFormatter
						.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

				ArrayList<HTTPRequest> httpRequests = new ArrayList<HTTPRequest>();
				while (currentDate.isBefore(to)) {
					final String url = PfssSettings.SERVER_URL
							+ currentDate.format(yearFormatter) + "/"
							+ currentDate.format(monthFormatter) + "/list.txt";
					httpRequests.add(UltimatePluginInterface
							.generateAndStartHTPPRequest(url, PRIORITY.MEDIUM));
					currentDate = currentDate.plusMonths(1);
				}

				for (HTTPRequest httpRequest : httpRequests) {
					while (!httpRequest.isFinished()) {
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

					String lines[];
					try {
						lines = httpRequest.getDataAsString().split("\\r?\\n");
						for (String line : lines) {
							int split = line.indexOf(' ');
							String dateString = line.substring(0, split);
							String fileName = line.substring(split + 1,
									line.length());

							LocalDateTime end = LocalDateTime.parse(dateString,
									dateTimeFormatter);
							LocalDateTime start = end
									.minusHours(PfssSettings.FITS_FILE_D_HOUR)
									.minusMinutes(
											PfssSettings.FITS_FILE_D_MINUTES)
									.plusNanos(1);

							if (!(end.isBefore(from) || start.isAfter(to))) {
								synchronized (descriptors) {
									if (curEpoch != epoch)
										return;

									descriptors.add(new FileDescriptor(start,
											end, fileName));
								}
							}
						}
					} catch (IOException e) {
						 parent.addBadRequest(httpRequest);
					}
					UltimatePluginInterface.repaintMainPanel();
				}
			}
		}, "PFSS-DESCRIPTION-LOADER");
		thread.start();

	}

	/**
	 * Returns the Descriptor at Index
	 * 
	 * @param index
	 * @return
	 */
	public FileDescriptor getFileDescriptor(LocalDateTime localDateTime) {
		synchronized (descriptors) {
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
	public FileDescriptor getNext(FileDescriptor current) {
		synchronized (descriptors) {
			int index = descriptors.indexOf(current);
			index = ++index % descriptors.size();
			return descriptors.get(index);
		}
	}

	void showErrorMessages() {
		if (!parent.isVisible())
			return;

		if (errorMessage == null)
			return;

		Object[] options = { "Retry", "Cancel" };
		Object[] params = { errorMessage };
		int n = JOptionPane.showOptionDialog(MainFrame.SINGLETON, params,
				"PFSS data", JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE, null, options, options[1]);

		if (n == 0)
			readFileDescriptors(loadingFrom, loadingTo);
	}

	public LocalDateTime getStartDate() {
		return firstDate;
	}
	
	public LocalDateTime getEndDate() {
		return endDate;
	}

	public void retryBadReqeuest() {
		// TODO Auto-generated method stub
		
	}
}
