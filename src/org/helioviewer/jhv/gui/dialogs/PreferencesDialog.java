package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.NumberFormatter;

import org.helioviewer.jhv.Directories;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.logging.LogSettings;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.interfaces.ShowableDialog;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Dialog that allows the user to change default preferences and settings.
 * 
 * @author Desmond Amadigwe
 * @author Benjamin Wamsler
 * @author Juan Pablo
 * @author Markus Langenberg
 * @author Andre Dau
 */
public class PreferencesDialog extends JDialog implements ShowableDialog{

	private static final long serialVersionUID = 1L;

	private final String defaultDateFormat = "yyyy/MM/dd";

	private JRadioButton loadDefaultMovieOnStartUp;
	private JRadioButton doNothingOnStartUp;
	private JPanel paramsPanel;
	private DefaultsSelectionPanel defaultsPanel;
	private JTextField dateFormatField;
	private JButton dateFormatInfo;

	private DateFormatInfoDialog dialog = new DateFormatInfoDialog();

	private ScreenshotExportPanel screenshotExportPanel;
	private MovieExportPanel movieExportPanel;

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
	public PreferencesDialog() {

		super(ImageViewerGui.getMainFrame(), "Preferences", true);
		setResizable(false);

		JPanel mainPanel = new JPanel(new BorderLayout());

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JPanel paramsSubPanel = new JPanel(new BorderLayout());
		paramsSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		paramsSubPanel.add(createParametersPanel(), BorderLayout.CENTER);

		JPanel defaultsSubPanel = new JPanel(new BorderLayout());
		defaultsSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		defaultsSubPanel.add(createDefaultSaveDirPanel(), BorderLayout.CENTER);

		JPanel exportSettings = new JPanel(new BorderLayout());

		JPanel movieExportSubPanel = new JPanel(new BorderLayout());
		movieExportSubPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3,
				3));
		movieExportSubPanel.add(createMovieExportPanel(), BorderLayout.CENTER);

		JPanel screenshotExportSubPanel = new JPanel(new BorderLayout());
		screenshotExportSubPanel.setBorder(BorderFactory.createEmptyBorder(3,
				3, 3, 3));
		screenshotExportSubPanel.add(createScreenshotExportPanel(),
				BorderLayout.CENTER);

		exportSettings.add(movieExportSubPanel, BorderLayout.NORTH);
		exportSettings.add(screenshotExportSubPanel, BorderLayout.CENTER);

		panel.add(paramsSubPanel, BorderLayout.NORTH);
		panel.add(defaultsSubPanel, BorderLayout.CENTER);
		panel.add(exportSettings, BorderLayout.SOUTH);

		mainPanel.add(panel, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		JButton acceptBtn = new JButton(" Accept ");
		JButton cancelBtn = new JButton(" Cancel ");
		JButton resetBtn = new JButton(" Reset ");

		acceptBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!isDateFormatValid(dateFormatField.getText())) {
					Message.err(
							"Syntax error",
							"The entered date pattern contains illegal signs!\nAll suppported signs are listed in the associated information dialog.",
							false);
					return;
				}

				saveSettings();
				ImageViewerGui.getSingletonInstance().updateComponents();
				dispose();
			}
		});

		cancelBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		resetBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				if (JOptionPane.showConfirmDialog(null,
						"Do you really want to reset the setting values?",
						"Attention", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					defaultsPanel.resetSettings();
					loadDefaultMovieOnStartUp.setSelected(true);
					dateFormatField.setText(defaultDateFormat);

				}
			}
		});

		if (System.getProperty("os.name").toUpperCase().contains("WIN")) {
			btnPanel.add(acceptBtn);
			btnPanel.add(resetBtn);
			btnPanel.add(cancelBtn);
		} else {
			btnPanel.add(resetBtn);
			btnPanel.add(cancelBtn);
			btnPanel.add(acceptBtn);
		}

		mainPanel.add(btnPanel, BorderLayout.SOUTH);

		getContentPane().add(mainPanel);
		pack();
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
	private boolean isDateFormatValid(String format) {

		// go through all signs of pattern
		for (int i = 0; i < format.length(); i++) {
			char sign = format.charAt(i);
			int ascii = (int) sign;

			// if it is a number or letter, check it if it is supported
			if ((ascii >= 48 && ascii <= 57) || (ascii >= 65 && ascii <= 90)
					|| (ascii >= 97 && ascii <= 122)) {
				if (sign != 'y' && sign != 'M' && sign != 'd' && sign != 'w'
						&& sign != 'D' && sign != 'E') {

					return false;
				}
			}
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public void showDialog() {

		loadSettings();

		pack();
		setSize(getPreferredSize());
		setLocationRelativeTo(ImageViewerGui.getMainFrame());
		setVisible(true);
	}

	/**
	 * Loads the settings.
	 * 
	 * Reads the informations from {@link org.helioviewer.jhv.Settings} and sets
	 * all gui elements according to them.
	 */
	private void loadSettings() {

		// In principle the settings have been previously loaded
		// settings.load();

		// Start up
		loadDefaultMovieOnStartUp.setSelected(Boolean.parseBoolean(Settings
				.getProperty("startup.loadmovie")));
		doNothingOnStartUp.setSelected(!Boolean.parseBoolean(Settings
				.getProperty("startup.loadmovie")));

		// Default date format
		String fmt = Settings.getProperty("default.date.format");

		if (fmt == null)
			dateFormatField.setText(defaultDateFormat);
		else
			dateFormatField.setText(fmt);

		// Default values
		defaultsPanel.loadSettings();
		movieExportPanel.loadSettings();
		screenshotExportPanel.loadSettings();
	}

	/**
	 * Saves the settings.
	 * 
	 * Writes the informations to {@link org.helioviewer.jhv.Settings}.
	 */
	private void saveSettings() {

		// Start up
		Settings.setProperty("startup.loadmovie",
				Boolean.toString(loadDefaultMovieOnStartUp.isSelected()));

		// Default date format
		Settings.setProperty("default.date.format", dateFormatField.getText());

		// Default values
		defaultsPanel.saveSettings();
		movieExportPanel.saveSettings();
		screenshotExportPanel.saveSettings();

		// Update and save settings
		Settings.apply();
		LogSettings.update();
	}

	/**
	 * Creates the general parameters panel.
	 * 
	 * @return General parameters panel
	 */
	private JPanel createParametersPanel() {
		paramsPanel = new JPanel();

		paramsPanel.setBorder(BorderFactory
				.createTitledBorder(" Configuration "));
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

		dateFormatInfo = new JButton(infoIcon);
		dateFormatInfo.setBorder(BorderFactory.createEtchedBorder());
		dateFormatInfo.setPreferredSize(new Dimension(
				infoIcon.getIconWidth() + 5, 23));
		dateFormatInfo.setToolTipText("Show possible date format information");
		dateFormatInfo.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				dialog.showDialog();
			}
		});

		row2.add(dateFormatInfo);
		paramsPanel.add(row2);

		return paramsPanel;
	}

	/**
	 * Creates the default save directories panel.
	 * 
	 * @return Default save directories panel
	 */
	private JPanel createDefaultSaveDirPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(" Defaults "));

		defaultsPanel = new DefaultsSelectionPanel();
		defaultsPanel.setPreferredSize(new Dimension(450, 100));
		defaultsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(defaultsPanel, BorderLayout.CENTER);

		return panel;
	}

	/**
	 * Creates the default movie export panel.
	 * 
	 * @return Default movie export panel
	 */
	private JPanel createMovieExportPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(" Movie export "));

		movieExportPanel = new MovieExportPanel();
		movieExportPanel.setPreferredSize(new Dimension(400, 120));
		movieExportPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		panel.add(movieExportPanel, BorderLayout.CENTER);

		return panel;
	}

	private class MovieExportPanel extends JPanel {

		private JComboBox<AspectRatio> movieAspectRatioSelection;
		private JFormattedTextField txtMovieImageWidth, txtMovieImageHeight;
		private static final String SETTING_MOVIE_IMG_WIDTH = "export.movie.image.width";
		private static final String SETTING_MOVIE_IMG_HEIGHT = "export.movie.image.height";
		private boolean hasChanged = false;
		/**
		 * 
		 */
		private static final long serialVersionUID = -4648762833065960026L;

		public MovieExportPanel() {

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
				this.add(lblAspectRation, "6, 4, right, default");
			}
			{
				movieAspectRatioSelection = new JComboBox<AspectRatio>(
						MOVIE_ASPECT_RATIO_PRESETS);
				this.add(movieAspectRatioSelection, "8, 4, left, default");
				movieAspectRatioSelection.addItemListener(new ItemListener() {

					@Override
					public void itemStateChanged(ItemEvent e) {
						AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection
								.getSelectedItem();
						if (aspectRatio.getWidth() != 0) {
							int width = Integer.parseInt(txtMovieImageWidth
									.getText());
							// txtMovieImageWidth.setValue(txtMovieImageWidth.getValue());
							hasChanged = true;
							txtMovieImageHeight.setValue(width
									* aspectRatio.getHeight()
									/ aspectRatio.getWidth());
						}
						hasChanged = false;
					}
				});
			}
			NumberFormat format = NumberFormat.getInstance();
			format.setGroupingUsed(false);
			NumberFormatter formatter = new NumberFormatter(format);
			formatter.setValueClass(Integer.class);
			formatter.setMinimum(1);
			formatter.setMaximum(2048);
			{
				JLabel lblImageWidth = new JLabel("Image width");
				this.add(lblImageWidth, "6, 6, right, default");
			}
			{
				txtMovieImageWidth = new JFormattedTextField(formatter);
				txtMovieImageWidth.setValue(1280);
				txtMovieImageWidth.addPropertyChangeListener("value",
						new PropertyChangeListener() {

							@Override
							public void propertyChange(PropertyChangeEvent pe) {
								if (!hasChanged) {
									AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection

									.getSelectedItem();
									if (aspectRatio.getHeight() != 0) {
										int width = Integer
												.parseInt(txtMovieImageWidth
														.getText());
										hasChanged = true;
										txtMovieImageHeight.setValue(width
												* aspectRatio.getHeight()
												/ aspectRatio.getWidth());
									}
								} else
									hasChanged = false;
							}
						});
				this.add(txtMovieImageWidth, "8, 6, left, default");
				txtMovieImageWidth.setColumns(10);
				txtMovieImageWidth.setToolTipText("value between 1 to 2048");
			}
			{
				JLabel lblImageHeight = new JLabel("Image height");
				this.add(lblImageHeight, "6, 8, right, default");
			}
			{
				txtMovieImageHeight = new JFormattedTextField(formatter);
				txtMovieImageHeight.setValue(720);
				txtMovieImageHeight.addPropertyChangeListener("value",
						new PropertyChangeListener() {

							@Override
							public void propertyChange(PropertyChangeEvent evt) {
								if (!hasChanged) {
									AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection

									.getSelectedItem();
									if (aspectRatio.getHeight() != 0) {
										int heigth = Integer
												.parseInt(txtMovieImageHeight
														.getText());
										hasChanged = true;
										txtMovieImageWidth.setValue(heigth
												* aspectRatio.getWidth()
												/ aspectRatio.getHeight());
									}
								} else
									hasChanged = false;
							}
						});
				this.add(txtMovieImageHeight, "8, 8, left, default");
				txtMovieImageHeight.setColumns(10);
				txtMovieImageHeight.setToolTipText("value between 1 to 2048");
			}

		}

		public void loadSettings() {
			String val;
			try {
				val = Settings.getProperty(SETTING_MOVIE_IMG_HEIGHT);
				if (val != null && !(val.length() == 0)) {
					txtMovieImageHeight.setValue(Math.round(Float
							.parseFloat(val)));
				}
			} catch (Throwable t) {
				Log.error(t);
			}

			try {
				val = Settings.getProperty(SETTING_MOVIE_IMG_WIDTH);
				if (val != null && !(val.length() == 0)) {
					txtMovieImageWidth.setValue(Math.round(Float
							.parseFloat(val)));
				}
			} catch (Throwable t) {
				Log.error(t);
			}

			float ar = 16 / 9f;
			try {
				int width = Integer.parseInt(txtMovieImageWidth.getText());
				int height = Integer.parseInt(txtMovieImageHeight.getText());
				ar = width / (float) height;
			} catch (Exception _e) {
			}

			movieAspectRatioSelection
					.setSelectedItem(MOVIE_ASPECT_RATIO_PRESETS[MOVIE_ASPECT_RATIO_PRESETS.length - 1]);
			for (AspectRatio asp : MOVIE_ASPECT_RATIO_PRESETS) {
				if (Math.abs(asp.width / (float) asp.height - ar) < 0.01) {
					movieAspectRatioSelection.setSelectedItem(asp);
					break;
				}
			}
		}

		public void saveSettings() {
			Settings.setProperty(SETTING_MOVIE_IMG_WIDTH, txtMovieImageWidth
					.getValue().toString());
			Settings.setProperty(SETTING_MOVIE_IMG_HEIGHT, txtMovieImageHeight
					.getValue().toString());
		}

	}

	private JPanel createScreenshotExportPanel() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(" Screenshot export "));

		screenshotExportPanel = new ScreenshotExportPanel();
		screenshotExportPanel.setPreferredSize(new Dimension(400, 120));
		screenshotExportPanel.setBorder(BorderFactory.createEmptyBorder(5, 5,
				5, 5));

		panel.add(screenshotExportPanel, BorderLayout.CENTER);

		return panel;
	}

	private class ScreenshotExportPanel extends JPanel {
		private JComboBox<AspectRatio> screenshotAspectRatioSelection;
		private JFormattedTextField txtScreenshotImageWidth,
				txtScreenshotImageHeight;
		private static final String SETTING_SCREENSHOT_IMG_WIDTH = "export.screenshot.image.width";
		private static final String SETTING_SCREENSHOT_IMG_HEIGHT = "export.screenshot.image.height";
		private boolean hasChanged = false;
		/**
		 * 
		 */
		private static final long serialVersionUID = -3998530177421043272L;

		public ScreenshotExportPanel() {

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
				this.add(lblAspectRation, "6, 4, right, default");
			}
			{
				screenshotAspectRatioSelection = new JComboBox<AspectRatio>(
						MOVIE_ASPECT_RATIO_PRESETS);
				this.add(screenshotAspectRatioSelection, "8, 4, left, default");
				screenshotAspectRatioSelection
						.addItemListener(new ItemListener() {

							@Override
							public void itemStateChanged(ItemEvent e) {
								AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection
										.getSelectedItem();
								if (aspectRatio.getWidth() != 0) {
									int width = Integer
											.parseInt(txtScreenshotImageWidth
													.getText());
									hasChanged = true;
									txtScreenshotImageHeight.setValue(width
											* aspectRatio.getHeight()
											/ aspectRatio.getWidth());
								}
								hasChanged = false;
							}
						});
			}
			NumberFormat format = NumberFormat.getInstance();
			format.setGroupingUsed(false);
			NumberFormatter formatter = new NumberFormatter(format);
			formatter.setValueClass(Integer.class);
			formatter.setMinimum(1);
			formatter.setMaximum(2048);
			{
				JLabel lblImageWidth = new JLabel("Image width");
				this.add(lblImageWidth, "6, 6, right, default");
			}
			{
				txtScreenshotImageWidth = new JFormattedTextField(formatter);
				txtScreenshotImageWidth.setValue(1280);
				txtScreenshotImageWidth.addPropertyChangeListener("value",
						new PropertyChangeListener() {

							@Override
							public void propertyChange(PropertyChangeEvent evt) {
								if (!hasChanged) {
									AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection

									.getSelectedItem();
									if (aspectRatio.getHeight() != 0) {
										int width = Integer
												.parseInt(txtScreenshotImageWidth
														.getText());
										hasChanged = true;
										txtScreenshotImageHeight.setValue(width
												* aspectRatio.getHeight()
												/ aspectRatio.getWidth());
									}
								} else
									hasChanged = false;
								}
						});
				this.add(txtScreenshotImageWidth, "8, 6, left, default");
				txtScreenshotImageWidth.setColumns(10);
				txtScreenshotImageWidth
						.setToolTipText("value between 1 to 2048");
			}
			{
				JLabel lblImageHeight = new JLabel("Image height");
				this.add(lblImageHeight, "6, 8, right, default");
			}
			{
				txtScreenshotImageHeight = new JFormattedTextField(formatter);
				txtScreenshotImageHeight.setValue(720);
				txtScreenshotImageHeight.addPropertyChangeListener("value",
						new PropertyChangeListener() {

							@Override
							public void propertyChange(PropertyChangeEvent evt) {
								if (!hasChanged) {
									AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection

									.getSelectedItem();
									if (aspectRatio.getHeight() != 0) {
										int heigth = Integer
												.parseInt(txtScreenshotImageHeight
														.getText());
										hasChanged = true;
										txtScreenshotImageWidth.setValue(heigth
												* aspectRatio.getWidth()
												/ aspectRatio.getHeight());
									}
								} else
									hasChanged = false;
								}
						});
				this.add(txtScreenshotImageHeight, "8, 8, left, default");
				txtScreenshotImageHeight.setColumns(10);
				txtScreenshotImageHeight
						.setToolTipText("value between 1 to 2048");
			}

		}

		public void loadSettings() {
			String val;
			try {
				val = Settings.getProperty(SETTING_SCREENSHOT_IMG_HEIGHT);
				if (val != null && !(val.length() == 0)) {
					txtScreenshotImageHeight.setValue(Math.round(Float
							.parseFloat(val)));
				}
			} catch (Throwable t) {
				Log.error(t);
			}

			try {
				val = Settings.getProperty(SETTING_SCREENSHOT_IMG_WIDTH);
				if (val != null && !(val.length() == 0)) {
					txtScreenshotImageWidth.setValue(Math.round(Float
							.parseFloat(val)));
				}
			} catch (Throwable t) {
				Log.error(t);
			}

			float ar = 16 / 9f;
			try {
				int width = Integer.parseInt(txtScreenshotImageWidth.getText());
				int height = Integer.parseInt(txtScreenshotImageHeight
						.getText());
				ar = width / (float) height;
			} catch (Exception _e) {
			}

			screenshotAspectRatioSelection
					.setSelectedItem(IMAGE_ASPECT_RATIO_PRESETS[IMAGE_ASPECT_RATIO_PRESETS.length - 1]);
			for (AspectRatio asp : IMAGE_ASPECT_RATIO_PRESETS) {
				if (Math.abs(asp.width / (float) asp.height - ar) < 0.01) {
					screenshotAspectRatioSelection.setSelectedItem(asp);
					break;
				}
			}
		}

		public void saveSettings() {
			Settings.setProperty(SETTING_SCREENSHOT_IMG_WIDTH,
					txtScreenshotImageWidth.getValue().toString());
			Settings.setProperty(SETTING_SCREENSHOT_IMG_HEIGHT,
					txtScreenshotImageHeight.getValue().toString());
		}

	}

	private class DefaultsSelectionPanel extends JPanel {

		private static final long serialVersionUID = 1L;

		private JTable table = null;
		private Object[][] tableData = null;

		public DefaultsSelectionPanel() {

			super(new BorderLayout());
			setPreferredSize(new Dimension(150, 180));

			tableData = new Object[][] {
					{ "Default local path",
							Settings.getProperty("default.local.path") },
					{ "Default remote path",
							Settings.getProperty("default.remote.path") } };

			table = new JTable(new DefaultTableModel(tableData, new String[] {
					"Description", "Value" }) {
				private static final long serialVersionUID = 1L;

				public boolean isCellEditable(int row, int column) {
					return ((row == 2) && (column == 1));
				}
			});

			table.setRowHeight(20);
			JScrollPane scrollPane = new JScrollPane(table);
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			// table.setFillsViewportHeight(true);

			table.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() != 2)
						return;

					int row = table.getSelectedRow();
					if (row >= 2)
						return;

					JFileChooser chooser = new JFileChooser((String) table
							.getModel().getValueAt(row, 1));
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

					if (chooser.showDialog(null, "Select") == JFileChooser.APPROVE_OPTION)
						table.getModel().setValueAt(
								chooser.getSelectedFile().toString(), row, 1);
				}
			});

			TableColumn col = table.getColumnModel().getColumn(0);
			col.setMaxWidth(150);
			col.setMinWidth(150);

			add(scrollPane, BorderLayout.CENTER);
		}

		public void loadSettings() {

			TableModel model = table.getModel();

			model.setValueAt(Settings.getProperty("default.local.path"), 0, 1);
			model.setValueAt(Settings.getProperty("default.remote.path"), 1, 1);
		}

		public void saveSettings() {

			TableModel model = table.getModel();

			Settings.setProperty("default.local.path", model.getValueAt(0, 1)
					.toString());
			Settings.setProperty("default.remote.path", model.getValueAt(1, 1)
					.toString());
		}

		public void resetSettings() {

			TableModel model = table.getModel();

			model.setValueAt(Directories.HOME.getPath(), 0, 1);
			model.setValueAt("jpip://delphi.nascom.nasa.gov:8090", 1, 1);
		}
	}

	@Override
	public void init() {
	}

	/**
	 * Class which stores aspect ratio information
	 * 
	 * @author Andre Dau
	 * 
	 */
	static class AspectRatio {
		private int width;
		private int height;

		public AspectRatio(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public String toString() {
			if (width == 0 || height == 0) {
				return "Custom";
			} else {
				return width + " : " + height;
			}
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

	}
}
