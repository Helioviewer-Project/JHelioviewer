package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.Globals;
import org.helioviewer.jhv.Globals.DialogType;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.viewmodel.TimeLine;

//FIXME: layout on windows broken
public class DownloadMovieDialog extends JDialog
{
	private final JProgressBar progressBar;
	private static final String PATH_SETTINGS = "download.path"; //TODO: consolidate/check all paths

	public DownloadMovieDialog()
	{
		//FIXME: looks bad (Windows), probably needs cancel, etc.
		super(MainFrame.SINGLETON, "Download movie", true);
		
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		setLocationRelativeTo(MainFrame.SINGLETON);
		setAlwaysOnTop(true);
		setLayout(new BorderLayout());
		setResizable(false);

		progressBar = new JProgressBar();
		add(progressBar);

		pack();
	}

	private void start(String fileName, String _url)
	{
		final HTTPDownloadRequest httpDownloadRequest = new HTTPDownloadRequest(_url, DownloadPriority.URGENT, fileName);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				setVisible(true);
				progressBar.setValue(0);
				progressBar.setMaximum(Integer.MAX_VALUE);
			}
		});
		
		UltimateDownloadManager.addRequest(httpDownloadRequest);
		Thread downloadMovieThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					while (!httpDownloadRequest.isFinished())
					{
						progressBar.setValue(httpDownloadRequest.getReceivedLength());
						progressBar.setMaximum(httpDownloadRequest.getTotalLength());
						Thread.sleep(20);
					}
					setVisible(false);
				}
				catch (InterruptedException e)
				{
					Telemetry.trackException(e);
				}
			}
		}, "DOWNLOAD-MOVIE");
		downloadMovieThread.start();
	}
	
	public void startDownload(String _url, Layer _layer)
	{
		if (TimeLine.SINGLETON.getFrameCount() >= 1000)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					JOptionPane.showMessageDialog(MainFrame.SINGLETON, "You cannot download more than 1000 frames at once.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}
		String defaultName = _layer.getFullName() + "_F" + Globals.FILE_DATE_TIME_FORMATTER.format(((AbstractImageLayer)_layer).getFirstLocalDateTime()) + "_T" + Globals.FILE_DATE_TIME_FORMATTER.format(((AbstractImageLayer)_layer).getLastLocalDateTime());
		
		File selectedFile = Globals.showFileDialog(DialogType.SAVE_FILE,
				"Download movie",
				Settings.getProperty(PATH_SETTINGS),
				true,
				defaultName,
				PredefinedFileFilter.JPX);
		
		if (selectedFile == null)
			return;

		Settings.setProperty(PATH_SETTINGS, selectedFile.getParent());
		start(selectedFile.getPath(), _url);
	}
}
