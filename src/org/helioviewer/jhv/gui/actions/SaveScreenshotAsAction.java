package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.Globals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.Globals.DialogType;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.filefilters.PredefinedFileFilter;

import com.google.common.io.Files;

/**
 * Action to save a screenshot in desired image format at desired location.
 * 
 * <p>
 * Therefore, opens a save dialog to choose format, name and location.
 */
public class SaveScreenshotAsAction extends AbstractAction
{
	private static final String SETTING_SCREENSHOT_IMG_WIDTH = "export.screenshot.image.width";
	private static final String SETTING_SCREENSHOT_IMG_HEIGHT = "export.screenshot.image.height";
	private static final String SETTING_SCREENSHOT_TEXT = "export.screenshot.text";
	private static final String SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY = "export.screenshot.last.directory";

	private int imageWidth;
	private int imageHeight;

	private boolean textEnabled;

	public SaveScreenshotAsAction() {
		super("Save screenshot as...");
		putValue(SHORT_DESCRIPTION, "Save screenshots to a file");
		putValue(
				ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK
						| Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	private void openFileChooser()
	{
		this.loadSettings();
		final File selectedFile = Globals.showFileDialog(
				DialogType.SAVE_FILE,
				"Save screenshot",
				Settings.getProperty(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY),
				false,
				getDefaultFileName(),
				PredefinedFileFilter.PNG_SINGLE,
				PredefinedFileFilter.JPG_SINGLE);
		
		if (selectedFile == null)
			return;
		
		Settings.setProperty(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY, selectedFile.getParent());
		startSavingScreenshot(selectedFile, null);
	}

	private void openFileChooserFX() {
		Platform.runLater(new Runnable() {

			@Override
			public void run()
			{
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save screenshot");
				fileChooser.setInitialFileName(getDefaultFileName());
				String val = Settings.getProperty(SETTING_SCREENSHOT_EXPORT_LAST_DIRECTORY);
				if (val != null && val.length() != 0)
				{
					File file = new File(val);
					if(file.exists())
						fileChooser.setInitialDirectory(file);
				}

				fileChooser.getExtensionFilters().addAll(
						PredefinedFileFilter.JPG_SINGLE.extensionFilter,
						PredefinedFileFilter.PNG_SINGLE.extensionFilter);
				final File selectedFile = fileChooser.showSaveDialog(new Stage());

				if (selectedFile != null)
					startSavingScreenshot(selectedFile, selectedFile.getName().substring(selectedFile.getName().lastIndexOf(".")+1));
			}
		});
	}

	private void startSavingScreenshot(final File selectedFile, final String extension)
	{
		loadSettings();
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					ImageIO.write(MainFrame.MAIN_PANEL.getBufferedImage(
							imageWidth, imageHeight, textEnabled),
							Files.getFileExtension(selectedFile.getPath()), selectedFile);
				}
				catch (IOException e1)
				{
					Telemetry.trackException(e1);
				}
			}
		});	
	}

	/**
	 * {@inheritDoc}
	 */
	public void actionPerformed(ActionEvent e) {
		if (Globals.USE_JAVA_FX_FILE_DIALOG){
			openFileChooserFX();
		}
		else {
			openFileChooser();
		}
	}

	private void loadSettings() {
		String val;
		try {
			val = Settings.getProperty(SETTING_SCREENSHOT_TEXT);
			if (val != null && !(val.length() == 0)) {
				this.textEnabled = Boolean.parseBoolean(val);
			}
		} catch (Throwable t) {
			Telemetry.trackException(t);
		}

		try {
			val = Settings.getProperty(SETTING_SCREENSHOT_IMG_HEIGHT);
			if (val != null && !(val.length() == 0)) {
				this.imageHeight = Integer.parseInt(val);
			}
		} catch (Throwable t) {
			Telemetry.trackException(t);
		}

		try {
			val = Settings.getProperty(SETTING_SCREENSHOT_IMG_WIDTH);
			if (val != null && !(val.length() == 0)) {
				this.imageWidth = Integer.parseInt(val);
			}
		} catch (Throwable t) {
			Telemetry.trackException(t);
		}

		if (imageWidth == 0)
			imageWidth = 1280;

		if (imageHeight == 0)
			imageHeight = 720;
	}

	/**
	 * Returns the default name for a screenshot. The name consists of
	 * "JHV_screenshot_created" plus the current system date and time.
	 * 
	 * @return Default name for a screenshot.
	 */
	private String getDefaultFileName() {
		String output = new String("JHV_screenshot_created_");

		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd_HH.mm.ss");
		output += dateFormat.format(new Date());

		return output;
	}
}
