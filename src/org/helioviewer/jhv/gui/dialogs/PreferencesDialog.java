package org.helioviewer.jhv.gui.dialogs;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

/**
 * Dialog that allows the user to change default preferences and settings.
 */
public class PreferencesDialog extends JDialog
{
	private final String defaultDateFormat = "yyyy/MM/dd";

	private JRadioButton loadDefaultMovieOnStartUp;
	private JRadioButton doNothingOnStartUp;
	private JTextField dateFormatField;

	private ScreenshotExportPanel screenshotExportPanel;
	private MovieExportPanel movieExportPanel;

	private JButton acceptBtn;
	private JButton cancelBtn;
	private JButton resetBtn;

	private static final int MAX_SIZE_SCREENSHOT = 4096;
	private static final int MAX_SIZE_MOVIE_EXPORT = 4096;

	private static final AspectRatio[] MOVIE_ASPECT_RATIO_PRESETS = {
			new AspectRatio(1, 1), new AspectRatio(4, 3),
			new AspectRatio(16, 9), new AspectRatio(16, 10),
			new AspectRatio(0, 0) };
	private static final AspectRatio[] IMAGE_ASPECT_RATIO_PRESETS = {
			new AspectRatio(1, 1), new AspectRatio(4, 3),
			new AspectRatio(16, 9), new AspectRatio(16, 10),
			new AspectRatio(0, 0) };

	/**
	 * The private constructor that sets the fields and the dialog.
	 */
	@SuppressWarnings("null")
	public PreferencesDialog()
	{
		super(MainFrame.SINGLETON, "Preferences", true);
		
		Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());
		
		setResizable(false);

		JPanel mainPanel = new JPanel(new BorderLayout());

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JPanel paramsSubPanel = new JPanel(new BorderLayout());
		paramsSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		paramsSubPanel.add(createParametersPanel(), BorderLayout.CENTER);

		JPanel exportSettings = new JPanel(new BorderLayout());

		JPanel movieExportSubPanel = new JPanel(new BorderLayout());
		movieExportSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		movieExportSubPanel.add(createMovieExportPanel(), BorderLayout.CENTER);

		JPanel screenshotExportSubPanel = new JPanel(new BorderLayout());
		screenshotExportSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		screenshotExportSubPanel.add(createScreenshotExportPanel(), BorderLayout.CENTER);

		exportSettings.add(movieExportSubPanel, BorderLayout.NORTH);
		exportSettings.add(screenshotExportSubPanel, BorderLayout.CENTER);

		panel.add(paramsSubPanel, BorderLayout.NORTH);
		panel.add(exportSettings, BorderLayout.SOUTH);

		mainPanel.add(panel, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		acceptBtn = new JButton("OK");
		cancelBtn = new JButton("Cancel");
		resetBtn = new JButton("Reset");

		acceptBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if (!isDateFormatValid(dateFormatField.getText()))
				{
					JOptionPane.showMessageDialog(
							PreferencesDialog.this,
							"Syntax error",
							"The entered date pattern contains illegal signs!\nAll suppported signs are listed in the associated information dialog.",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				saveSettings();
				dispose();
			}
		});

		cancelBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(@Nullable ActionEvent e)
			{
				dispose();
			}
		});

		resetBtn.addActionListener(new ActionListener()
		{
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if (JOptionPane.showConfirmDialog(null,
						"Do you really want to reset the Preferences?",
						"Attention", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
				{
					loadDefaultMovieOnStartUp.setSelected(true);
					dateFormatField.setText(defaultDateFormat);
				}
			}
		});

		if (Globals.isWindows())
		{
			btnPanel.add(acceptBtn);
			btnPanel.add(resetBtn);
			btnPanel.add(cancelBtn);
		}
		else
		{
			btnPanel.add(resetBtn);
			btnPanel.add(cancelBtn);
			btnPanel.add(acceptBtn);
		}

		mainPanel.add(btnPanel, BorderLayout.SOUTH);

		getContentPane().add(mainPanel);
		
		loadSettings();

		pack();
		setSize(getPreferredSize());
		setLocationRelativeTo(MainFrame.SINGLETON);
		DialogTools.setDefaultButtons(acceptBtn, cancelBtn);

		setVisible(true);
	}

	/**
	 * Checks the passed pattern if it is a supported date pattern. The pattern
	 * could contain defined letters and special characters. The method checks
	 * valid signs only!
	 * 
	 * @param format
	 *            pattern to check.
	 * @return boolean value if pattern is supported.
	 */
	private boolean isDateFormatValid(String format)
	{
		// go through all signs of pattern
		for (int i = 0; i < format.length(); i++)
		{
			char sign = format.charAt(i);
			int ascii = (int) sign;

			// if it is a number or letter, check it if it is supported
			if ((ascii >= 48 && ascii <= 57) || (ascii >= 65 && ascii <= 90) || (ascii >= 97 && ascii <= 122))
				if (sign != 'y' && sign != 'M' && sign != 'd' && sign != 'w' && sign != 'D' && sign != 'E')
					return false;
		}

		return true;
	}

	/**
	 * Loads the settings.
	 * 
	 * Reads the informations from {@link org.helioviewer.jhv.base.Settings} and sets
	 * all gui elements according to them.
	 */
	private void loadSettings()
	{
		// Start up
		loadDefaultMovieOnStartUp.setSelected(Settings.getBoolean("startup.loadmovie"));
		doNothingOnStartUp.setSelected(!Settings.getBoolean("startup.loadmovie"));
		
		// Default date format
		String fmt = Settings.getString("default.date.format");

		if (fmt == null)
			dateFormatField.setText(defaultDateFormat);
		else
			dateFormatField.setText(fmt);

		// Default values
		movieExportPanel.loadSettings();
		screenshotExportPanel.loadSettings();
	}

	/**
	 * Saves the settings.
	 * 
	 * Writes the informations to {@link org.helioviewer.jhv.base.Settings}.
	 */
	private void saveSettings()
	{
		// Start up
		Settings.setBoolean("startup.loadmovie", loadDefaultMovieOnStartUp.isSelected());
		
		// Default date format
		Settings.setString("default.date.format", dateFormatField.getText());
		
		// Default values
		movieExportPanel.saveSettings();
		screenshotExportPanel.saveSettings();
	}

	/**
	 * Creates the general parameters panel.
	 * 
	 * @return General parameters panel
	 */
	private JPanel createParametersPanel()
	{
		JPanel paramsPanel = new JPanel();

		paramsPanel.setBorder(BorderFactory.createTitledBorder(" Configuration "));
		paramsPanel.setLayout(new GridLayout(0, 1));

		JPanel row0 = new JPanel(new FlowLayout(FlowLayout.LEADING));
		row0.add(new JLabel("At start-up: "));

		loadDefaultMovieOnStartUp = new JRadioButton("Load default movie", true);
		doNothingOnStartUp = new JRadioButton("Do nothing", false);

		ButtonGroup buttonGroup = new ButtonGroup();
		buttonGroup.add(loadDefaultMovieOnStartUp);
		buttonGroup.add(doNothingOnStartUp);

		row0.add(loadDefaultMovieOnStartUp);
		row0.add(doNothingOnStartUp);
		paramsPanel.add(row0);

		dateFormatField = new JTextField();
		dateFormatField.setPreferredSize(new Dimension(150, 23));

		JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
		row2.add(new JLabel("Default date format:  "));
		row2.add(dateFormatField);
		Icon infoIcon = IconBank.getIcon(JHVIcon.INFO);

		JButton dateFormatInfo = new JButton(infoIcon);
		dateFormatInfo.setBorder(BorderFactory.createEtchedBorder());
		dateFormatInfo.setPreferredSize(new Dimension(infoIcon.getIconWidth() + 5, 23));
		dateFormatInfo.setToolTipText("Show possible date format information");
		dateFormatInfo.addActionListener(new ActionListener()
		{
			public void actionPerformed(@Nullable ActionEvent e)
			{
				new DateFormatInfoDialog();
			}
		});

		row2.add(dateFormatInfo);
		paramsPanel.add(row2);

		return paramsPanel;
	}

	/**
	 * Creates the default movie export panel.
	 * 
	 * @return Default movie export panel
	 */
	private JPanel createMovieExportPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(" Movie export "));

		movieExportPanel = new MovieExportPanel();
		movieExportPanel.setPreferredSize(new Dimension(400, 120));
		movieExportPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(movieExportPanel, BorderLayout.CENTER);

		return panel;
	}

	private static class MovieExportPanel extends JPanel
	{
		private JComboBox<AspectRatio> movieAspectRatioSelection;
		private JFormattedTextField txtMovieImageWidth, txtMovieImageHeight;
		private JCheckBox isTextEnabled;
		private static final String SETTING_MOVIE_IMG_WIDTH = "export.movie.image.width";
		private static final String SETTING_MOVIE_IMG_HEIGHT = "export.movie.image.height";
		private static final String SETTING_MOVIE_TEXT = "export.movie.text";
		private boolean hasChanged = false;

		public MovieExportPanel()
		{
			this.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"), }, new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC, }));
			{
				JLabel lblAspectRation = new JLabel("Aspect ratio");
				this.add(lblAspectRation, "2, 4, right, default");
			}
			{
				movieAspectRatioSelection = new JComboBox<>(
						MOVIE_ASPECT_RATIO_PRESETS);
				this.add(movieAspectRatioSelection, "4, 4, left, default");
				movieAspectRatioSelection.addItemListener(new ItemListener() {

					@Override
					public void itemStateChanged(@Nullable ItemEvent e) {
						AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection
								.getSelectedItem();
						if (aspectRatio.width != 0) {
							int width = Integer.parseInt(txtMovieImageWidth
									.getText());
							// txtMovieImageWidth.setValue(txtMovieImageWidth.getValue());
							hasChanged = true;
							txtMovieImageHeight.setValue(width
									* aspectRatio.height
									/ aspectRatio.width);
						}
						hasChanged = false;
					}
				});
			}
			{
				isTextEnabled = new JCheckBox("Render time stamps");
				this.add(isTextEnabled, "8,4,left, default");
			}
			NumberFormat format = NumberFormat.getInstance();
			format.setGroupingUsed(false);
			final NumberFormatter formatter = new NumberFormatter(format);
			formatter.setValueClass(Integer.class);
			formatter.setCommitsOnValidEdit(true);
			formatter.setMinimum(1);
			formatter.setMaximum(MAX_SIZE_MOVIE_EXPORT);
			{
				JLabel lblImageWidth = new JLabel("Image width");
				this.add(lblImageWidth, "2, 6, right, default");
			}
			{
				txtMovieImageWidth = new JFormattedTextField(formatter);
				txtMovieImageWidth.setValue(1280);
				txtMovieImageWidth.addPropertyChangeListener("value",
						new PropertyChangeListener() {

							@Override
							public void propertyChange(@Nullable PropertyChangeEvent pe) {
								if (!hasChanged) {
									AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection

									.getSelectedItem();
									if (aspectRatio.height != 0) {
										int width = (int) txtMovieImageWidth
												.getValue();
										hasChanged = true;
										txtMovieImageHeight.setValue(width
												* aspectRatio.height
												/ aspectRatio.width);
									}
								} else
									hasChanged = false;
							}
						});
				this.add(txtMovieImageWidth, "4, 6, left, default");
				txtMovieImageWidth.setColumns(10);
				txtMovieImageWidth.setToolTipText("value between 1 to 4096");
			}
			{
				JLabel lblImageHeight = new JLabel("Image height");
				this.add(lblImageHeight, "2, 8, right, default");
			}
			{
				txtMovieImageHeight = new JFormattedTextField(formatter);
				txtMovieImageHeight.setValue(720);
				txtMovieImageHeight.addPropertyChangeListener("value",
						new PropertyChangeListener() {

							@Override
							public void propertyChange(@Nullable PropertyChangeEvent evt) {
								if (!hasChanged) {
									AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection

									.getSelectedItem();
									if (aspectRatio.height != 0) {
										int heigth = (int) txtMovieImageHeight
												.getValue();
										hasChanged = true;
										txtMovieImageWidth.setValue(heigth
												* aspectRatio.width
												/ aspectRatio.height);
									}
								} else
									hasChanged = false;
							}
						});
				this.add(txtMovieImageHeight, "4, 8, left, default");
				txtMovieImageHeight.setColumns(10);
				txtMovieImageHeight.setToolTipText("value between 1 to 4096");
			}

		}

		public void loadSettings()
		{
			String val;
			try
			{
				val = Settings.getString(SETTING_MOVIE_TEXT);
				if (val != null && !(val.length() == 0))
					isTextEnabled.setSelected(Boolean.parseBoolean(val));
			}
			catch (Throwable t)
			{
				Telemetry.trackException(t);
			}

			try
			{
				val = Settings.getString(SETTING_MOVIE_IMG_HEIGHT);
				if (val != null && !(val.length() == 0))
					txtMovieImageHeight.setValue(Math.round(Float.parseFloat(val)));
			}
			catch (Throwable t)
			{
				Telemetry.trackException(t);
			}

			try
			{
				val = Settings.getString(SETTING_MOVIE_IMG_WIDTH);
				if (val != null && !(val.length() == 0))
					txtMovieImageWidth.setValue(Math.round(Float.parseFloat(val)));
			}
			catch (Throwable t)
			{
				Telemetry.trackException(t);
			}

			float ar = 16 / 9f;
			try
			{
				int width = Integer.parseInt(txtMovieImageWidth.getText());
				int height = Integer.parseInt(txtMovieImageHeight.getText());
				ar = width / (float) height;
			}
			catch (Exception _e)
			{
				Telemetry.trackException(_e);
			}

			movieAspectRatioSelection.setSelectedItem(MOVIE_ASPECT_RATIO_PRESETS[MOVIE_ASPECT_RATIO_PRESETS.length - 1]);
			for (AspectRatio asp : MOVIE_ASPECT_RATIO_PRESETS)
				if (Math.abs(asp.width / (float) asp.height - ar) < 0.01)
				{
					movieAspectRatioSelection.setSelectedItem(asp);
					break;
				}
		}

		public void saveSettings()
		{
			Settings.setString(SETTING_MOVIE_TEXT, isTextEnabled.isSelected() + "");
			Settings.setString(SETTING_MOVIE_IMG_WIDTH, txtMovieImageWidth.getValue().toString());
			Settings.setString(SETTING_MOVIE_IMG_HEIGHT, txtMovieImageHeight.getValue().toString());
		}

	}

	private JPanel createScreenshotExportPanel()
	{
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(" Screenshot export "));

		screenshotExportPanel = new ScreenshotExportPanel();
		screenshotExportPanel.setPreferredSize(new Dimension(400, 120));
		screenshotExportPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(screenshotExportPanel, BorderLayout.CENTER);
		return panel;
	}

	private static class ScreenshotExportPanel extends JPanel
	{
		private JComboBox<AspectRatio> screenshotAspectRatioSelection;
		private JFormattedTextField txtScreenshotImageWidth, txtScreenshotImageHeight;
		private JCheckBox isTextEnabled;
		private static final String SETTING_SCREENSHOT_IMG_WIDTH = "export.screenshot.image.width";
		private static final String SETTING_SCREENSHOT_IMG_HEIGHT = "export.screenshot.image.height";
		private static final String SETTING_SCREENSHOT_TEXT = "export.screenshot.text";
		private boolean hasChanged = false;
		
		public ScreenshotExportPanel()
		{
			this.setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"), }, new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC, }));
			{
				JLabel lblAspectRation = new JLabel("Aspect ratio");
				this.add(lblAspectRation, "2, 4, right, default");
			}
			{
				screenshotAspectRatioSelection = new JComboBox<>(
						IMAGE_ASPECT_RATIO_PRESETS);
				this.add(screenshotAspectRatioSelection, "4, 4, left, default");
				screenshotAspectRatioSelection
						.addItemListener(new ItemListener() {

							@Override
							public void itemStateChanged(@Nullable ItemEvent e) {
								AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection
										.getSelectedItem();
								if (aspectRatio.width != 0) {
									int width = Integer
											.parseInt(txtScreenshotImageWidth
													.getText());
									hasChanged = true;
									txtScreenshotImageHeight.setValue(width
											* aspectRatio.height
											/ aspectRatio.width);
								}
								hasChanged = false;
							}
						});
			}
			{
				isTextEnabled = new JCheckBox("Render time stamps");
				this.add(isTextEnabled, "8,4,left, default");
			}

			NumberFormat format = NumberFormat.getInstance();
			format.setGroupingUsed(false);
			NumberFormatter formatter = new NumberFormatter(format);
			formatter.setValueClass(Integer.class);
			formatter.setMinimum(1);
			formatter.setMaximum(MAX_SIZE_SCREENSHOT);
			{
				JLabel lblImageWidth = new JLabel("Image width");
				this.add(lblImageWidth, "2, 6, right, default");
			}
			{
				txtScreenshotImageWidth = new JFormattedTextField(formatter);
				txtScreenshotImageWidth.setValue(1280);
				txtScreenshotImageWidth.addPropertyChangeListener("value",
						new PropertyChangeListener() {

							@Override
							public void propertyChange(@Nullable PropertyChangeEvent evt) {
								if (!hasChanged) {
									AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection

									.getSelectedItem();
									if (aspectRatio.height != 0) {
										int width = Integer
												.parseInt(txtScreenshotImageWidth
														.getText());
										hasChanged = true;
										txtScreenshotImageHeight.setValue(width
												* aspectRatio.height
												/ aspectRatio.width);
									}
								} else
									hasChanged = false;
							}
						});
				this.add(txtScreenshotImageWidth, "4, 6, left, default");
				txtScreenshotImageWidth.setColumns(10);
				txtScreenshotImageWidth
						.setToolTipText("value between 1 to 4096");
			}
			{
				JLabel lblImageHeight = new JLabel("Image height");
				this.add(lblImageHeight, "2, 8, right, default");
			}
			{
				txtScreenshotImageHeight = new JFormattedTextField(formatter);
				txtScreenshotImageHeight.setValue(720);
				txtScreenshotImageHeight.addPropertyChangeListener("value",
						new PropertyChangeListener() {

							@Override
							public void propertyChange(@Nullable PropertyChangeEvent evt) {
								if (!hasChanged) {
									AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection

									.getSelectedItem();
									if (aspectRatio.height != 0) {
										int heigth = Integer
												.parseInt(txtScreenshotImageHeight
														.getText());
										hasChanged = true;
										txtScreenshotImageWidth.setValue(heigth
												* aspectRatio.width
												/ aspectRatio.height);
									}
								} else
									hasChanged = false;
							}
						});
				this.add(txtScreenshotImageHeight, "4, 8, left, default");
				txtScreenshotImageHeight.setColumns(10);
				txtScreenshotImageHeight
						.setToolTipText("value between 1 to 4096");
			}

		}

		public void loadSettings() {
			String val;
			try {
				val = Settings.getString(SETTING_SCREENSHOT_TEXT);
				if (val != null && !(val.length() == 0))
					isTextEnabled.setSelected(Boolean.parseBoolean(val));
			}
			catch (Throwable t)
			{
				Telemetry.trackException(t);
			}

			try
			{
				val = Settings.getString(SETTING_SCREENSHOT_IMG_HEIGHT);
				if (val != null && !(val.length() == 0))
					txtScreenshotImageHeight.setValue(Math.round(Float.parseFloat(val)));
			}
			catch (Throwable t)
			{
				Telemetry.trackException(t);
			}

			try {
				val = Settings.getString(SETTING_SCREENSHOT_IMG_WIDTH);
				if (val != null && !(val.length() == 0)) {
					txtScreenshotImageWidth.setValue(Math.round(Float
							.parseFloat(val)));
				}
			} catch (Throwable t) {
				Telemetry.trackException(t);
			}

			float ar = 16 / 9f;
			try
			{
				int width = Integer.parseInt(txtScreenshotImageWidth.getText());
				int height = Integer.parseInt(txtScreenshotImageHeight.getText());
				ar = width / (float) height;
			}
			catch (Exception _e)
			{
				Telemetry.trackException(_e);
			}

			screenshotAspectRatioSelection.setSelectedItem(IMAGE_ASPECT_RATIO_PRESETS[IMAGE_ASPECT_RATIO_PRESETS.length - 1]);
			for (AspectRatio asp : IMAGE_ASPECT_RATIO_PRESETS)
				if (Math.abs(asp.width / (float) asp.height - ar) < 0.01)
				{
					screenshotAspectRatioSelection.setSelectedItem(asp);
					break;
				}
		}

		public void saveSettings()
		{
			Settings.setString(SETTING_SCREENSHOT_TEXT, isTextEnabled.isSelected() + "");
			Settings.setString(SETTING_SCREENSHOT_IMG_WIDTH, txtScreenshotImageWidth.getValue().toString());
			Settings.setString(SETTING_SCREENSHOT_IMG_HEIGHT, txtScreenshotImageHeight.getValue().toString());
		}
	}

	/**
	 * Class which stores aspect ratio information
	 */
	private static class AspectRatio
	{
		final int width;
		final int height;

		private AspectRatio(int _width, int _height)
		{
			width = _width;
			height = _height;
		}

		public String toString()
		{
			if (width == 0 || height == 0)
				return "Custom";
			else
				return width + " : " + height;
		}
	}
}
