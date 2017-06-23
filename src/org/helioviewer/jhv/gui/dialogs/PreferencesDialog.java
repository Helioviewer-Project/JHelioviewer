package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.IntBuffer;
import java.text.NumberFormat;

import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.NumberFormatter;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.BooleanKey;
import org.helioviewer.jhv.base.Settings.IntKey;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;

import com.jogamp.opengl.GL2;

public class PreferencesDialog extends JDialog
{
	private JCheckBox loadDefaultMovieOnStartUp;
	private JTextField dateFormatField;

	private JButton acceptBtn;
	private JButton cancelBtn;
	private JButton resetBtn;

	private static int MAX_SIZE_SCREENSHOT = 2048; //opengl texture size
	private static int MAX_SIZE_MOVIE_EXPORT = 2048; //opengl texture size
	
	private static final AspectRatio[] MOVIE_ASPECT_RATIO_PRESETS = { new AspectRatio(1, 1), new AspectRatio(4, 3),
			new AspectRatio(16, 9), new AspectRatio(16, 10), new AspectRatio(0, 0) };
	private static final AspectRatio[] IMAGE_ASPECT_RATIO_PRESETS = { new AspectRatio(1, 1), new AspectRatio(4, 3),
			new AspectRatio(16, 9), new AspectRatio(16, 10), new AspectRatio(0, 0) };

	public static void setMaxSizeScreenshot(GL2 _gl)
	{
		try 
		{
			IntBuffer buffer = IntBuffer.allocate(1);
			_gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, buffer);
			
			int maxSize = buffer.get();
			
			if (maxSize > 1024 * 16)
			{
				maxSize = 1024 * 16;
			}
			
			MAX_SIZE_SCREENSHOT = maxSize;
			MAX_SIZE_MOVIE_EXPORT = maxSize;
		}
		catch (Exception e)
		{
			Telemetry.trackException(e);
		}
	}

	public PreferencesDialog()
	{
		super(MainFrame.SINGLETON, "Preferences", true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setResizable(false);

		Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		JPanel mainPanel = new JPanel(new BorderLayout());

		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		Icon infoIcon = IconBank.getIcon(JHVIcon.INFO);
		
		NumberFormat screenshotFormat = NumberFormat.getInstance();
		screenshotFormat.setGroupingUsed(false);
		final NumberFormatter screenshotFormatter = new NumberFormatter(screenshotFormat);
		screenshotFormatter.setValueClass(Integer.class);
		screenshotFormatter.setCommitsOnValidEdit(true);
		screenshotFormatter.setMinimum(1);
		screenshotFormatter.setMaximum(MAX_SIZE_SCREENSHOT);


		NumberFormat movieFormat = NumberFormat.getInstance();
		movieFormat.setGroupingUsed(false);
		final NumberFormatter movieFormatter = new NumberFormatter(movieFormat);
		movieFormatter.setValueClass(Integer.class);
		movieFormatter.setCommitsOnValidEdit(true);
		movieFormatter.setMinimum(1);
		movieFormatter.setMaximum(MAX_SIZE_MOVIE_EXPORT);
		
		
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		
		JPanel paramsPanel = new JPanel();
		panel.add(paramsPanel);
		
				paramsPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
								paramsPanel.setLayout(new GridLayout(0, 1, 0, 0));
								
								JPanel panel_1 = new JPanel();
								panel_1.setBorder(new EmptyBorder(5, 5, 0, 5));
								paramsPanel.add(panel_1);
								panel_1.setLayout(new BorderLayout(0, 0));
						
								loadDefaultMovieOnStartUp = new JCheckBox("Load default movie at startup", true);
								panel_1.add(loadDefaultMovieOnStartUp);
								loadDefaultMovieOnStartUp.setHorizontalAlignment(SwingConstants.LEFT);
										
												dateFormatField = new JTextField();
												dateFormatField.setPreferredSize(new Dimension(200, 26));
												
														JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
														row2.setAlignmentX(Component.LEFT_ALIGNMENT);
														row2.setBorder(new EmptyBorder(0, 5, 5, 5));
														row2.add(new JLabel("Date format: "));
														row2.add(dateFormatField);
														
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
				
				Component verticalStrut_1 = Box.createVerticalStrut(20);
				panel.add(verticalStrut_1);
				
				JPanel movieExportPanelBorder = new JPanel(new BorderLayout());
				panel.add(movieExportPanelBorder);
				movieExportPanelBorder.setBorder(BorderFactory.createTitledBorder("Movie export"));
				
						
						
						
						
						
						
						JPanel movieExportPanel = new JPanel();
						GridBagLayout gbl_movieExportPanel = new GridBagLayout();
						gbl_movieExportPanel.columnWidths = new int[]{120, 100, 50, 150, 0};
						gbl_movieExportPanel.rowHeights = new int[]{29, 26, 26, 0};
						gbl_movieExportPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
						gbl_movieExportPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
						movieExportPanel.setLayout(gbl_movieExportPanel);
						{
							JLabel lblAspectRation = new JLabel("Aspect ratio");
							GridBagConstraints gbc_lblAspectRation = new GridBagConstraints();
							gbc_lblAspectRation.anchor = GridBagConstraints.WEST;
							gbc_lblAspectRation.insets = new Insets(0, 0, 5, 5);
							gbc_lblAspectRation.gridx = 0;
							gbc_lblAspectRation.gridy = 0;
							movieExportPanel.add(lblAspectRation, gbc_lblAspectRation);
						}
						{
							movieAspectRatioSelection = new JComboBox<>();
							movieAspectRatioSelection.setModel(new DefaultComboBoxModel<>(MOVIE_ASPECT_RATIO_PRESETS));
							GridBagConstraints gbc_movieAspectRatioSelection = new GridBagConstraints();
							gbc_movieAspectRatioSelection.anchor = GridBagConstraints.WEST;
							gbc_movieAspectRatioSelection.insets = new Insets(0, 0, 5, 5);
							gbc_movieAspectRatioSelection.gridx = 1;
							gbc_movieAspectRatioSelection.gridy = 0;
							movieExportPanel.add(movieAspectRatioSelection, gbc_movieAspectRatioSelection);
							movieAspectRatioSelection.addItemListener(new ItemListener()
							{

								@Override
								public void itemStateChanged(@Nullable ItemEvent e)
								{
									AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection.getSelectedItem();
									if (aspectRatio.width != 0)
									{
										int width = Integer.parseInt(txtMovieImageWidth.getText());
										// txtMovieImageWidth.setValue(txtMovieImageWidth.getValue());
										movieChanged = true;
										txtMovieImageHeight.setValue(width * aspectRatio.height / aspectRatio.width);
									}
									movieChanged = false;
								}
							});
						}
						{
							movieTextEnabled = new JCheckBox("Render time stamps");
							GridBagConstraints gbc_movieTextEnabled = new GridBagConstraints();
							gbc_movieTextEnabled.anchor = GridBagConstraints.NORTHWEST;
							gbc_movieTextEnabled.insets = new Insets(0, 0, 5, 0);
							gbc_movieTextEnabled.gridx = 3;
							gbc_movieTextEnabled.gridy = 0;
							movieExportPanel.add(movieTextEnabled, gbc_movieTextEnabled);
						}
						{
							JLabel lblImageWidth = new JLabel("Image width");
							GridBagConstraints gbc_lblImageWidth = new GridBagConstraints();
							gbc_lblImageWidth.anchor = GridBagConstraints.WEST;
							gbc_lblImageWidth.insets = new Insets(0, 0, 5, 5);
							gbc_lblImageWidth.gridx = 0;
							gbc_lblImageWidth.gridy = 1;
							movieExportPanel.add(lblImageWidth, gbc_lblImageWidth);
						}
						
						NumberFormat format = NumberFormat.getInstance();
						format.setGroupingUsed(false);
						txtMovieImageWidth = new JFormattedTextField(movieFormatter);
						txtMovieImageWidth.setValue(1280);
						txtMovieImageWidth.addPropertyChangeListener("value", new PropertyChangeListener()
						{
							@Override
							public void propertyChange(@Nullable PropertyChangeEvent pe)
							{
								if (!movieChanged)
								{
									AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection
	
											.getSelectedItem();
									if (aspectRatio.height != 0)
									{
										int width = (int) txtMovieImageWidth.getValue();
										movieChanged = true;
										txtMovieImageHeight.setValue(width * aspectRatio.height / aspectRatio.width);
									}
								}
								else
									movieChanged = false;
							}
						});
						GridBagConstraints gbc_txtMovieImageWidth = new GridBagConstraints();
						gbc_txtMovieImageWidth.fill = GridBagConstraints.HORIZONTAL;
						gbc_txtMovieImageWidth.anchor = GridBagConstraints.NORTH;
						gbc_txtMovieImageWidth.insets = new Insets(0, 0, 5, 5);
						gbc_txtMovieImageWidth.gridx = 1;
						gbc_txtMovieImageWidth.gridy = 1;
						movieExportPanel.add(txtMovieImageWidth, gbc_txtMovieImageWidth);
						txtMovieImageWidth.setColumns(10);
						txtMovieImageWidth.setToolTipText("value between 1 to 4096");
						{
							JLabel lblImageHeight = new JLabel("Image height");
							GridBagConstraints gbc_lblImageHeight = new GridBagConstraints();
							gbc_lblImageHeight.anchor = GridBagConstraints.WEST;
							gbc_lblImageHeight.insets = new Insets(0, 0, 0, 5);
							gbc_lblImageHeight.gridx = 0;
							gbc_lblImageHeight.gridy = 2;
							movieExportPanel.add(lblImageHeight, gbc_lblImageHeight);
						}
						txtMovieImageHeight = new JFormattedTextField(movieFormatter);
						txtMovieImageHeight.setValue(720);
						txtMovieImageHeight.addPropertyChangeListener("value", new PropertyChangeListener()
						{

							@Override
							public void propertyChange(@Nullable PropertyChangeEvent evt)
							{
								if (!movieChanged)
								{
									AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection

											.getSelectedItem();
									if (aspectRatio.height != 0)
									{
										int heigth = (int) txtMovieImageHeight.getValue();
										movieChanged = true;
										txtMovieImageWidth.setValue(heigth * aspectRatio.width / aspectRatio.height);
									}
								}
								else
									movieChanged = false;
							}
						});
						GridBagConstraints gbc_txtMovieImageHeight = new GridBagConstraints();
						gbc_txtMovieImageHeight.fill = GridBagConstraints.HORIZONTAL;
						gbc_txtMovieImageHeight.anchor = GridBagConstraints.NORTH;
						gbc_txtMovieImageHeight.insets = new Insets(0, 0, 0, 5);
						gbc_txtMovieImageHeight.gridx = 1;
						gbc_txtMovieImageHeight.gridy = 2;
						movieExportPanel.add(txtMovieImageHeight, gbc_txtMovieImageHeight);
						txtMovieImageHeight.setColumns(10);
						txtMovieImageHeight.setToolTipText("value between 1 to 4096");
						movieExportPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
						
								movieExportPanelBorder.add(movieExportPanel, BorderLayout.CENTER);
				
				Component verticalStrut = Box.createVerticalStrut(20);
				panel.add(verticalStrut);
				
				JPanel screenshotExportPanelBorder = new JPanel(new BorderLayout());
				panel.add(screenshotExportPanelBorder);
				screenshotExportPanelBorder.setBorder(BorderFactory.createTitledBorder("Screenshot export"));
				
						JPanel screenshotExportPanel = new JPanel();
						GridBagLayout gbl_screenshotExportPanel = new GridBagLayout();
						gbl_screenshotExportPanel.columnWidths = new int[]{120, 100, 50, 150, 0};
						gbl_screenshotExportPanel.rowHeights = new int[]{29, 26, 26, 0};
						gbl_screenshotExportPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
						gbl_screenshotExportPanel.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
						screenshotExportPanel.setLayout(gbl_screenshotExportPanel);
						txtScreenshotImageHeight = new JFormattedTextField(screenshotFormatter);
						txtScreenshotImageHeight.setValue(720);
						txtScreenshotImageHeight.addPropertyChangeListener("value", new PropertyChangeListener()
						{

							@Override
							public void propertyChange(@Nullable PropertyChangeEvent evt)
							{
								if (!screenshotChanged)
								{
									AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection

											.getSelectedItem();
									if (aspectRatio.height != 0)
									{
										int heigth = Integer.parseInt(txtScreenshotImageHeight.getText());
										screenshotChanged = true;
										txtScreenshotImageWidth.setValue(heigth * aspectRatio.width / aspectRatio.height);
									}
								}
								else
									screenshotChanged = false;
							}
						});
						{
							JLabel lblAspectRation = new JLabel("Aspect ratio");
							GridBagConstraints gbc_lblAspectRation = new GridBagConstraints();
							gbc_lblAspectRation.anchor = GridBagConstraints.WEST;
							gbc_lblAspectRation.insets = new Insets(0, 0, 5, 5);
							gbc_lblAspectRation.gridx = 0;
							gbc_lblAspectRation.gridy = 0;
							screenshotExportPanel.add(lblAspectRation, gbc_lblAspectRation);
						}
						{
							screenshotAspectRatioSelection = new JComboBox<>();
							screenshotAspectRatioSelection.setModel(new DefaultComboBoxModel<>(IMAGE_ASPECT_RATIO_PRESETS));

							GridBagConstraints gbc_screenshotAspectRatioSelection = new GridBagConstraints();
							gbc_screenshotAspectRatioSelection.anchor = GridBagConstraints.WEST;
							gbc_screenshotAspectRatioSelection.insets = new Insets(0, 0, 5, 5);
							gbc_screenshotAspectRatioSelection.gridx = 1;
							gbc_screenshotAspectRatioSelection.gridy = 0;
							screenshotExportPanel.add(screenshotAspectRatioSelection, gbc_screenshotAspectRatioSelection);
							screenshotAspectRatioSelection.addItemListener(new ItemListener()
							{

								@Override
								public void itemStateChanged(@Nullable ItemEvent e)
								{
									AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection.getSelectedItem();
									if (aspectRatio.width != 0)
									{
										int width = Integer.parseInt(txtScreenshotImageWidth.getText());
										screenshotChanged = true;
										txtScreenshotImageHeight.setValue(width * aspectRatio.height / aspectRatio.width);
									}
									screenshotChanged = false;
								}
							});
						}
						{
							screenshotTextEnabled = new JCheckBox("Render time stamps");
							GridBagConstraints gbc_screenshotTextEnabled = new GridBagConstraints();
							gbc_screenshotTextEnabled.anchor = GridBagConstraints.NORTHWEST;
							gbc_screenshotTextEnabled.insets = new Insets(0, 0, 5, 0);
							gbc_screenshotTextEnabled.gridx = 3;
							gbc_screenshotTextEnabled.gridy = 0;
							screenshotExportPanel.add(screenshotTextEnabled, gbc_screenshotTextEnabled);
						}
						{
							JLabel lblImageWidth = new JLabel("Image width");
							GridBagConstraints gbc_lblImageWidth = new GridBagConstraints();
							gbc_lblImageWidth.anchor = GridBagConstraints.WEST;
							gbc_lblImageWidth.insets = new Insets(0, 0, 5, 5);
							gbc_lblImageWidth.gridx = 0;
							gbc_lblImageWidth.gridy = 1;
							screenshotExportPanel.add(lblImageWidth, gbc_lblImageWidth);
						}
						{
							txtScreenshotImageWidth = new JFormattedTextField(screenshotFormatter);
							txtScreenshotImageWidth.setValue(1280);
							txtScreenshotImageWidth.addPropertyChangeListener("value", new PropertyChangeListener()
							{

								@Override
								public void propertyChange(@Nullable PropertyChangeEvent evt)
								{
									if (!screenshotChanged)
									{
										AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection

												.getSelectedItem();
										if (aspectRatio.height != 0)
										{
											int width = Integer.parseInt(txtScreenshotImageWidth.getText());
											screenshotChanged = true;
											txtScreenshotImageHeight.setValue(width * aspectRatio.height / aspectRatio.width);
										}
									}
									else
										screenshotChanged = false;
								}
							});
							GridBagConstraints gbc_txtScreenshotImageWidth = new GridBagConstraints();
							gbc_txtScreenshotImageWidth.fill = GridBagConstraints.HORIZONTAL;
							gbc_txtScreenshotImageWidth.anchor = GridBagConstraints.NORTH;
							gbc_txtScreenshotImageWidth.insets = new Insets(0, 0, 5, 5);
							gbc_txtScreenshotImageWidth.gridx = 1;
							gbc_txtScreenshotImageWidth.gridy = 1;
							screenshotExportPanel.add(txtScreenshotImageWidth, gbc_txtScreenshotImageWidth);
							txtScreenshotImageWidth.setColumns(10);
							txtScreenshotImageWidth.setToolTipText("value between 1 to 4096");
						}
						{
							JLabel lblImageHeight = new JLabel("Image height");
							GridBagConstraints gbc_lblImageHeight = new GridBagConstraints();
							gbc_lblImageHeight.anchor = GridBagConstraints.WEST;
							gbc_lblImageHeight.insets = new Insets(0, 0, 0, 5);
							gbc_lblImageHeight.gridx = 0;
							gbc_lblImageHeight.gridy = 2;
							screenshotExportPanel.add(lblImageHeight, gbc_lblImageHeight);
						}
						GridBagConstraints gbc_txtScreenshotImageHeight = new GridBagConstraints();
						gbc_txtScreenshotImageHeight.fill = GridBagConstraints.HORIZONTAL;
						gbc_txtScreenshotImageHeight.anchor = GridBagConstraints.NORTH;
						gbc_txtScreenshotImageHeight.insets = new Insets(0, 0, 0, 5);
						gbc_txtScreenshotImageHeight.gridx = 1;
						gbc_txtScreenshotImageHeight.gridy = 2;
						screenshotExportPanel.add(txtScreenshotImageHeight, gbc_txtScreenshotImageHeight);
						txtScreenshotImageHeight.setColumns(10);
						txtScreenshotImageHeight.setToolTipText("value between 1 to 4096");
								screenshotExportPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
								
										screenshotExportPanelBorder.add(screenshotExportPanel, BorderLayout.CENTER);

		mainPanel.add(panel, BorderLayout.CENTER);

		FlowLayout fl_btnPanel = new FlowLayout(FlowLayout.RIGHT);
		JPanel btnPanel = new JPanel(fl_btnPanel);
		btnPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
		
		JPanel panel_2 = new JPanel();
		btnPanel.add(panel_2);
				panel_2.setLayout(new GridLayout(0, 3, 15, 0));
		
				acceptBtn = new JButton("OK");
				panel_2.add(acceptBtn);
				
						acceptBtn.addActionListener(new ActionListener()
						{
							public void actionPerformed(@Nullable ActionEvent e)
							{
								if (!isDateFormatValid(dateFormatField.getText()))
								{
									JOptionPane.showMessageDialog(PreferencesDialog.this, "Syntax error",
											"The entered date format contains illegal signs!\nAll suppported signs are listed in the associated information dialog.",
											JOptionPane.ERROR_MESSAGE);
									return;
								}
				
								saveSettings();
								dispose();
							}
						});
						resetBtn = new JButton("Reset");
						panel_2.add(resetBtn);
						cancelBtn = new JButton("Cancel");
						panel_2.add(cancelBtn);
						
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
								if (JOptionPane.showConfirmDialog(null, "Do you really want to reset all settings?", "Attention",
										JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
								{
									Settings.resetAllSettings();
									dispose();
								}
							}
						});
		
		if (Globals.IS_OS_X)
		{
			panel_2.remove(resetBtn);
			panel_2.remove(cancelBtn);
			panel_2.remove(acceptBtn);
			
			panel_2.add(resetBtn);
			panel_2.add(cancelBtn);
			panel_2.add(acceptBtn);
		}

		mainPanel.add(btnPanel, BorderLayout.SOUTH);

		getContentPane().add(mainPanel);

		loadSettings();
		
		DialogTools.setDefaultButtons(acceptBtn, cancelBtn);

		setSize(new Dimension(597, 579));
		setLocationRelativeTo(MainFrame.SINGLETON);

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
		for(char ch:format.toCharArray())
			// if it is a number or letter, check it if it is supported
			if ((ch >= 48 && ch <= 57) || (ch >= 65 && ch <= 90) || (ch >= 97 && ch <= 122))
				if (ch != 'y' && ch != 'M' && ch != 'd' && ch != 'w' && ch != 'D' && ch != 'E')
					return false;

		return true;
	}

	/**
	 * Loads the settings.
	 * 
	 * Reads the informations from {@link org.helioviewer.jhv.base.Settings} and
	 * sets all gui elements according to them.
	 */
	private void loadSettings()
	{
		// Start up
		loadDefaultMovieOnStartUp.setSelected(Settings.getBoolean(Settings.BooleanKey.STARTUP_LOADMOVIE));

		// Default date format
		dateFormatField.setText(Settings.getString(StringKey.DATE_FORMAT));

		loadMovieSettings();
		loadScreenshotSettings();
	}

	private void saveSettings()
	{
		// Start up
		Settings.setBoolean(BooleanKey.STARTUP_LOADMOVIE, loadDefaultMovieOnStartUp.isSelected());

		// Default date format
		Settings.setString(StringKey.DATE_FORMAT, dateFormatField.getText());

		saveMovieSettings();
		saveScreenshotSettings();
	}

	private JComboBox<AspectRatio> movieAspectRatioSelection;
	private JFormattedTextField txtMovieImageWidth, txtMovieImageHeight;
	private JCheckBox movieTextEnabled;
	private boolean movieChanged = false;

	public void loadMovieSettings()
	{
		movieTextEnabled.setSelected(Settings.getBoolean(BooleanKey.MOVIE_TEXT));

		txtMovieImageWidth.setValue(Settings.getInt(IntKey.MOVIE_IMG_WIDTH));
		txtMovieImageHeight.setValue(Settings.getInt(IntKey.MOVIE_IMG_HEIGHT));

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

		movieAspectRatioSelection
				.setSelectedItem(MOVIE_ASPECT_RATIO_PRESETS[MOVIE_ASPECT_RATIO_PRESETS.length - 1]);
		for (AspectRatio asp : MOVIE_ASPECT_RATIO_PRESETS)
			if (Math.abs(asp.width / (float) asp.height - ar) < 0.01)
			{
				movieAspectRatioSelection.setSelectedItem(asp);
				break;
			}
	}

	public void saveMovieSettings()
	{
		Settings.setBoolean(BooleanKey.MOVIE_TEXT, movieTextEnabled.isSelected());
		try
		{
			Settings.setInt(IntKey.MOVIE_IMG_WIDTH, Integer.parseInt(txtMovieImageWidth.getValue().toString()));
		}
		catch (NumberFormatException _nfe)
		{
		}
		try
		{
			Settings.setInt(IntKey.MOVIE_IMG_HEIGHT, Integer.parseInt(txtMovieImageHeight.getValue().toString()));
		}
		catch (NumberFormatException _nfe)
		{
		}
	}

	
	private JComboBox<AspectRatio> screenshotAspectRatioSelection;
	private JFormattedTextField txtScreenshotImageWidth, txtScreenshotImageHeight;
	private JCheckBox screenshotTextEnabled;
	private boolean screenshotChanged = false;

	public void loadScreenshotSettings()
	{
		screenshotTextEnabled.setSelected(Settings.getBoolean(BooleanKey.SCREENSHOT_TEXT));
		txtScreenshotImageWidth.setValue(Settings.getInt(IntKey.SCREENSHOT_IMG_WIDTH));
		txtScreenshotImageHeight.setValue(Settings.getInt(IntKey.SCREENSHOT_IMG_HEIGHT));

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

		screenshotAspectRatioSelection
				.setSelectedItem(IMAGE_ASPECT_RATIO_PRESETS[IMAGE_ASPECT_RATIO_PRESETS.length - 1]);
		for (AspectRatio asp : IMAGE_ASPECT_RATIO_PRESETS)
			if (Math.abs(asp.width / (float) asp.height - ar) < 0.01)
			{
				screenshotAspectRatioSelection.setSelectedItem(asp);
				break;
			}
	}

	public void saveScreenshotSettings()
	{
		Settings.setBoolean(BooleanKey.SCREENSHOT_TEXT, screenshotTextEnabled.isSelected());
		try
		{
			Settings.setInt(IntKey.SCREENSHOT_IMG_WIDTH,
					Integer.parseInt(txtScreenshotImageWidth.getValue().toString()));
		}
		catch (NumberFormatException _nfe)
		{
		}
		try
		{
			Settings.setInt(IntKey.SCREENSHOT_IMG_HEIGHT,
					Integer.parseInt(txtScreenshotImageHeight.getValue().toString()));
		}
		catch (NumberFormatException _nfe)
		{
		}
	}
	
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
