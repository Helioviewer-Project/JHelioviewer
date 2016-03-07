package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.DownloadManager;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPDownloadRequest;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.viewmodel.TimeLine;

public class DownloadMovieDialog extends JDialog
{
	public DownloadMovieDialog(String _url, Layer _layer)
	{
		super(MainFrame.SINGLETON, "Download movie", true);
		
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		setLocationRelativeTo(MainFrame.SINGLETON);
		BorderLayout borderLayout = new BorderLayout();
		getContentPane().setLayout(borderLayout);
		setResizable(false);
		
		final JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel, BorderLayout.NORTH);
		
		final JProgressBar progressBar = new JProgressBar(0,1);
		progressBar.setPreferredSize(new Dimension(300, 20));
		panel.add(progressBar);
		progressBar.setValue(50);
		pack();
		
		
		if (TimeLine.SINGLETON.getFrameCount() >= 1000)
		{
			JOptionPane.showMessageDialog(MainFrame.SINGLETON, "You cannot download more than 1000 frames at once.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		String defaultName = _layer.getFullName() + " " + Globals.FILE_DATE_TIME_FORMATTER.format(MathUtils.toLDT(((ImageLayer)_layer).getFirstTimeMS())) + " " + Globals.FILE_DATE_TIME_FORMATTER.format(MathUtils.toLDT(((ImageLayer)_layer).getLastTimeMS()));
		File selectedFile = Globals.showFileDialog(DialogType.SAVE_FILE,
				"Download movie",
				Settings.getString(StringKey.MOVIE_DOWNLOAD_PATH),
				true,
				defaultName,
				PredefinedFileFilter.JPX);
		
		if (selectedFile == null)
			return;
		
		Settings.setString(StringKey.MOVIE_DOWNLOAD_PATH, selectedFile.getParent());
		final HTTPDownloadRequest httpDownloadRequest = new HTTPDownloadRequest(_url, DownloadPriority.URGENT, selectedFile.getPath());
		
		DownloadManager.addRequest(httpDownloadRequest);
		Thread downloadMovieThread = new Thread(() ->
			{
				try
				{
					while (!httpDownloadRequest.isFinished())
					{
						SwingUtilities.invokeAndWait(() ->
							{
								progressBar.setValue(httpDownloadRequest.getReceivedLength());
								progressBar.setMaximum(httpDownloadRequest.getTotalLength());
							});
						Thread.sleep(200);
					}
					
					SwingUtilities.invokeAndWait(() -> setVisible(false));
				}
				catch (Exception e)
				{
					Telemetry.trackException(e);
				}
			}, "DOWNLOAD-MOVIE");
		downloadMovieThread.start();
		
		setVisible(true);
	}
}
