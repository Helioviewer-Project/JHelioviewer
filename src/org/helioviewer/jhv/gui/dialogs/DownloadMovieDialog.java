package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.io.File;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.viewmodel.TimeLine;

public class DownloadMovieDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1367652999885843133L;
	private JProgressBar progressBar;
	private static final String PATH_SETTINGS = "download.path";
	private String url = null;
	private String defaultName;
	private static class JPXFilter extends ExtensionFileFilter {

		/**
		 * Default Constructor.
		 */
		private static final String DESCRIPTION = "JPG2000 files (\".jpx\")";
		private static final String EXTENSION = "*.jpx";
		public JPXFilter() {
			extensions = new String[] { EXTENSION };
		}

		/**
		 * {@inheritDoc}
		 */
		public String getDescription() {
			return DESCRIPTION;
		}
		
		public static ExtensionFilter getExtensionFilter(){
			return new ExtensionFilter(DESCRIPTION, EXTENSION);
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

	private void openFileChooser(){
		String lastPath = Settings.getProperty(PATH_SETTINGS);
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Download imagedata");
		if (lastPath != null){
			fileChooser.setCurrentDirectory(new File(lastPath + "/" + defaultName));
		}
		fileChooser.setFileFilter(new JPXFilter());
		int retVal = fileChooser.showSaveDialog(MainFrame.SINGLETON);

		if (retVal == JFileChooser.CANCEL_OPTION) {
			return;
		}

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
			start(fileName);
		}
	}
	
	private void openFileChooserFX(){
		Platform.runLater(new Runnable() {

			@Override
			public void run() {

				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Downlaod imagedata");
				fileChooser.setInitialFileName(defaultName);
				
				String lastPath = Settings.getProperty(PATH_SETTINGS);
				
				if (lastPath != null) {
					File file = new File(lastPath);
					if (file.exists())
					fileChooser.setInitialDirectory(file);
				}

				fileChooser.getExtensionFilters().addAll(JPXFilter.getExtensionFilter());
				final File selectedFile = fileChooser
						.showSaveDialog(new Stage());

				if (selectedFile != null) {
					start(selectedFile.toString());
				}
			}
		});
	}
	
	private void start(String fileName){

		final HTTPDownloadRequest httpDownloadRequest = new HTTPDownloadRequest(
				url, PRIORITY.URGENT, fileName);
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
	
	public void startDownload(String url, AbstractLayer layer) {
		if (TimeLine.SINGLETON.getFrameCount() >= 1000){
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					JOptionPane.showMessageDialog(MainFrame.SINGLETON, "More then 1000 frames aren't available to downlaod", "Not supported framecount",JOptionPane.ERROR_MESSAGE);
				}
			});
			return;
		}
		this.defaultName = layer.getFullName() + "_F" + JHVGlobals.FILE_DATE_TIME_FORMATTER.format(((AbstractImageLayer)layer).getFirstLocalDateTime()) + "_T" + JHVGlobals.FILE_DATE_TIME_FORMATTER.format(((AbstractImageLayer)layer).getLastLocalDateTime());
		this.url = url;
		if (JHVGlobals.USE_JAVA_FX){
			openFileChooserFX();
		}
		else {
			openFileChooser();
		}
	}
}
