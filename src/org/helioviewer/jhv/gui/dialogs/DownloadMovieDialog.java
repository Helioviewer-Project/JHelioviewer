package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.viewmodel.TimeLine;

//TODO: layout on windows broken
public class DownloadMovieDialog extends JDialog
{
	private static final String PATH_SETTINGS = "download.path"; //TODO: consolidate/check all paths

	public DownloadMovieDialog(String _url, Layer _layer)
	{
		super(MainFrame.SINGLETON, "Download movie", true);
		
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		setLocationRelativeTo(MainFrame.SINGLETON);
		setLayout(new BorderLayout());
		setResizable(false);
		
		final JProgressBar progressBar = new JProgressBar(0,1);
		add(progressBar);
		pack();
		
		
		

		if (TimeLine.SINGLETON.getFrameCount() >= 1000)
		{
			JOptionPane.showMessageDialog(MainFrame.SINGLETON, "You cannot download more than 1000 frames at once.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		String defaultName = _layer.getFullName() + " " + Globals.FILE_DATE_TIME_FORMATTER.format(((AbstractImageLayer)_layer).getFirstLocalDateTime()) + " " + Globals.FILE_DATE_TIME_FORMATTER.format(((AbstractImageLayer)_layer).getLastLocalDateTime());
		File selectedFile = Globals.showFileDialog(DialogType.SAVE_FILE,
				"Download movie",
				Settings.getString(PATH_SETTINGS),
				true,
				defaultName,
				PredefinedFileFilter.JPX);
		
		if (selectedFile == null)
			return;
		
		Settings.setString(PATH_SETTINGS, selectedFile.getParent());
		final HTTPDownloadRequest httpDownloadRequest = new HTTPDownloadRequest(_url, DownloadPriority.URGENT, selectedFile.getPath());
		
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
						SwingUtilities.invokeAndWait(new Runnable()
						{
							@Override
							public void run()
							{
								progressBar.setValue(httpDownloadRequest.getReceivedLength());
								progressBar.setMaximum(httpDownloadRequest.getTotalLength());
							}
						});
						Thread.sleep(200);
					}
					
					SwingUtilities.invokeAndWait(new Runnable()
					{
						@Override
						public void run()
						{
							setVisible(false);
						}
					});
				}
				catch (Exception e)
				{
					Telemetry.trackException(e);
				}
			}
		}, "DOWNLOAD-MOVIE");
		downloadMovieThread.start();
		
		setVisible(true);
	}
}
