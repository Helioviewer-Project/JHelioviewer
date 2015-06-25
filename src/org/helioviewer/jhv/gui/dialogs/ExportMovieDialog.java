package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.MetaDataException;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;

public class ExportMovieDialog implements ActionListener {

	private long speed = 0;
	private IMediaWriter writer;

	private MovieFileFilter selectedOutputFormat = MovieFileFilter.ImplementedMovieFilter.MP4
			.getMovieFilter();

	private String txtTargetFile;
	private ProgressDialog progressDialog;

	private boolean started = true;

	private String directory;
	private String filename;

	private volatile FileOutputStream fileOutputStream;
	private volatile ZipOutputStream zipOutputStream;

	private static final String SETTING_MOVIE_EXPORT_LAST_DIRECTORY = "export.movie.last.directory";

	private static final String SETTING_IMG_WIDTH = "export.movie.image.width";
	private static final String SETTING_IMG_HEIGHT = "export.movie.image.height";
	private static final String SETTING_TEXT = "export.movie.text";

	private boolean textEnabled;
	private int imageWidth;
	private int imageHeight;
	private Thread thread;
	private ArrayList<String> descriptions;
	private BufferedImage bufferedImage;

	public ExportMovieDialog() {
		if (openFileChooser() == JFileChooser.APPROVE_OPTION) {

			this.loadSettings();
			Settings.setProperty(SETTING_MOVIE_EXPORT_LAST_DIRECTORY, directory);
			MainFrame.SINGLETON.setEnabled(false);

			progressDialog = new ProgressDialog(this);
			progressDialog.setVisible(true);

			this.initExportMovie();
			thread = new Thread(new Runnable() {
				
				@Override
				public void run() {
						TimeLine.SINGLETON.setCurrentFrame(0);
						for (int i = 0; i < TimeLine.SINGLETON.getMaxFrames(); i++){
							
							descriptions = null;
							if (textEnabled) {
								descriptions = new ArrayList<String>();
								for (LayerInterface layer : Layers.getLayers()) {
									if (layer.isVisible()) {
										try {
											descriptions.add(layer.getMetaData().getFullName()
													+ " - "
													+ layer.getTime().format(JHVGlobals.DATE_TIME_FORMATTER));
										} catch (MetaDataException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
									}
								}
							}

							bufferedImage = null;
							progressDialog.setDescription("Rendering images");
							SwingUtilities.invokeLater(new Runnable() {
								
								@Override
								public void run() {
									bufferedImage = MainFrame.MAIN_PANEL.getBufferedImage(
											imageWidth, imageHeight, descriptions);
								}
							});
							
							while (bufferedImage == null){
								try {
									if(!started) break;
									Thread.sleep(20);
								} catch (InterruptedException e) {
									break;
								}
							}
							if(!started) break;
							
							progressDialog.updateProgressBar(i);
							
							if (selectedOutputFormat.isMovieFile() && started) {
									writer.encodeVideo(0, bufferedImage, speed
										* i, TimeUnit.MILLISECONDS);
							}

							else if (selectedOutputFormat.isCompressedFile() && started) {
								String number = String.format("%04d", i);
								try {
									zipOutputStream.putNextEntry(new ZipEntry(filename
											+ "/"
											+ filename
											+ "-"
											+ number
											+ selectedOutputFormat.getInnerMovieFilter()
													.getExtension()));
									ImageIO.write(bufferedImage, selectedOutputFormat
											.getInnerMovieFilter().getFileType(),
											zipOutputStream);
									zipOutputStream.closeEntry();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}

							else if (selectedOutputFormat.isImageFile() && started) {
								String number = String.format("%04d", i);
								try {
									ImageIO.write(bufferedImage, selectedOutputFormat
											.getFileType(), new File(directory + filename
											+ filename + "-" + number
											+ selectedOutputFormat.getExtension()));
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							TimeLine.SINGLETON.nextFrame();
						}
						stopExportMovie();
					}
					
			}, "Movie Export");
			thread.start();
		}
	}

	private int openFileChooser() {
		txtTargetFile = "";

		txtTargetFile += LocalDateTime.now().format(
				JHVGlobals.DATE_TIME_FORMATTER);
		txtTargetFile += selectedOutputFormat.getExtension();

		// Open save-dialog
		final JFileChooser fileChooser = JHVGlobals.getJFileChooser();
		fileChooser.setFileHidingEnabled(false);
		fileChooser.setMultiSelectionEnabled(false);
		fileChooser.setAcceptAllFileFilterUsed(false);

		String val;
		try {
			val = Settings.getProperty(SETTING_MOVIE_EXPORT_LAST_DIRECTORY);
			if (val != null && !(val.length() == 0)) {
				fileChooser.setCurrentDirectory(new File(val));
			}
		} catch (Throwable t) {
			System.err.println(t);
		}

		// add Filter
		for (MovieFileFilter.ImplementedMovieFilter movieFilter : MovieFileFilter.ImplementedMovieFilter
				.values()) {
			fileChooser.addChoosableFileFilter(movieFilter.getMovieFilter());
		}

		// if txtTargetFile's set the selectedOutputFormat and fileChooser's
		// filter according to txtTargetFile's extension

		for (FileFilter fileFilter : fileChooser.getChoosableFileFilters()) {
			if (txtTargetFile.endsWith(((MovieFileFilter) fileFilter)
					.getExtension())) {
				fileChooser.setFileFilter(fileFilter);
				selectedOutputFormat = (MovieFileFilter) fileFilter;
			}
		}

		txtTargetFile = txtTargetFile.substring(0,
				txtTargetFile.lastIndexOf(selectedOutputFormat.getExtension()));

		fileChooser.setSelectedFile(new File(txtTargetFile));

		int retVal = fileChooser
				.showDialog(MainFrame.SINGLETON, "Export movie");

		if (retVal != JFileChooser.CANCEL_OPTION) {

			selectedOutputFormat = (MovieFileFilter) fileChooser
					.getFileFilter();
			directory = fileChooser.getCurrentDirectory().getPath() + "/";
			filename = fileChooser.getSelectedFile().getName();

			if (fileChooser.getSelectedFile().exists()) {
				// ask if the user wants to overwrite
				int response = JOptionPane.showConfirmDialog(null,
						"Overwrite existing file?", "Confirm Overwrite",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.QUESTION_MESSAGE);

				// if the user doesn't want to overwrite, simply return null
				if (response == JOptionPane.CANCEL_OPTION) {
					return JFileChooser.CANCEL_OPTION;
				}
			}

			for (FileFilter fileFilter : fileChooser.getChoosableFileFilters()) {
				if (txtTargetFile.endsWith(((MovieFileFilter) fileFilter)
						.getExtension())) {
					// does the file already exist?
					selectedOutputFormat = (MovieFileFilter) fileFilter;
					filename = filename.substring(0, filename
							.lastIndexOf(selectedOutputFormat.getExtension()));
					return retVal;
				}
			}
		}

		return retVal;
	}

	private void loadSettings() {
		String val;
		try {
			val = Settings.getProperty(SETTING_TEXT);
			if (val != null && !(val.length() == 0)) {
				textEnabled = Boolean.parseBoolean(val);
			}
		} catch (Throwable t) {
			System.err.println(t);
		}

		try {
			val = Settings.getProperty(SETTING_IMG_HEIGHT);
			if (val != null && !(val.length() == 0)) {
				this.imageHeight = Integer.parseInt(val);
			}
		} catch (Throwable t) {
			System.err.println(t);
		}

		try {
			val = Settings.getProperty(SETTING_IMG_WIDTH);
			if (val != null && !(val.length() == 0)) {
				this.imageWidth = Integer.parseInt(val);
			}
		} catch (Throwable t) {
			System.err.println(t);
		}

		// default settings if nothing was specified so far
		if (imageWidth == 0)
			imageWidth = 1280;

		if (imageHeight == 0)
			imageHeight = 720;
	}

	private void initExportMovie() {
		if (this.selectedOutputFormat.isMovieFile()) {

			writer = ToolFactory.makeWriter(directory + filename
					+ this.selectedOutputFormat.getExtension());

			speed = 1000 / TimeLine.SINGLETON.getSpeedFactor();

			writer.addVideoStream(0, 0, this.selectedOutputFormat.getCodec(),
					this.imageWidth, this.imageHeight);
		}

		else if (this.selectedOutputFormat.isCompressedFile()) {
			try {
				fileOutputStream = new FileOutputStream(this.directory
						+ this.filename
						+ this.selectedOutputFormat.getExtension());
				zipOutputStream = new ZipOutputStream(fileOutputStream);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		else if (this.selectedOutputFormat.isImageFile()) {
			File dir = new File(this.directory + this.filename);
			dir.mkdir();
			directory += this.filename + "/";
		}

		progressDialog.setMaximumOfProgressBar(TimeLine.SINGLETON
				.getMaxFrames());
		TimeLine.SINGLETON.setCurrentFrame(0);
	}

	private void stopExportMovie() {
		TimeLine.SINGLETON.setCurrentFrame(0);
		// export movie
		if (selectedOutputFormat.isMovieFile())
			writer.close();
		else if (selectedOutputFormat.isCompressedFile()) {
			try {
				zipOutputStream.close();
				fileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		progressDialog.dispose();
	}

	private static class ProgressDialog extends JDialog implements
			ActionListener {

		private static final long serialVersionUID = -488930636247393662L;
		private JProgressBar progressBar;
		private JButton btnCancel;
		private JLabel lblDescription;
		private ExportMovieDialog exportMovieDialog;
		private final JPanel contentPanel = new JPanel();

		private ProgressDialog(ExportMovieDialog exportMovieDialog) {
			super(MainFrame.SINGLETON);
			this.exportMovieDialog = exportMovieDialog;
			setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			setResizable(false);
			setTitle("Movie export");
			setBounds(100, 100, 450, 300);

			getContentPane().setLayout(new BorderLayout());
			contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

			getContentPane().add(contentPanel, BorderLayout.CENTER);
			contentPanel.setLayout(new BorderLayout(0, 0));

			{
				JLabel lblMovieExportIs = new JLabel("Movie export is running");
				contentPanel.add(lblMovieExportIs, BorderLayout.NORTH);
			}
			{
				progressBar = new JProgressBar();
				contentPanel.add(progressBar, BorderLayout.CENTER);
			}
			{
				lblDescription = new JLabel("Rendering...");
				contentPanel.add(lblDescription, BorderLayout.SOUTH);
			}
			{
				JPanel buttonPane = new JPanel();
				buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
				getContentPane().add(buttonPane, BorderLayout.SOUTH);
				{
					btnCancel = new JButton("Cancel");
					buttonPane.add(btnCancel);
					btnCancel.addActionListener(this);
				}
			}

			this.pack();
			this.setLocationRelativeTo(MainFrame.SINGLETON);

		}

		public void setMaximumOfProgressBar(int maximum) {
			this.progressBar.setMaximum(maximum);
		}

		private void updateProgressBar(int value) {
			this.progressBar.setValue(value);
		}

		public void setDescription(String description) {
			this.lblDescription.setText(description);
		}

		@Override
		public void dispose() {
			MainFrame.SINGLETON.setEnabled(true);
			super.dispose();
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == btnCancel) {
				this.exportMovieDialog.cancelMovie();;
				dispose();
			}

		}
	}
	
	public void cancelMovie() {
		started = false;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// exportMovie();
	}

}
