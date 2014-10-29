package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.text.NumberFormatter;

import org.helioviewer.base.logging.Log;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.ImageViewerGui;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class ExportMovieSettingsDialog extends JDialog implements
		ActionListener, ItemListener, PropertyChangeListener {
	/**
   * 
   */
  private static final long serialVersionUID=7202899812438717165L;

  private final JPanel contentPanel = new JPanel();

	private JComboBox<?> movieAspectRatioSelection,
			screenshotAspectRatioSelection;
	private JFormattedTextField txtMovieImageWidth, txtMovieImageHeight,
			txtScreenshotImageWidth, txtScreenshotImageHeight;
	// private JCheckBox movieUseCurrentOpenglSize,
	// screenshotUseCurrentOpenglSize;
	private JButton btnSave;
	private JButton btnCancel;
	private JPanel moviePanel, screenshotPanel;
	private JTabbedPane tabbedPane;

	private boolean hasChanged = false;

	private static final String SETTING_MOVIE_RATIO = "export.movie.aspect.ratio";
	private static final String SETTING_MOVIE_IMG_WIDTH = "export.movie.image.width";
	private static final String SETTING_MOVIE_IMG_HEIGHT = "export.movie.image.height";
	private static final String SETTING_SCREENSHOT_RATIO = "export.screenshot.aspect.ratio";
	private static final String SETTING_SCREENSHOT_IMG_WIDTH = "export.screenshot.image.width";
	private static final String SETTING_SCREENSHOT_IMG_HEIGHT = "export.screenshot.image.height";
	
	private static final AspectRatio[] movieAspectRatioPresets = {
			new AspectRatio(1, 1), new AspectRatio(4, 3),
			new AspectRatio(16, 9), new AspectRatio(16, 10),
			new AspectRatio(0, 0) };
	private static final AspectRatio[] screenshotAspectRatioPresets = {
			new AspectRatio(1, 1), new AspectRatio(4, 3),
			new AspectRatio(16, 9), new AspectRatio(16, 10),
			new AspectRatio(0, 0) };

	/**
	 * Launch the application.
	 */

	/**
	 * Create the dialog.
	 */
	public ExportMovieSettingsDialog() {
		super(ImageViewerGui.getMainFrame(), "Movie export settings", true);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.initGui();
		this.loadMovieSettings();
		this.loadScreenshotSettings();
	}

	private void initGui() {
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);

		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			tabbedPane = new JTabbedPane(JTabbedPane.TOP);
			contentPanel.add(tabbedPane, BorderLayout.CENTER);
			{
				this.moviePanel = new JPanel();
				tabbedPane.addTab("Movie", null, moviePanel, null);
			}
			{
				this.screenshotPanel = new JPanel();
				tabbedPane.addTab("Screenshot", null, screenshotPanel, null);
			}
		}

		this.initMovieGui();
		this.initScreenshotGui();
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				btnSave = new JButton("Save");
				btnSave.addActionListener(this);
				btnSave.setActionCommand("Save");
				buttonPane.add(btnSave);
				getRootPane().setDefaultButton(btnSave);
			}
			{
				btnCancel = new JButton("Cancel");
				btnCancel.addActionListener(this);
				btnCancel.setActionCommand("Cancel");
				buttonPane.add(btnCancel);
			}

		}

		this.pack();
		this.setLocationRelativeTo(ImageViewerGui.getMainFrame());

	}

	private void initMovieGui() {

		moviePanel
				.setLayout(new FormLayout(new ColumnSpec[] {
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
			JLabel lblVideoSize = new JLabel("Video size");
			moviePanel.add(lblVideoSize, "6, 2");
		}
		{
			// JLabel lblGetCurrentOpengl = new
			// JLabel("Get current OpenGL-Frame size");
			// moviePanel.add(lblGetCurrentOpengl, "6, 4");
		}
		{
			// movieUseCurrentOpenglSize = new JCheckBox("", true);
			// moviePanel.add(movieUseCurrentOpenglSize, "8, 4");
			// movieUseCurrentOpenglSize.addItemListener(this);
		}
		{
			JLabel lblAspectRation = new JLabel("Aspect Ratio");
			moviePanel.add(lblAspectRation, "6, 6, right, default");
		}
		{
			movieAspectRatioSelection = new JComboBox<Object>(movieAspectRatioPresets);
			moviePanel.add(movieAspectRatioSelection, "8, 6, fill, default");
			movieAspectRatioSelection.addItemListener(this);
		}
		NumberFormat format = NumberFormat.getInstance();
		format.setGroupingUsed(false);
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(1);
		formatter.setMaximum(2048);
		{
			JLabel lblImageWidth = new JLabel("Image Width");
			moviePanel.add(lblImageWidth, "6, 8, right, default");
		}
		{
			txtMovieImageWidth = new JFormattedTextField(formatter);
			txtMovieImageWidth.setValue(1280);
			txtMovieImageWidth.addPropertyChangeListener("value", this);
			moviePanel.add(txtMovieImageWidth, "8, 8, fill, default");
			txtMovieImageWidth.setColumns(10);
			txtMovieImageWidth.setToolTipText("value between 1 to 2048");
		}
		{
			JLabel lblImageHeight = new JLabel("Image Height");
			moviePanel.add(lblImageHeight, "6, 10, right, default");
		}
		{
			txtMovieImageHeight = new JFormattedTextField(formatter);
			txtMovieImageHeight.setValue(720);
			txtMovieImageHeight.addPropertyChangeListener("value", this);
			moviePanel.add(txtMovieImageHeight, "8, 10, fill, default");
			txtMovieImageHeight.setColumns(10);
			txtMovieImageHeight.setToolTipText("value between 1 to 2048");

		}

	}

	private void initScreenshotGui() {
		screenshotPanel
				.setLayout(new FormLayout(new ColumnSpec[] {
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
			JLabel lblVideoSize = new JLabel("Screenshot size");
			screenshotPanel.add(lblVideoSize, "6, 2");
		}
		{
			// JLabel lblGetCurrentOpengl = new
			// JLabel("Get current OpenGL-Frame size");
			// screenshotPanel.add(lblGetCurrentOpengl, "6, 4");
		}
		{
			// screenshotUseCurrentOpenglSize = new JCheckBox("", true);
			// screenshotPanel.add(screenshotUseCurrentOpenglSize, "8, 4");
			// screenshotUseCurrentOpenglSize.addItemListener(this);
		}
		{
			JLabel lblAspectRation = new JLabel("Aspect Ratio");
			screenshotPanel.add(lblAspectRation, "6, 6, right, default");
		}
		{
			screenshotAspectRatioSelection = new JComboBox<Object>(
					screenshotAspectRatioPresets);
			screenshotPanel.add(screenshotAspectRatioSelection,
					"8, 6, fill, default");
			screenshotAspectRatioSelection.addItemListener(this);
		}
		NumberFormat format = NumberFormat.getInstance();
		format.setGroupingUsed(false);
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(1);
		formatter.setMaximum(2048);
		{
			JLabel lblImageWidth = new JLabel("Image Width");
			screenshotPanel.add(lblImageWidth, "6, 8, right, default");
		}
		{
			txtScreenshotImageWidth = new JFormattedTextField(formatter);
			txtScreenshotImageWidth.setValue(640);
			txtScreenshotImageWidth.addPropertyChangeListener("value", this);
			screenshotPanel.add(txtScreenshotImageWidth, "8, 8, fill, default");
			txtScreenshotImageWidth.setColumns(10);
			txtScreenshotImageWidth.setToolTipText("value between 1 to 2048");
		}
		{
			JLabel lblImageHeight = new JLabel("Image Height");
			screenshotPanel.add(lblImageHeight, "6, 10, right, default");
		}
		{
			txtScreenshotImageHeight = new JFormattedTextField(formatter);
			txtScreenshotImageHeight.setValue(480);
			txtScreenshotImageHeight.addPropertyChangeListener("value", this);
			screenshotPanel.add(txtScreenshotImageHeight,
					"8, 10, fill, default");
			txtScreenshotImageHeight.setColumns(10);
			txtScreenshotImageHeight.setToolTipText("value between 1 to 2048");
		}

	}

	private void loadMovieSettings() {
		Settings settings = Settings.getSingletonInstance();
		String val;
		/*
		 * try { val =
		 * settings.getProperty(SETTING_MOVIE_USE_CURRENT_OPENGL_SIZE); if (val
		 * != null && !(val.length() == 0)) {
		 * movieUseCurrentOpenglSize.setSelected(Boolean.parseBoolean(val)); } }
		 * catch (Throwable t) { Log.error(t); }
		 */

		try {
			val = settings.getProperty(SETTING_MOVIE_RATIO);
			if (val != null && !(val.length() == 0)) {
				int width, height;
				if (val.equals("Custom")) {
					width = height = 0;
				} else {
					String[] parts = val.split(" : ");
					width = Integer.parseInt(parts[0]);
					height = Integer.parseInt(parts[1]);
				}
				for (int i = 0; i < movieAspectRatioPresets.length; ++i) {
					if (movieAspectRatioPresets[i].width == width
							&& movieAspectRatioPresets[i].height == height) {
						movieAspectRatioSelection
								.setSelectedItem(movieAspectRatioPresets[i]);
						break;
					}
				}
			}
		} catch (Throwable t) {
			Log.error(t);
		}

		try {
			val = settings.getProperty(SETTING_MOVIE_IMG_HEIGHT);
			if (val != null && !(val.length() == 0)) {
				txtMovieImageHeight.setValue(Math.round(Float.parseFloat(val)));
			}
		} catch (Throwable t) {
			Log.error(t);
		}

		try {
			val = settings.getProperty(SETTING_MOVIE_IMG_WIDTH);
			if (val != null && !(val.length() == 0)) {
				txtMovieImageWidth.setValue(Math.round(Float.parseFloat(val)));
			}
		} catch (Throwable t) {
			Log.error(t);
		}

	}

	private void loadScreenshotSettings() {
		Settings settings = Settings.getSingletonInstance();
		String val;
		/*
		 * try { val =
		 * settings.getProperty(SETTING_SCREENSHOT_USE_CURRENT_OPENGL_SIZE); if
		 * (val != null && !(val.length() == 0)) {
		 * screenshotUseCurrentOpenglSize
		 * .setSelected(Boolean.parseBoolean(val)); } } catch (Throwable t) {
		 * Log.error(t); }
		 */

		try {
			val = settings.getProperty(SETTING_SCREENSHOT_RATIO);
			if (val != null && !(val.length() == 0)) {
				int width, height;
				if (val.equals("Custom")) {
					width = height = 0;
				} else {
					String[] parts = val.split(" : ");
					width = Integer.parseInt(parts[0]);
					height = Integer.parseInt(parts[1]);
				}
				for (int i = 0; i < screenshotAspectRatioPresets.length; ++i) {
					if (screenshotAspectRatioPresets[i].width == width
							&& screenshotAspectRatioPresets[i].height == height) {
						screenshotAspectRatioSelection
								.setSelectedItem(screenshotAspectRatioPresets[i]);
						break;
					}
				}
			}
		} catch (Throwable t) {
			Log.error(t);
		}

		try {
			val = settings.getProperty(SETTING_SCREENSHOT_IMG_HEIGHT);
			if (val != null && !(val.length() == 0)) {
				txtScreenshotImageHeight.setValue(Math.round(Float
						.parseFloat(val)));
			}
		} catch (Throwable t) {
			Log.error(t);
		}

		try {
			val = settings.getProperty(SETTING_SCREENSHOT_IMG_WIDTH);
			if (val != null && !(val.length() == 0)) {
				txtScreenshotImageWidth.setValue(Math.round(Float
						.parseFloat(val)));
			}
		} catch (Throwable t) {
			Log.error(t);
		}

	}

	private void saveSettings() {
		Settings settings = Settings.getSingletonInstance();
		// settings.setProperty(SETTING_MOVIE_USE_CURRENT_OPENGL_SIZE,
		// Boolean.toString(movieUseCurrentOpenglSize.isSelected()));
		settings.setProperty(SETTING_MOVIE_RATIO, movieAspectRatioSelection
				.getSelectedItem().toString());
		settings.setProperty(SETTING_MOVIE_IMG_WIDTH, txtMovieImageWidth
				.getValue().toString());
		settings.setProperty(SETTING_MOVIE_IMG_HEIGHT, txtMovieImageHeight
				.getValue().toString());
		// settings.setProperty(SETTING_SCREENSHOT_USE_CURRENT_OPENGL_SIZE,
		// Boolean.toString(screenshotUseCurrentOpenglSize.isSelected()));
		settings.setProperty(SETTING_SCREENSHOT_RATIO,
				screenshotAspectRatioSelection.getSelectedItem().toString());
		settings.setProperty(SETTING_SCREENSHOT_IMG_WIDTH,
				txtScreenshotImageWidth.getValue().toString());
		settings.setProperty(SETTING_SCREENSHOT_IMG_HEIGHT,
				txtScreenshotImageHeight.getValue().toString());
		settings.save();

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

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == this.btnSave) {
			this.saveSettings();
			this.dispose();
		} else if (ae.getSource() == this.btnCancel)
			this.dispose();

	}

	@Override
	public void itemStateChanged(ItemEvent ae) {
		
		if (ae.getSource() == this.movieAspectRatioSelection) {
			AspectRatio aspectRatio = (AspectRatio) movieAspectRatioSelection
					.getSelectedItem();
			if (aspectRatio.getWidth() != 0) {
				int width = Integer.parseInt(this.txtMovieImageWidth.getText());
				// txtMovieImageWidth.setValue(txtMovieImageWidth.getValue());
				this.hasChanged = true;
				txtMovieImageHeight.setValue(width * aspectRatio.getHeight()
						/ aspectRatio.getWidth());
			}
		}
		
		else if (ae.getSource() == this.screenshotAspectRatioSelection) {
			AspectRatio aspectRatio = (AspectRatio) screenshotAspectRatioSelection
					.getSelectedItem();
			if (aspectRatio.getWidth() != 0) {
				int width = Integer.parseInt(this.txtScreenshotImageWidth
						.getText());
				this.hasChanged = true;
				txtScreenshotImageHeight.setValue(width
						* aspectRatio.getHeight() / aspectRatio.getWidth());
			}
		}
		hasChanged = false;
	}

	@Override
	public void propertyChange(PropertyChangeEvent pe) {
		AspectRatio aspectRatio;
		if (tabbedPane.getSelectedComponent() == moviePanel && !hasChanged) {
			aspectRatio = (AspectRatio) movieAspectRatioSelection
					.getSelectedItem();
			if (aspectRatio.getHeight() != 0) {
				if (pe.getSource() == this.txtMovieImageWidth) {
					int width = Integer.parseInt(this.txtMovieImageWidth
							.getText());
					this.hasChanged = true;
					this.txtMovieImageHeight.setValue(width
							* aspectRatio.getHeight() / aspectRatio.getWidth());
				} else if (pe.getSource() == this.txtMovieImageHeight) {
					int heigth = Integer.parseInt(this.txtMovieImageHeight
							.getText());
					this.hasChanged = true;
					this.txtMovieImageWidth.setValue(heigth
							* aspectRatio.getWidth() / aspectRatio.getHeight());
				}
			}
		} else if (tabbedPane.getSelectedComponent() == screenshotPanel
				&& !hasChanged) {
			aspectRatio = (AspectRatio) screenshotAspectRatioSelection
					.getSelectedItem();
			if (aspectRatio.getHeight() != 0) {
				if (pe.getSource() == this.txtScreenshotImageWidth) {
					int width = Integer.parseInt(this.txtScreenshotImageWidth
							.getText());
					this.hasChanged = true;
					this.txtScreenshotImageHeight.setValue(width
							* aspectRatio.getHeight() / aspectRatio.getWidth());
				} else if (pe.getSource() == this.txtScreenshotImageHeight) {
					int heigth = Integer.parseInt(this.txtScreenshotImageHeight
							.getText());
					this.hasChanged = true;
					this.txtScreenshotImageWidth.setValue(heigth
							* aspectRatio.getWidth() / aspectRatio.getHeight());
				}

			}
		} else {
			this.hasChanged = false;
		}
	}

}