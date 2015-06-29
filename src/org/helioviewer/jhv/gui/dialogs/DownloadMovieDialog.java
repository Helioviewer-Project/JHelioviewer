package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;

public class DownloadMovieDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1367652999885843133L;
	private JProgressBar progressBar;
	private static final String PATH_SETTINGS = "download.path";
	
	private class JPXFilter extends ExtensionFileFilter {

		/**
		 * Default Constructor.
		 */
		public JPXFilter() {
			extensions = new String[] { "jpx" };
		}

		/**
		 * {@inheritDoc}
		 */
		public String getDescription() {
			return "JPG2000 files (\".jpx\")";
		}
	}

	public DownloadMovieDialog() {
		super(MainFrame.SINGLETON, "Download imagedata", true);
		setLocationRelativeTo(MainFrame.SINGLETON);
		setAlwaysOnTop(true);
		setLayout(new BorderLayout());
		setResizable(false);
		initGUI();
		pack();
	}

	private void initGUI() {
		progressBar = new JProgressBar();
		this.add(progressBar);
	}

	public void startDownload(String url) {
		String lastPath = Settings.getProperty(PATH_SETTINGS);
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Download imagedata");
		if (lastPath != null){
			fileChooser.setCurrentDirectory(new File(lastPath));
		}
		fileChooser.setFileFilter(new JPXFilter());
		int retVal = fileChooser.showSaveDialog(MainFrame.SINGLETON);
		
		if (fileChooser.getSelectedFile().exists()) {
			// ask if the user wants to overwrite
			int response = JOptionPane.showConfirmDialog(null,
					"Overwrite existing file?", "Confirm Overwrite",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);

			// if the user doesn't want to overwrite, simply return null
			if (response == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}
		
		if (retVal == JFileChooser.APPROVE_OPTION) {
			Settings.setProperty(PATH_SETTINGS, fileChooser.getCurrentDirectory().getAbsolutePath());
			String fileName = fileChooser.getSelectedFile().toString();
			JPXFilter fileFilter = (JPXFilter)fileChooser.getFileFilter();
			fileName = fileName.endsWith(fileFilter.getDefaultExtension()) ? fileName : fileName + fileFilter.getDefaultExtension();

			final HTTPDownloadRequest httpDownloadRequest = new HTTPDownloadRequest(
					url, PRIORITY.URGENT, fileName);
			System.out.println(url);
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					setVisible(true);
					progressBar.setValue(0);
					progressBar.setMaximum(Integer.MAX_VALUE);
					
				}
			});
			UltimateDownloadManager.addRequest(httpDownloadRequest);
			Thread downloadMovieThread = new Thread(new Runnable() {
			
				@Override
				public void run() {
					while (!httpDownloadRequest.isFinished()){
						progressBar.setValue(httpDownloadRequest.getReceivedLength());
						progressBar.setMaximum(httpDownloadRequest.getTotalLength());
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					setVisible(false);
				}
			}, "DOWNLOAD-MOVIE");
			downloadMovieThread.start();
		}
	}
}
