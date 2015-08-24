package org.helioviewer.jhv.gui.leftPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.CacheableImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class MoviePanel extends JPanel implements TimeLineListener,
		LayerListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3837685812219375888L;

	// different animation speeds
	private enum SPEED_UNIT {
		FRAMES_PER_SECOND("Frames/sec", 1), MINUTES_PER_SECOND(
				"Solar minutes/sec", 60), HOURS_PER_SECOND("Solar hours/sec",
				3600), DAYS_PER_SECOND("Solar days/sec", 864000);
		private String text;
		private int factor;

		private SPEED_UNIT(String text, int factor) {
			this.text = text;
			this.factor = factor;
		}

		@Override
		public String toString() {
			return text;
		}

	}

	public enum ANIMATION_MODE {
		LOOP("loop"), STOP("Stop"), SWING("swing");
		private String text;

		private ANIMATION_MODE(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}

	// Icons
	private static final Icon ICON_PLAY = IconBank.getIcon(JHVIcon.PLAY_NEW,
			16, 16);
	private static final Icon ICON_PAUSE = IconBank.getIcon(JHVIcon.PAUSE_NEW,
			16, 16);
	private static final Icon ICON_OPEN = IconBank.getIcon(JHVIcon.DOWN_NEW,
			16, 16);
	private static final Icon ICON_CLOSE = IconBank.getIcon(JHVIcon.UP_NEW, 16,
			16);
	private static final Icon ICON_BACKWARD = IconBank.getIcon(
			JHVIcon.BACKWARD_NEW, 16, 16);
	private static final Icon ICON_FORWARD = IconBank.getIcon(
			JHVIcon.FORWARD_NEW, 16, 16);

	private boolean showMore = false;
	private JPanel optionPane;

	private TimeLine timeLine = TimeLine.SINGLETON;
	private JLabel lblFrames;

	private JSlider slider;
	private JButton btnPrevious, btnPlayPause, btnForward;

	private static int size;
	private static int buttonSize;
	private static int width;
	private static int optionPaneSize;
	
	static{
		JSlider slider = new JSlider();		
		JButton button = new JButton(ICON_PLAY);
		JLabel label = new JLabel("test");
		size = slider.getPreferredSize().height + label.getPreferredSize().height + 10 + 20;
		System.out.println("tt");
		JComboBox<String> comboBox = new JComboBox<String>();
		JSpinner spinner = new JSpinner();
	
		buttonSize = button.getPreferredSize().height;
		int tmpSize = Math.max(Math.max(label.getPreferredSize().height, spinner.getPreferredSize().height), comboBox.getPreferredSize().height);
		optionPaneSize = size + tmpSize * 2;
	}

	public MoviePanel() {
		setBorder(new EmptyBorder(0, 2, 0, 10));
		TimeLine.SINGLETON.addListener(this);
		Layers.addNewLayerListener(this);
		this.setLayout(new BorderLayout());
		this.add(initGUI(), BorderLayout.CENTER);
		optionPane = initOptionPane();
		this.add(optionPane, BorderLayout.SOUTH);
		width = slider.getPreferredSize().width;
		this.setMinimumSize(new Dimension(width, size));
		this.setPreferredSize(new Dimension(width, size));
		System.out.println(getSize().height);
	}

	private JPanel initOptionPane() {
		JPanel contentPanel = new JPanel();
		contentPanel
				.setLayout(new FormLayout(new ColumnSpec[] {
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("max(5px;default)"),
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("max(5px;default)"),
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"), }, new RowSpec[] {
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, }));

		JLabel lblSpeed = new JLabel("Speed:");
		contentPanel.add(lblSpeed, "2, 2");

		SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(60, 1,
				300, 1);
		final JSpinner spinner = new JSpinner(spinnerNumberModel);
		contentPanel.add(spinner, "4, 2");

		final JComboBox<SPEED_UNIT> speedUnitComboBox = new JComboBox<SPEED_UNIT>(
				SPEED_UNIT.values());

		spinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				timeLine.setSpeedFactor((int) spinner.getValue()
						* ((SPEED_UNIT) speedUnitComboBox.getSelectedItem()).factor);
			}
		});
		speedUnitComboBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				timeLine.setSpeedFactor((int) spinner.getValue()
						* ((SPEED_UNIT) speedUnitComboBox.getSelectedItem()).factor);
			}
		});
		contentPanel.add(speedUnitComboBox, "6, 2, fill, default");

		JLabel lblAnimationMode = new JLabel("Animation Mode:");
		contentPanel.add(lblAnimationMode, "2, 4, 3, 1");

		final JComboBox<ANIMATION_MODE> animationModeComboBox = new JComboBox<ANIMATION_MODE>(
				ANIMATION_MODE.values());
		animationModeComboBox.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				timeLine.setAnimationMode((ANIMATION_MODE) animationModeComboBox
						.getSelectedItem());
			}
		});
		contentPanel.add(animationModeComboBox, "6, 4, fill, default");
		contentPanel.setVisible(false);
		return contentPanel;
	}

	private JPanel initGUI() {
		JPanel contentPanel = new JPanel();

		contentPanel.setLayout(new FormLayout(
				new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("pref:grow"),
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC,
						FormFactory.RELATED_GAP_ROWSPEC,
						FormFactory.DEFAULT_ROWSPEC, }));

		slider = new TimeSlider();
		slider.setValue(0);
		slider.setPreferredSize(new Dimension(0, 20));
		slider.setMinimum(0);
		slider.setMaximum(49);
		slider.setSnapToTicks(true);
		slider.setEnabled(false);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				lblFrames.setText(slider.getValue() + "/" + slider.getMaximum());
				timeLine.setCurrentFrame(slider.getValue());
			}
		});
		contentPanel.add(slider, "2, 2, 11, 1");

		btnPrevious = new JButton(ICON_BACKWARD);
		btnPrevious.setEnabled(false);
		btnPrevious.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				timeLine.previousFrame();
			}
		});
		btnPrevious.setPreferredSize(new Dimension(btnPrevious.getPreferredSize().width, buttonSize));
		contentPanel.add(btnPrevious, "2, 4");

		btnPlayPause = new JButton(ICON_PLAY);
		btnPlayPause.setEnabled(false);
		btnPlayPause.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (timeLine.isPlaying())
					btnPlayPause.setIcon(ICON_PLAY);
				else
					btnPlayPause.setIcon(ICON_PAUSE);
				timeLine.setPlaying(!timeLine.isPlaying());
			}
		});
		btnPlayPause.setPreferredSize(new Dimension(btnPlayPause.getPreferredSize().width, buttonSize));
		contentPanel.add(btnPlayPause, "4, 4");

		btnForward = new JButton(ICON_FORWARD);
		btnForward.setEnabled(false);
		btnForward.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				timeLine.nextFrame();
			}
		});
		btnForward.setPreferredSize(new Dimension(btnForward.getPreferredSize().width, buttonSize));
		
		contentPanel.add(btnForward, "6, 4");
		final JButton btnOptionPane = new JButton("More Options", ICON_OPEN);
		btnOptionPane.setToolTipText("More Options to Control Playback");
		btnOptionPane.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (showMore) {
					btnOptionPane.setIcon(ICON_OPEN);
					setPreferredSize(new Dimension(300, size));
				} else {
					btnOptionPane.setIcon(ICON_CLOSE);
					setPreferredSize(new Dimension(300, optionPaneSize));
				}
				showMore = !showMore;
				optionPane.setVisible(showMore);
			}
		});
		btnOptionPane.setPreferredSize(new Dimension(btnOptionPane.getPreferredSize().width, buttonSize));
		
		contentPanel.add(btnOptionPane, "8, 4");
		lblFrames = new JLabel();
		contentPanel.add(lblFrames, "12, 4");

		contentPanel.setPreferredSize(new Dimension(300, size));
		setEnableButtons(false);
		return contentPanel;
	}

	public void setPlaying(boolean playing) {
		if (!playing)
			btnPlayPause.setIcon(ICON_PLAY);
		else
			btnPlayPause.setIcon(ICON_PAUSE);
		TimeLine.SINGLETON.setPlaying(playing);
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		slider.setValue(timeLine.getCurrentFrame() < 0 ? 0 : timeLine
				.getCurrentFrame());
	}

	public void repaintSlider() {
		slider.repaint();
	}

	private class TimeSlider extends JSlider {

		/**
		 * 
		 */
		private static final long serialVersionUID = 2053723659623341117L;

		public TimeSlider() {
			setUI(new TimeSliderUI(this));
		}

	}

	private class TimeSliderUI extends BasicSliderUI {
		private final Color COLOR_NOT_CACHED = Color.LIGHT_GRAY;
		private final Color COLOR_PARTIALLY_CACHED = new Color(0x8080FF);
		private final Color COLOR_COMPLETELY_CACHED = new Color(0x4040FF);
		private final Color COLOR_GRAY = new Color(0xEEEEEE);

		private static final double SCALE_FACTOR = 1000000;
		private static final double INVERSE_SCALE_FACTOR = 1 / SCALE_FACTOR;

		public TimeSliderUI(JSlider slider) {
			super(slider);
		}

		@Override
		public void paintThumb(Graphics g) {

			Rectangle knobBounds = thumbRect;
			int w = knobBounds.width;
			int h = knobBounds.height - 2;
			g.setColor(COLOR_GRAY);
			g.translate(knobBounds.x, knobBounds.y);

			int cw = w / 2;
			g.fillRect(1, 1, w - 3, h - 1 - cw);
			Polygon p = new Polygon();
			p.addPoint(1, h - cw);
			p.addPoint(cw - 1, h - 1);
			p.addPoint(w - 2, h - 1 - cw);
			g.fillPolygon(p);

			g.setColor(getHighlightColor());
			g.drawLine(0, 0, w - 2, 0);
			g.drawLine(0, 1, 0, h - 1 - cw);
			g.drawLine(0, h - cw, cw - 1, h - 1);

			g.setColor(Color.black);
			g.drawLine(w - 1, 0, w - 1, h - 2 - cw);
			g.drawLine(w - 1, h - 1 - cw, w - 1 - cw, h - 1);

			g.setColor(getShadowColor());
			g.drawLine(w - 2, 1, w - 2, h - 2 - cw);
			g.drawLine(w - 2, h - 1 - cw, w - 1 - cw, h - 2);
			g.translate(-knobBounds.x, -knobBounds.y);

		}

		@Override
		public void paintTrack(Graphics g) {
			Rectangle trackBounds = trackRect;

			int cy = (trackBounds.height / 2) - 2;
			int cw = trackBounds.width;

			g.translate(trackBounds.x, 0);
			Graphics2D g2 = (Graphics2D) g;
			g2.scale(INVERSE_SCALE_FACTOR, 1);
			int offset = trackRect.height - 3;
			int height = 2;

			if (Layers.getActiveImageLayer() != null) {
				AbstractImageLayer layer = (AbstractImageLayer) Layers
						.getActiveImageLayer();
				ConcurrentSkipListSet<LocalDateTime> treeSet = layer.getLocalDateTime().clone();
				int total = treeSet.size();
				Color[] colors = new Color[treeSet.size()];
				int counter = 0;
				int totalSize = 0;
				for (LocalDateTime localDateTime : treeSet) {
					CacheableImageData cacheableImageData = layer
							.getCacheStatus(localDateTime);
					switch (cacheableImageData.getCacheStatus()) {
					case FILE:
						colors[counter++] = COLOR_COMPLETELY_CACHED;
						break;
					case KDU:
						colors[counter++] = COLOR_PARTIALLY_CACHED;
						break;
					default:
						colors[counter++] = COLOR_NOT_CACHED;
						break;
					}
				}

				int currentSize = 0;
				int trackWidth = (int) (trackRect.getWidth());
				double partPerDate = 1 / (double) treeSet.size();
				int delta = (int) (partPerDate * trackWidth * SCALE_FACTOR);
				for (int i = 0; i < colors.length; i++) {
					g.setColor(colors[i]);
					g2.fillRect(currentSize, offset, delta, height);
					currentSize += delta;
				}
			}
			else {
				g.setColor(COLOR_NOT_CACHED);
				g2.fillRect(0, offset,
						(int) (trackRect.getWidth() * SCALE_FACTOR), height);
			}
			Dimension sliderSize = super.slider.getSize();

			g2.scale(SCALE_FACTOR, 1);
			g.translate(-trackBounds.x, 0);

			int partialCachedOffset = sliderSize.width / 2;

			int completeCachedOffset = sliderSize.width / 4;
			/*
			 * g.setColor(COLOR_NOT_CACHED); g.fillRect(trackRect.x +
			 * partialCachedOffset, offset, trackRect.width -
			 * partialCachedOffset, height);
			 * 
			 * g.setColor(COLOR_PARTIALLY_CACHED); g.fillRect(trackRect.x +
			 * completeCachedOffset, offset, partialCachedOffset -
			 * completeCachedOffset, height);
			 * 
			 * g.setColor(COLOR_COMPLETELY_CACHED); g.fillRect(trackRect.x,
			 * offset, completeCachedOffset, height);
			 */
		}
	}

	@Override
	public void newlayerAdded() {
		boolean enable = false;
		for (AbstractLayer layer : Layers.getLayers()){
			enable |= layer.isImageLayer();
		}
		setEnableButtons(enable);
		if (Layers.getActiveImageLayer() != null){
			lblFrames.setText(slider.getValue() + "/" + slider.getMaximum());
		}
	}

	@Override
	public void newlayerRemoved(int idx) {
		boolean enable = false;
		for (AbstractLayer layer : Layers.getLayers()){
			enable |= layer.isImageLayer();
		}
		setEnableButtons(enable);
	}

	@Override
	public void activeLayerChanged(AbstractLayer layer) {
		if (layer != null && layer.isImageLayer()){
			slider.setMaximum(((ImageLayer) layer).getLocalDateTime().size() > 0 ? ((ImageLayer) layer)
					.getLocalDateTime().size() - 1 : 0);
			lblFrames.setText(slider.getValue() + "/" + slider.getMaximum());
		}
	}

	public void setEnableButtons(boolean enable) {
		slider.setEnabled(enable);
		btnPrevious.setEnabled(enable);
		btnPlayPause.setEnabled(enable);
		btnForward.setEnabled(enable);
		lblFrames.setText("");
	}

	@Override
	public void dateTimesChanged(int framecount) {
		this.slider.setMaximum(framecount > 0 ? framecount - 1 : 0);
		lblFrames.setText(slider.getValue() + "/" + slider.getMaximum());
		this.repaint();
	}

	/**
	 * Action to play or pause the active layer, if it is an image series.
	 * 
	 * Static movie actions are supposed be integrated into {@link MenuBar},
	 * also to provide shortcuts. They always refer to the active layer.
	 */
	public static class StaticPlayPauseAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7709407709240852446L;

		/**
		 * Default constructor.
		 */
		public StaticPlayPauseAction() {
			super("Play movie", ICON_PLAY);
			putValue(MNEMONIC_KEY, KeyEvent.VK_A);
			putValue(ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_MASK));
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent e) {
			MainFrame.MOVIE_PANEL.setPlaying(!TimeLine.SINGLETON.isPlaying());
		}
	}

	/**
	 * Action to step to the previous frame for the active layer, if it is an
	 * image series.
	 * 
	 * Static movie actions are supposed be integrated into {@link MenuBar},
	 * also to provide shortcuts. They always refer to the active layer.
	 */
	public static class StaticPreviousFrameAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6342665405812226382L;

		/**
		 * Default constructor.
		 */
		public StaticPreviousFrameAction() {
			super("Step to Previous Frame", ICON_BACKWARD);
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
			putValue(ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK));
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent e) {
			TimeLine.SINGLETON.previousFrame();
		}
	}

	/**
	 * Action to step to the next frame for the active layer, if it is an image
	 * series.
	 * 
	 * Static movie actions are supposed be integrated into {@link MenuBar},
	 * also to provide shortcuts. They always refer to the active layer.
	 */
	public static class StaticNextFrameAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = -8262700086251171378L;

		/**
		 * Default constructor.
		 */
		public StaticNextFrameAction() {
			super("Step to Next Frame", ICON_FORWARD);
			putValue(MNEMONIC_KEY, KeyEvent.VK_N);
			putValue(ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_MASK));
		}

		/**
		 * {@inheritDoc}
		 */
		public void actionPerformed(ActionEvent e) {
			TimeLine.SINGLETON.nextFrame();
		}
	}
}
