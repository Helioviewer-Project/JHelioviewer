package org.helioviewer.jhv.gui.actions;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
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

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.BooleanKey;
import org.helioviewer.jhv.base.Settings.IntKey;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.TimeLine;

import com.google.common.io.Files;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;

public class SaveMovieAsAction extends AbstractAction
{
	private boolean started = true;
	private boolean textEnabled;
	private int imageWidth;
	private int imageHeight;
	@Nullable
	private volatile BufferedImage bufferedImage;

	public SaveMovieAsAction()
	{
		super("Save movie as...");
		putValue(SHORT_DESCRIPTION, "Export a movie to a file");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E,
				KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	public void actionPerformed(@Nullable ActionEvent e)
	{
		if (Layers.anyImageLayers())
			openExportMovieDialog();
		else
			JOptionPane.showMessageDialog(MainFrame.SINGLETON.MAIN_PANEL,
					"At least one active layer must be visible.\n\nPlease add a layer before exporting movies.",
					"Error", JOptionPane.ERROR_MESSAGE);
	}

	private void openExportMovieDialog()
	{
		String txtTargetFile = LocalDateTime.now().format(Globals.FILE_DATE_TIME_FORMATTER);

		// Open save-dialog
		final File file = Globals.showFileDialog(DialogType.SAVE_FILE, "Save Movie as",
				Settings.getString(StringKey.MOVIE_EXPORT_DIRECTORY), false, txtTargetFile,
				PredefinedFileFilter.SaveMovieFileFilter);

		if (file == null)
			return;

		PredefinedFileFilter selectedOutputFormat=null;
		for (PredefinedFileFilter mff : PredefinedFileFilter.SaveMovieFileFilter)
			if (mff.accept(file))
			{
				selectedOutputFormat = mff;
				break;
			}

		String fileName=file.getName();
		
		if(selectedOutputFormat==null)
		{
			selectedOutputFormat = PredefinedFileFilter.MP4;
			fileName += selectedOutputFormat.getDefaultExtension();
		}
		
		startMovieExport(file.getParent() + "/", fileName,selectedOutputFormat);
		
		Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());
	}

	@SuppressWarnings("null")
	private void startMovieExport(String _directory, final String _filename, final PredefinedFileFilter _selectedOutputFormat)
	{
		this.loadSettings();
		Settings.setString(StringKey.MOVIE_EXPORT_DIRECTORY, _directory);
		MainFrame.SINGLETON.setEnabled(false);

		final @Nullable FileOutputStream fileOutputStream;
		final @Nullable ZipOutputStream zipOutputStream;
		final @Nullable IMediaWriter writer;
		final int msPerFrame;
		if (_selectedOutputFormat.isMovieFile())
		{
			fileOutputStream = null;
			zipOutputStream = null;

			msPerFrame = (int) Math.round(1000d / TimeLine.SINGLETON.getMillisecondsPerFrame());

			writer = ToolFactory.makeWriter(_directory + _filename);
			writer.addVideoStream(0, 0, _selectedOutputFormat.codec, imageWidth, imageHeight);
		}
		else if (_selectedOutputFormat.isCompressedFile())
		{
			writer = null;
			msPerFrame = 0;
			try
			{
				fileOutputStream = new FileOutputStream(_directory + _filename);
				zipOutputStream = new ZipOutputStream(fileOutputStream);
			}
			catch (FileNotFoundException e1)
			{
				Telemetry.trackException(e1);
				return;
			}
		}
		else if (_selectedOutputFormat.isImageFile())
		{
			fileOutputStream = null;
			zipOutputStream = null;
			writer = null;
			msPerFrame = 0;
			new File(_directory + Files.getNameWithoutExtension(_filename)).mkdir();
			_directory += _filename + "/";
		}
		else
			throw new RuntimeException();

		Telemetry.trackEvent("Export movie", "Format", _selectedOutputFormat.description, "Width", imageWidth + "",
				"Height", imageHeight + "", "Text", textEnabled ? "1" : "0");
		Telemetry.trackMetric("MovieWidth", imageWidth);
		Telemetry.trackMetric("MovieHeight", imageHeight);
		Telemetry.trackMetric("MovieTextEnabled", textEnabled ? 1 : 0);

		final ProgressDialog progressDialog = new ProgressDialog();
		progressDialog.setDescription("Rendering images");
		progressDialog.setVisible(true);
		progressDialog.setMaximumOfProgressBar(TimeLine.SINGLETON.getFrameCount());
		TimeLine.SINGLETON.setCurrentFrame(0);

		final String directory = _directory;
		Thread thread = new Thread(() ->
		{
			TimeLine.SINGLETON.setCurrentFrame(0);
			for (int i = 0; i < TimeLine.SINGLETON.getFrameCount(); i++)
			{
				final int finalI=i;
				bufferedImage = null;
				try
				{
					SwingUtilities.invokeAndWait(() ->
					{
						//TODO: this leads to terrible flickering sometimes in the mainpanel
						//(repro: lightbulb example, time slider in the middle)
						
						bufferedImage = MainFrame.SINGLETON.MAIN_PANEL.getBufferedImage(imageWidth, imageHeight, textEnabled);
						progressDialog.updateProgressBar(finalI);
						if(finalI!=0)
							TimeLine.SINGLETON.nextFrame();
					});
				}
				catch (Exception _e1)
				{
					Telemetry.trackException(_e1);
				}

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

				if (_selectedOutputFormat.isMovieFile() && started)
				{
					writer.encodeVideo(0, bufferedImage, msPerFrame * i, TimeUnit.MILLISECONDS);
				}
				else if (_selectedOutputFormat.isCompressedFile() && started)
				{
					String number = String.format("%04d", i);
					try
					{
						zipOutputStream.putNextEntry(new ZipEntry(_filename + "/" + Files.getNameWithoutExtension(_filename) + "-" + number
								+ _selectedOutputFormat.getInnerMovieFilter().getDefaultExtension()));
						ImageIO.write(bufferedImage, _selectedOutputFormat.getInnerMovieFilter().fileType, zipOutputStream);
						zipOutputStream.closeEntry();
					}
					catch (IOException e)
					{
						Telemetry.trackException(e);
					}
				}
				else if (_selectedOutputFormat.isImageFile() && started)
				{
					String number = String.format("%04d", i);
					try
					{
						ImageIO.write(bufferedImage, _selectedOutputFormat.fileType, new File(
								directory + Files.getNameWithoutExtension(_filename) + "-" + number + "." + Files.getFileExtension(_filename)));
					}
					catch (IOException e)
					{
						Telemetry.trackException(e);
					}
				}
			}

			try
			{
				// export movie
				if (writer != null)
					writer.close();
				if (zipOutputStream != null)
					zipOutputStream.close();
				if (fileOutputStream != null)
					fileOutputStream.close();
			}
			catch (IOException e)
			{
				Telemetry.trackException(e);
			}

			progressDialog.dispose();
		}, "Movie export");
		thread.start();
	}

	private void loadSettings()
	{
		textEnabled = Settings.getBoolean(BooleanKey.MOVIE_TEXT);
		imageWidth = Settings.getInt(IntKey.MOVIE_IMG_WIDTH);
		imageHeight = Settings.getInt(IntKey.MOVIE_IMG_HEIGHT);

		// default settings if nothing was specified so far
		if (imageWidth < 2 || imageHeight < 2)
		{
			imageWidth = 1280;
			imageHeight = 720;
		}
	}

	private void cancelMovie()
	{
		started = false;
	}

	//TODO: needs a redesign
	private class ProgressDialog extends JDialog
	{
		private final JPanel contentPanel = new JPanel();
		private JProgressBar progressBar;
		private JButton btnCancel;
		private JLabel lblDescription;

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

				btnCancel = new JButton("Cancel");
				buttonPane.add(btnCancel);
				btnCancel.addActionListener(a ->
				{
					SaveMovieAsAction.this.cancelMovie();
					dispose();
				});
			}

			pack();
			setLocationRelativeTo(MainFrame.SINGLETON);
		}

		public void setMaximumOfProgressBar(int maximum)
		{
			progressBar.setMaximum(maximum);
		}

		private void updateProgressBar(int value)
		{
			progressBar.setValue(value);
		}

		public void setDescription(String description)
		{
			lblDescription.setText(description);
		}

		@Override
		public void dispose()
		{
			MainFrame.SINGLETON.setEnabled(true);
			super.dispose();
		}
	}
}
