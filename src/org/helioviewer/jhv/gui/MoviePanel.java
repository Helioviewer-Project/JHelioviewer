package org.helioviewer.jhv.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.MenuBar;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.TimeLineListener;

public class MoviePanel extends JPanel implements TimeLineListener, LayerListener
{
	// different animation speeds
	private enum PlaybackSpeedUnit
	{
		FRAMES_PER_SECOND("Frames/sec", 1),
		
		//TODO: add support for other playback speed units again
		/*
		MINUTES_PER_SECOND("Solar minutes/sec", 60),
		HOURS_PER_SECOND("Solar hours/sec", 3600),
		DAYS_PER_SECOND("Solar days/sec", 864000)*/;
		
		public final String text;
		public final int factor;

		PlaybackSpeedUnit(String _text, int _factor)
		{
			text = _text;
			factor = _factor;
		}

		@Override
		public String toString()
		{
			return text;
		}
	}

	public enum AnimationMode
	{
		LOOP("Loop"), STOP("Stop"), SWING("Swing");
		private String text;
		
		AnimationMode(String _text)
		{
			text = _text;
		}
		
		@Override
		public String toString()
		{
			return text;
		}
	}

	// Icons
	private static final Icon ICON_PLAY = IconBank.getIcon(JHVIcon.PLAY_NEW, 16, 16);
	private static final Icon ICON_PAUSE = IconBank.getIcon(JHVIcon.PAUSE_NEW, 16, 16);
	private static final Icon ICON_OPEN = IconBank.getIcon(JHVIcon.DOWN_NEW, 16, 16);
	private static final Icon ICON_CLOSE = IconBank.getIcon(JHVIcon.UP_NEW, 16, 16);
	private static final Icon ICON_BACKWARD = IconBank.getIcon(JHVIcon.BACKWARD_NEW, 16, 16);
	private static final Icon ICON_FORWARD = IconBank.getIcon(JHVIcon.FORWARD_NEW, 16, 16);

	private boolean showMore = false;
	
	private int ignoreSliderChanges = 0;

	private TimeLine timeLine = TimeLine.SINGLETON;
	private JLabel lblFrames;
	private JComboBox<PlaybackSpeedUnit> speedUnitComboBox;
	private JComboBox<AnimationMode> animationModeComboBox;
	private JSpinner spinner;

	private JPanel optionPane;
	private JButton btnPrevious, btnPlayPause, btnForward;
	private JSlider slider;

	public MoviePanel()
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		TimeLine.SINGLETON.addListener(this);
		Layers.addLayerListener(this);
		setLayout(new BorderLayout());
		
		SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(20, 1, 300, 1);
		
		JPanel contentPanel = new JPanel();

		
		
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_contentPanel.rowHeights = new int[]{0, 0, 0};
		gbl_contentPanel.columnWeights = new double[]{0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_contentPanel.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		contentPanel.setLayout(gbl_contentPanel);
		
		btnPlayPause = new JButton(ICON_PLAY);
		btnPlayPause.setEnabled(false);
		btnPlayPause.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				TimeLine.SINGLETON.setPlaying(!timeLine.isPlaying());
			}
		});
		
		btnPrevious = new JButton(ICON_BACKWARD);
		btnPrevious.setEnabled(false);
		btnPrevious.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				timeLine.previousFrame();
			}
		});
		
		slider = new JSlider();
		slider.setUI(new TimeSliderUI(slider));
		slider.setValue(0);
		slider.setMinimum(0);
		slider.setMaximum(49);
		slider.setSnapToTicks(true);
		slider.setEnabled(false);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(@Nullable ChangeEvent e)
			{
				lblFrames.setText(slider.getValue() + "/" + slider.getMaximum());
				
				if(ignoreSliderChanges==0)
					timeLine.setCurrentFrame(slider.getValue());
			}
		});

		GridBagConstraints gbc_slider_2 = new GridBagConstraints();
		gbc_slider_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_2.gridwidth = 4;
		gbc_slider_2.insets = new Insets(0, 0, 5, 5);
		gbc_slider_2.gridx = 0;
		gbc_slider_2.gridy = 0;
		contentPanel.add(slider, gbc_slider_2);
		lblFrames = new JLabel("0/0");
		lblFrames.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_lblFrames = new GridBagConstraints();
		gbc_lblFrames.insets = new Insets(0, 0, 5, 0);
		gbc_lblFrames.anchor = GridBagConstraints.EAST;
		gbc_lblFrames.gridx = 4;
		gbc_lblFrames.gridy = 0;
		contentPanel.add(lblFrames, gbc_lblFrames);
		
		GridBagConstraints gbc_btnPrevious = new GridBagConstraints();
		gbc_btnPrevious.insets = new Insets(0, 0, 0, 5);
		gbc_btnPrevious.gridx = 0;
		gbc_btnPrevious.gridy = 1;
		contentPanel.add(btnPrevious, gbc_btnPrevious);
		GridBagConstraints gbc_btnPlayPause = new GridBagConstraints();
		gbc_btnPlayPause.insets = new Insets(0, 0, 0, 5);
		gbc_btnPlayPause.gridx = 1;
		gbc_btnPlayPause.gridy = 1;
		contentPanel.add(btnPlayPause, gbc_btnPlayPause);
		final JButton btnMoreOptions = new JButton("More", ICON_OPEN);
		btnMoreOptions.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if (showMore)
					btnMoreOptions.setIcon(ICON_OPEN);
				else
					btnMoreOptions.setIcon(ICON_CLOSE);
		
				showMore = !showMore;
				optionPane.setVisible(showMore);
			}
		});

		btnForward = new JButton(ICON_FORWARD);
		btnForward.setEnabled(false);
		btnForward.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				timeLine.nextFrame();
			}
		});
		//btnForward.setPreferredSize(new Dimension(btnForward.getPreferredSize().width, buttonSize));
		
		GridBagConstraints gbc_btnForward = new GridBagConstraints();
		gbc_btnForward.insets = new Insets(0, 0, 0, 5);
		gbc_btnForward.gridx = 2;
		gbc_btnForward.gridy = 1;
		contentPanel.add(btnForward, gbc_btnForward);
		
		GridBagConstraints gbc_btnMoreOptions = new GridBagConstraints();
		gbc_btnMoreOptions.gridwidth = 2;
		gbc_btnMoreOptions.anchor = GridBagConstraints.NORTHEAST;
		gbc_btnMoreOptions.gridx = 3;
		gbc_btnMoreOptions.gridy = 1;
		contentPanel.add(btnMoreOptions, gbc_btnMoreOptions);

		setButtonsEnabled(false);
		add(contentPanel, BorderLayout.CENTER);

		optionPane = new JPanel();
		optionPane.setBorder(new EmptyBorder(15, 0, 0, 0));
		GridBagLayout gbl_optionPane = new GridBagLayout();
		gbl_optionPane.columnWidths = new int[]{0, 0, 0, 0};
		gbl_optionPane.rowHeights = new int[]{26, 26, 0};
		gbl_optionPane.columnWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_optionPane.rowWeights = new double[]{0.0, 0.0, Double.MIN_VALUE};
		optionPane.setLayout(gbl_optionPane);
		
				JLabel lblSpeed = new JLabel("Speed:");
				GridBagConstraints gbc_lblSpeed = new GridBagConstraints();
				gbc_lblSpeed.anchor = GridBagConstraints.WEST;
				gbc_lblSpeed.insets = new Insets(0, 0, 5, 5);
				gbc_lblSpeed.gridx = 0;
				gbc_lblSpeed.gridy = 0;
				optionPane.add(lblSpeed, gbc_lblSpeed);
		spinner = new JSpinner(spinnerNumberModel);
		
				GridBagConstraints gbc_spinner = new GridBagConstraints();
				gbc_spinner.anchor = GridBagConstraints.NORTHWEST;
				gbc_spinner.insets = new Insets(0, 0, 5, 5);
				gbc_spinner.gridx = 1;
				gbc_spinner.gridy = 0;
				optionPane.add(spinner, gbc_spinner);
				
						spinner.addChangeListener(new ChangeListener()
						{
							@Override
							public void stateChanged(@Nullable ChangeEvent e)
							{
								timeLine.setFPS((int) spinner.getValue() * ((PlaybackSpeedUnit) speedUnitComboBox.getSelectedItem()).factor);
							}
						});
		
		speedUnitComboBox = new JComboBox<>();
		speedUnitComboBox.setModel(new DefaultComboBoxModel<>(PlaybackSpeedUnit.values()));
		
		speedUnitComboBox.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(@Nullable ItemEvent e)
			{
				timeLine.setFPS((int) spinner.getValue() * ((PlaybackSpeedUnit) speedUnitComboBox.getSelectedItem()).factor);
			}
		});
		GridBagConstraints gbc_speedUnitComboBox = new GridBagConstraints();
		gbc_speedUnitComboBox.anchor = GridBagConstraints.NORTH;
		gbc_speedUnitComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_speedUnitComboBox.insets = new Insets(0, 0, 5, 0);
		gbc_speedUnitComboBox.gridx = 2;
		gbc_speedUnitComboBox.gridy = 0;
		optionPane.add(speedUnitComboBox, gbc_speedUnitComboBox);
		
				JLabel lblAnimationMode = new JLabel("Animation mode:");
				GridBagConstraints gbc_lblAnimationMode = new GridBagConstraints();
				gbc_lblAnimationMode.fill = GridBagConstraints.HORIZONTAL;
				gbc_lblAnimationMode.insets = new Insets(0, 0, 0, 5);
				gbc_lblAnimationMode.gridwidth = 2;
				gbc_lblAnimationMode.gridx = 0;
				gbc_lblAnimationMode.gridy = 1;
				optionPane.add(lblAnimationMode, gbc_lblAnimationMode);
		optionPane.setVisible(false);
		add(optionPane, BorderLayout.SOUTH);
		
		animationModeComboBox = new JComboBox<>();
		animationModeComboBox.setModel(new DefaultComboBoxModel<>(AnimationMode.values()));
		
				animationModeComboBox.addItemListener(new ItemListener()
				{
					@Override
					public void itemStateChanged(@Nullable ItemEvent e)
					{
						timeLine.setAnimationMode((AnimationMode) animationModeComboBox.getSelectedItem());
					}
				});
				GridBagConstraints gbc_animationModeComboBox = new GridBagConstraints();
				gbc_animationModeComboBox.anchor = GridBagConstraints.NORTH;
				gbc_animationModeComboBox.fill = GridBagConstraints.HORIZONTAL;
				gbc_animationModeComboBox.gridx = 2;
				gbc_animationModeComboBox.gridy = 1;
				optionPane.add(animationModeComboBox, gbc_animationModeComboBox);
	}

	@Override
	public void timeStampChanged(long current, long last)
	{
		try
		{
			ignoreSliderChanges++;
			slider.setValue(timeLine.getCurrentFrameIndex());
		}
		finally
		{
			ignoreSliderChanges--;
		}
	}
	
	@Override	
	public void timeRangeChanged(long _startMS, long _endMS)
	{
		try
		{
			ignoreSliderChanges++;
			
			setButtonsEnabled(TimeLine.SINGLETON.isThereAnythingToPlay());
			
			int framecount = TimeLine.SINGLETON.getFrameCount();
			slider.setMaximum(framecount > 0 ? framecount - 1 : 0);
			slider.setValue(timeLine.getCurrentFrameIndex());
			lblFrames.setMinimumSize(new Dimension(lblFrames.getFontMetrics(lblFrames.getFont()).stringWidth(slider.getMaximum() + "/" + slider.getMaximum()),lblFrames.getMinimumSize().height));
			lblFrames.setPreferredSize(new Dimension(lblFrames.getFontMetrics(lblFrames.getFont()).stringWidth(slider.getMaximum() + "/" + slider.getMaximum()),lblFrames.getPreferredSize().height));
		}
		finally
		{
			ignoreSliderChanges--;
		}
	}

	private class TimeSliderUI extends BasicSliderUI
	{
		private final Color COLOR_NA = Color.RED;
		private final Color COLOR_NOT_CACHED = Color.LIGHT_GRAY;
		private final Color COLOR_PARTIALLY_CACHED = new Color(0x8080FF);
		private final Color COLOR_COMPLETELY_CACHED = new Color(0x4040FF);
		private final Color COLOR_GRAY = new Color(0xEEEEEE);

		public TimeSliderUI(JSlider slider)
		{
			super(slider);
		}

		@Override
		public void paintThumb(@Nullable Graphics g)
		{
			if(g==null)
				return;
			
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

			g.setColor(Color.BLACK);
			g.drawLine(w - 1, 0, w - 1, h - 2 - cw);
			g.drawLine(w - 1, h - 1 - cw, w - 1 - cw, h - 1);

			g.setColor(getShadowColor());
			g.drawLine(w - 2, 1, w - 2, h - 2 - cw);
			g.drawLine(w - 2, h - 1 - cw, w - 1 - cw, h - 2);
			g.translate(-knobBounds.x, -knobBounds.y);
		}
		
		@Override
		public void paintTrack(@Nullable Graphics g)
		{
			if(g==null)
				return;
			
			final int WIDTH = (int)trackRect.getWidth();
			final int HEIGHT = 7; //(int)trackRect.getHeight();

			g.translate(trackRect.x, trackRect.height - HEIGHT - 1);
			
			Graphics2D g2 = (Graphics2D) g;
			
			//TODO: redrawn every frame --> should only be recalculated when needed
			
			ImageLayer layer = Layers.getActiveImageLayer();
			if (layer != null)
			{
				//TODO: speed this code up
				int max=Math.min(TimeLine.SINGLETON.getFrameCount()-1, (int)trackRect.getWidth());
				//int max=(int)trackRect.getWidth();
				for(int i=0;i<=max;i++)
				{
					//snap position to nearest frame
					int frame = (int)(TimeLine.SINGLETON.getFrameCount()*i/(double)(max+1));
					long timeMS=TimeLine.SINGLETON.getFirstTimeMS()+frame*TimeLine.SINGLETON.getCadenceMS();
					
					if(!layer.isDataAvailableOnServer(timeMS))
						g.setColor(COLOR_NA);
					else
					{
						Match currentMatch = layer.findBestFrame(timeMS);
						if(currentMatch==null || currentMatch.timeDifferenceMS>layer.getCadenceMS()/2)
							g.setColor(COLOR_NOT_CACHED);
						else
							g.setColor(currentMatch.movie.isFullQuality() ? COLOR_COMPLETELY_CACHED : COLOR_PARTIALLY_CACHED);
					}
					
					int xa=(int)Math.round((i-0.5)/(double)max*WIDTH);
					int xb=(int)Math.round((i+0.5)/(double)max*WIDTH);
					if(xa<0)
						xa=0;
					if(xb>WIDTH)
						xb=WIDTH;
					g2.fillRect(xa, 0, xb-xa, HEIGHT);
				}
				
				
				
				/*
				max=(int)trackRect.getWidth();
				for(int i=0;i<=max;i++)
				{
					LocalDateTime localDateTime=TimeLine.SINGLETON.getFirstDateTime().plusSeconds((int)(duration/(double)max*i));
					if(!layer.isDataAvailableOnServer(localDateTime))
						g.setColor(COLOR_NA);
					else
					{
						Match currentMatch = layer.findBestFrame(localDateTime);
						if(currentMatch==null || currentMatch.timeDifferenceSeconds>layer.getCadence()/2)
							g.setColor(COLOR_NOT_CACHED);
						else
							switch (currentMatch.movie.quality)
							{
								case FULL:
									g.setColor(COLOR_COMPLETELY_CACHED);
									break;
								case PREVIEW:
									g.setColor(COLOR_PARTIALLY_CACHED);
									break;
								default:
									throw new RuntimeException();
							}
					}
					
					//g.setColor((i&1)==1 ? Color.RED : Color.GREEN);
					
					int xa=(int)Math.round((i-0.5)/(double)max*WIDTH);
					int xb=(int)Math.round((i+0.5)/(double)max*WIDTH);
					if(xa<0)
						xa=0;
					if(xb>WIDTH)
						xb=WIDTH;
					g2.fillRect(xa, 0, xb-xa, HEIGHT/4);


					LocalDateTime localDateTime=TimeLine.SINGLETON.getFirstDateTime().plusSeconds((int)(duration/(double)max*i));
					if(!layer.isDataAvailableOnServer(localDateTime))
						g.setColor(COLOR_NA);
					else
					{
						Match currentMatch = layer.findBestFrame(localDateTime);
						if(currentMatch==null || currentMatch.timeDifferenceSeconds>duration/max)
							g.setColor(COLOR_NOT_CACHED);
						else
							switch (currentMatch.movie.quality)
							{
								case FULL:
									g.setColor(COLOR_COMPLETELY_CACHED);
									break;
								case PREVIEW:
									g.setColor(COLOR_PARTIALLY_CACHED);
									break;
								default:
									throw new RuntimeException();
							}
					}
					
					g2.fillRect(xa, HEIGHT/4, xb-xa, HEIGHT/4);
				}

				max=TimeLine.SINGLETON.getFrameCount()-1;
				for(int i=0;i<=max;i++)
				{
					int xa=(int)Math.round((i-0.5)/(double)max*WIDTH);
					int xb=(int)Math.round((i+0.5)/(double)max*WIDTH);
					if(xa<0)
						xa=0;
					if(xb>WIDTH)
						xb=WIDTH;
					
					g.setColor((i&1)==1 ? Color.RED : Color.GREEN);
					g2.fillRect(xa, (int)(HEIGHT*0.5), xb-xa, HEIGHT/4);
				}*/
			}
			else
			{
				g.setColor(COLOR_NOT_CACHED);
				g2.fillRect(0, 0, WIDTH, HEIGHT);
			}

			g.translate(-trackRect.x, -trackRect.height + HEIGHT + 1);
		}
	}

	@Override
	public void layerAdded()
	{
		boolean enable = false;
		for (Layer layer : Layers.getLayers())
			enable |= layer instanceof ImageLayer;
		
		setButtonsEnabled(enable);
	}

	@Override
	public void layersRemoved()
	{
		boolean enable = false;
		for (Layer layer : Layers.getLayers())
			enable |= layer instanceof ImageLayer;
		
		setButtonsEnabled(enable);
	}

	@Override
	public void activeLayerChanged(@Nullable Layer layer)
	{
		repaint();
	}

	public void setButtonsEnabled(boolean _enable)
	{
		slider.setEnabled(_enable);
		btnPrevious.setEnabled(_enable);
		btnPlayPause.setEnabled(_enable);
		btnForward.setEnabled(_enable);
	}
	
	/**
	 * Action to play or pause the active layer, if it is an image series.
	 * 
	 * Static movie actions are supposed be integrated into {@link MenuBar},
	 * also to provide shortcuts. They always refer to the active layer.
	 */
	public static class StaticPlayPauseAction extends AbstractAction
	{
		public StaticPlayPauseAction()
		{
			super("Play movie", ICON_PLAY);
			putValue(MNEMONIC_KEY, KeyEvent.VK_A);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_MASK));
		}

		public void actionPerformed(@Nullable ActionEvent e)
		{
			TimeLine.SINGLETON.setPlaying(!TimeLine.SINGLETON.isPlaying());
		}
	}

	/**
	 * Action to step to the previous frame for the active layer, if it is an
	 * image series.
	 * 
	 * Static movie actions are supposed be integrated into {@link MenuBar},
	 * also to provide shortcuts. They always refer to the active layer.
	 */
	public static class StaticPreviousFrameAction extends AbstractAction
	{
		public StaticPreviousFrameAction()
		{
			super("Step to Previous Frame", ICON_BACKWARD);
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK));
		}

		public void actionPerformed(@Nullable ActionEvent e)
		{
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
	public static class StaticNextFrameAction extends AbstractAction
	{
		public StaticNextFrameAction()
		{
			super("Step to Next Frame", ICON_FORWARD);
			putValue(MNEMONIC_KEY, KeyEvent.VK_N);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_MASK));
		}

		public void actionPerformed(@Nullable ActionEvent e)
		{
			TimeLine.SINGLETON.nextFrame();
		}
	}

	@Override
	public void isPlayingChanged(boolean _isPlaying)
	{
		if(_isPlaying)
			btnPlayPause.setIcon(ICON_PAUSE);
		else
			btnPlayPause.setIcon(ICON_PLAY);
	}
}
