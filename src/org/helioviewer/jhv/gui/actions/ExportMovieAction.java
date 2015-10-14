package org.helioviewer.jhv.gui.actions;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.helioviewer.jhv.Globals;
import org.helioviewer.jhv.Globals.DialogType;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;

public class ExportMovieAction extends AbstractAction
{
  	public ExportMovieAction()
  	{
		super("Save movie as...");
		putValue(SHORT_DESCRIPTION, "Export a movie to a file");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    }

	public void actionPerformed(@Nullable ActionEvent e)
	{
		if(Layers.getLayerCount() > 0)
			openExportMovieDialog();
		else
			JOptionPane.showMessageDialog(MainFrame.MAIN_PANEL, "At least one active layer must be visible.\n\nPlease add a layer before exporting movies.", "Error", JOptionPane.ERROR_MESSAGE);
	}
	
	private long speed = 0;
	private IMediaWriter writer;

	private PredefinedFileFilter selectedOutputFormat = PredefinedFileFilter.MP4;

	private String txtTargetFile;
	private ProgressDialog progressDialog;

	private boolean started = true;

	private String directory;
	private String filename;

	private volatile FileOutputStream fileOutputStream;
	@Nullable private volatile ZipOutputStream zipOutputStream;

	private static final String SETTING_MOVIE_EXPORT_LAST_DIRECTORY = "export.movie.last.directory";

	private static final String SETTING_IMG_WIDTH = "export.movie.image.width";
	private static final String SETTING_IMG_HEIGHT = "export.movie.image.height";
	private static final String SETTING_TEXT = "export.movie.text";

	private boolean textEnabled;
	private int imageWidth;
	private int imageHeight;
	private Thread thread;
	@Nullable private volatile BufferedImage bufferedImage;

	private void openExportMovieDialog()
	{
		txtTargetFile = LocalDateTime.now().format(Globals.DATE_TIME_FORMATTER);

		// Open save-dialog
		final File file = Globals.showFileDialog(
				DialogType.SAVE_FILE,
				"Save Movie as", 
				Settings.getProperty(SETTING_MOVIE_EXPORT_LAST_DIRECTORY),
				false,txtTargetFile,
				PredefinedFileFilter.SaveMovieFileFilter);
		
		if (file == null)
			return;
		
		for(PredefinedFileFilter mff:PredefinedFileFilter.SaveMovieFileFilter)
			if(mff.accept(file))
			{
				selectedOutputFormat=mff;
				break;
			}
		
		directory = file.getParent() + "/";
		filename = file.getName();
		
		startMovieExport();
		
    	Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());
	}

	private void startMovieExport()
	{
		this.loadSettings();
		Settings.setProperty(SETTING_MOVIE_EXPORT_LAST_DIRECTORY, directory);
		MainFrame.SINGLETON.setEnabled(false);

		progressDialog = new ProgressDialog();
		progressDialog.setVisible(true);

		this.initMovieExport();
		thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				TimeLine.SINGLETON.setCurrentFrame(0);
				for (int i = 0; i < TimeLine.SINGLETON.getFrameCount(); i++)
				{
					//FIXME: invokeLATER?!!?!
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							bufferedImage = null;
							progressDialog.setDescription("Rendering images");
							bufferedImage = MainFrame.MAIN_PANEL.getBufferedImage(imageWidth, imageHeight, textEnabled);
						}
					});

					while (bufferedImage == null)
					{
						try
						{
							if (!started)
								break;
							Thread.sleep(20);
						}
						catch (InterruptedException e)
						{
							break;
						}
					}
					if (!started)
						break;

					progressDialog.updateProgressBar(i);

					if (selectedOutputFormat.isMovieFile() && started)
					{
						writer.encodeVideo(0, bufferedImage, speed * i, TimeUnit.MILLISECONDS);
					}
					else if (selectedOutputFormat.isCompressedFile() && started)
					{
						String number = String.format("%04d", i);
						try
						{
							zipOutputStream.putNextEntry(new ZipEntry(filename + "/" + filename + "-" + number
									+ selectedOutputFormat.getInnerMovieFilter().getDefaultExtension()));
							ImageIO.write(bufferedImage, selectedOutputFormat.getInnerMovieFilter().fileType, zipOutputStream);
							zipOutputStream.closeEntry();
						}
						catch (IOException e)
						{
							Telemetry.trackException(e);
						}
					}
					else if (selectedOutputFormat.isImageFile() && started)
					{
						String number = String.format("%04d", i);
						try
						{
							ImageIO.write(
									bufferedImage,
									selectedOutputFormat.fileType,
									new File(directory + filename + "-" + number + selectedOutputFormat.getDefaultExtension()));
						}
						catch (IOException e)
						{
							Telemetry.trackException(e);
						}
					}
					TimeLine.SINGLETON.nextFrame();
				}
				stopMovieExport();
			}

		}, "Movie Export");
		thread.start();
	}

	private void loadSettings()
	{
		String val;
		try
		{
			val = Settings.getProperty(SETTING_TEXT);
			if (val != null && !(val.length() == 0))
			{
				textEnabled = Boolean.parseBoolean(val);
			}
		}
		catch (Throwable t)
		{
			Telemetry.trackException(t);
		}

		try
		{
			val = Settings.getProperty(SETTING_IMG_HEIGHT);
			if (val != null && !(val.length() == 0))
				this.imageHeight = Integer.parseInt(val);
		}
		catch (Throwable t)
		{
			Telemetry.trackException(t);
		}

		try
		{
			val = Settings.getProperty(SETTING_IMG_WIDTH);
			if (val != null && !(val.length() == 0))
			{
				this.imageWidth = Integer.parseInt(val);
			}
		} catch (Throwable t)
		{
			Telemetry.trackException(t);
		}

		// default settings if nothing was specified so far
		if (imageWidth == 0)
			imageWidth = 1280;

		if (imageHeight == 0)
			imageHeight = 720;
	}

	private void initMovieExport()
	{
		if (this.selectedOutputFormat.isMovieFile())
		{
			writer = ToolFactory.makeWriter(directory + filename + this.selectedOutputFormat.getDefaultExtension());

			speed = 1000 / TimeLine.SINGLETON.getMillisecondsPerFrame();

			writer.addVideoStream(0, 0, this.selectedOutputFormat.codec, this.imageWidth, this.imageHeight);
		}
		else if (this.selectedOutputFormat.isCompressedFile())
		{
			try
			{
				fileOutputStream = new FileOutputStream(this.directory
						+ this.filename
						+ this.selectedOutputFormat.getDefaultExtension());
				zipOutputStream = new ZipOutputStream(fileOutputStream);
			}
			catch (FileNotFoundException e)
			{
				Telemetry.trackException(e);
			}
		}
		else if (this.selectedOutputFormat.isImageFile())
		{
			new File(this.directory + this.filename).mkdir();
			directory += this.filename + "/";
		}

		progressDialog.setMaximumOfProgressBar(TimeLine.SINGLETON.getFrameCount());
		TimeLine.SINGLETON.setCurrentFrame(0);
	}

	private void stopMovieExport()
	{
		TimeLine.SINGLETON.setCurrentFrame(0);
		// export movie
		if (selectedOutputFormat.isMovieFile())
			writer.close();
		else if (selectedOutputFormat.isCompressedFile())
		{
			try
			{
				zipOutputStream.close();
				fileOutputStream.close();
			}
			catch (IOException e)
			{
				Telemetry.trackException(e);
			}
		}
		progressDialog.dispose();
	}

	private class ProgressDialog extends JDialog implements ActionListener
	{
		private JProgressBar progressBar;
		private JButton btnCancel;
		private JLabel lblDescription;
		private final JPanel contentPanel = new JPanel();

		private ProgressDialog()
		{
			super(MainFrame.SINGLETON);
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
		public void actionPerformed(@Nullable ActionEvent ae)
		{
			if (ae.getSource() == btnCancel)
			{
				ExportMovieAction.this.cancelMovie();
				dispose();
			}
		}
	}

	private void cancelMovie()
	{
		started = false;
	}
}
