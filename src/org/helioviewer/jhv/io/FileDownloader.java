package org.helioviewer.jhv.io;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JP2Filter;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;

/**
 * Class for downloading files from the internet.
 * 
 * This class provides the capability to download files from the internet and
 * give a feedback via a progress bar.
 * 
 * @author Stephan Pagel
 * @author Markus Langenberg
 */
public class FileDownloader {

	private JProgressBar progressBar;
	private StandAloneDialog dialog;

	/**
	 * Downloads a file from a given HTTP address to the download directory of
	 * JHV.
	 * 
	 * @param sourceURI
	 *            address of file which have to be downloaded.
	 * @param downloadIfAlreadyExists
	 *            set this flag to download a file when a file in the download
	 *            directory with the same name already exists. The file will not
	 *            be overridden but an increased number will be added to the
	 *            file name.
	 * @return URI to the downloaded file or null if download fails.
	 */
	public URI downloadFromHTTP(URI sourceURI, boolean downloadIfAlreadyExists) {

		// check if sourceURI is an http address
		if (sourceURI == null)
			return null;

		String scheme = sourceURI.getScheme();

		if (scheme == null)
			return null;

		if (!scheme.equalsIgnoreCase("http")) {
			return null;
		}

		try {
			sourceURI = new URI(sourceURI.toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		String name = sourceURI.getPath().substring(
				sourceURI.getPath().lastIndexOf('/') + 1);
		File outFile = chooseFile(name);

		progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
		dialog = new StandAloneDialog("Downloading " + name);
		dialog.setVisible(true);

		// if local file name doesn't exist, download file
		try {
			if (!downloadFile(sourceURI, outFile)) {
				if (dialog.wasInterrupted == false) {
					Message.err("Download", "Unable to download from http",
							false);
				}
				// if the file was not loaded successfully
			} else {
			}
		} catch (IOException e) {
			dialog.setVisible(false);
			e.printStackTrace();
		} finally {
			dialog.setVisible(false);
			dialog = null;
		}
		// return destination of file
		if (outFile != null)
			return outFile.toURI();

		return null;
	}

	private File chooseFile(String defaultTargetFileName) {
		JFileChooser fileChooser = JHVGlobals.getJFileChooser();
		fileChooser.setFileHidingEnabled(false);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser.addChoosableFileFilter(new JP2Filter());

		fileChooser.setSelectedFile(new File(defaultTargetFileName));

		int retVal = fileChooser.showSaveDialog(MainFrame.SINGLETON);
		File selectedFile = null;

		if (retVal == JFileChooser.APPROVE_OPTION) {
			selectedFile = fileChooser.getSelectedFile();

			// Has user entered the correct extension or not?
			ExtensionFileFilter fileFilter = (ExtensionFileFilter) fileChooser
					.getFileFilter();

			if (!fileFilter.accept(selectedFile)) {
				selectedFile = new File(selectedFile.getPath() + "."
						+ fileFilter.getDefaultExtension());
			}

			// does the file already exist?
			if (selectedFile.exists()) {

				// ask if the user wants to overwrite
				int response = JOptionPane.showConfirmDialog(null,
						"Overwrite existing file?", "Confirm Overwrite",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE);

				// if the user doesn't want to overwrite, simply return null
				if (response == JOptionPane.CANCEL_OPTION) {
					return null;
				}
			}
		}

		return selectedFile;
	}

	/**
	 * Gets the file from the source and writes it to the destination file. The
	 * methods provides an own dialog, which displays the current download
	 * progress.
	 * 
	 * @param source
	 *            specifies the location of the file which has to be downloaded.
	 * @param dest
	 *            location where data of the file has to be stored.
	 * @param title
	 *            title which should be displayed in the header of the progress
	 *            dialog.
	 * @return True, if download was successful, false otherwise.
	 * @throws IOException
	 */
	public boolean get(URI source, File dest, String title) throws IOException {

		// create own dialog where to display the progress
		progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
		// progressBar.setPreferredSize(new Dimension(200, 20));

		dialog = new StandAloneDialog(title);
		dialog.setVisible(true);

		// download the file
		boolean result = downloadFile(source, dest);

		dialog.dispose();

		return result;
	}

	/**
	 * Gets the file from the source and writes it to the _dest file.
	 * 
	 * @param source
	 *            specifies the location of the file which has to be downloaded.
	 * @param dest
	 *            location where data of the file has to be stored.
	 * @return True, if download was successful, false otherwise.
	 * @throws IOException
	 * @throws URISyntaxException
	 * */
	private boolean downloadFile(URI source, File dest) throws IOException {

		final URI finalSource;
		final File finalDest;
		if (source == null || dest == null)
			return false;

		if (source.getScheme().equalsIgnoreCase("jpip")) {
			try {
				finalSource = new URI(source.toString()
						.replaceFirst("jpip://", "http://")
						.replaceFirst(":" + source.getPort(), "/jp2"));
			} catch (URISyntaxException e) {
				e.printStackTrace();
				return false;
			}
		} else {
			finalSource = source;
		}

		if (!(finalSource.getScheme().equalsIgnoreCase("http") || finalSource
				.getScheme().equalsIgnoreCase("ftp"))) {
			return false;
		}

		if (dest.isDirectory()) {
			finalDest = new File(dest, finalSource.getPath().substring(
					Math.max(0, finalSource.getPath().lastIndexOf("/"))));
		} else {
			finalDest = dest;
		}
		finalDest.createNewFile();

		URLConnection conn = null;
		FileOutputStream out = null;
		InputStream in = null;

		try {
			conn = finalSource.toURL().openConnection();
			in = conn.getInputStream();
			out = new FileOutputStream(finalDest);

			if (progressBar != null) {
				progressBar.setMaximum(conn.getContentLength());
			}

			byte[] buffer = new byte[1024];
			int numCurrentRead;
			int numTotalRead = 0;

			while (!Thread.interrupted() && !dialog.wasInterrupted
					&& (numCurrentRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numCurrentRead);

				if (progressBar != null) {
					numTotalRead += numCurrentRead;
					progressBar.setValue(numTotalRead);
				}
			}
			if (dialog.wasInterrupted){
				Files.deleteIfExists(finalDest.toPath());
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				in.close();
			} catch (Exception e) {
			}
			try {
				out.close();
			} catch (Exception e) {
			}
		}

		boolean result = true;
		if (progressBar != null) {
			result = (progressBar.getValue() >= progressBar.getMaximum());
		}
		return result;
	}

	/**
	 * Dialog displaying the current download status.
	 * 
	 * The download can be interrupted using the provided button.
	 */
	private class StandAloneDialog extends JWindow implements ActionListener {

		private static final long serialVersionUID = 1L;
		private boolean wasInterrupted = false;

		/**
		 * Default constructor
		 * 
		 * @param title
		 *            Text to show on top of the progress bar
		 */
		public StandAloneDialog(String title) {
			super(MainFrame.SINGLETON);
			setLocationRelativeTo(MainFrame.SINGLETON);
			
			setLayout(new FlowLayout());

			progressBar.setString(title);
			progressBar.setStringPainted(true);
			add(progressBar);

			JButton cmdCancel = new JButton("Cancel");
			cmdCancel.addActionListener(this);
			add(cmdCancel);

			setSize(getPreferredSize());
			MainFrame mainPanel = MainFrame.SINGLETON;
			Point mainImagePanelLocation = mainPanel.getLocationOnScreen();
			Dimension mainImagePanelSize = mainPanel.getSize();
			int x = mainImagePanelLocation.x + mainImagePanelSize.width
					- getSize().width - 4;
			int y = mainImagePanelLocation.y + 2;
			setLocation(x, y);
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent e) {
			System.out.println("interruppted");
				wasInterrupted = true;
		}
	}
}
