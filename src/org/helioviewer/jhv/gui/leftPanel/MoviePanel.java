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
import org.helioviewer.jhv.layers.AbstractLayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.TimeLineListener;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class MoviePanel extends JPanel implements TimeLineListener, LayerListener
{
	private static final long serialVersionUID = 3837685812219375888L;

	// different animation speeds
	private enum PlaybackSpeedUnit
	{
		FRAMES_PER_SECOND("Frames/sec", 1),
		MINUTES_PER_SECOND("Solar minutes/sec", 60),
		HOURS_PER_SECOND("Solar hours/sec", 3600),
		DAYS_PER_SECOND("Solar days/sec", 864000);
		
		private String text;
		private int factor;

		private PlaybackSpeedUnit(String _text, int _factor)
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
		
		private AnimationMode(String _text)
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
	private JPanel optionPane;

	private TimeLine timeLine = TimeLine.SINGLETON;
	private JLabel lblFrames;

	private JSlider slider;
	private JButton btnPrevious, btnPlayPause, btnForward;

	public MoviePanel()
	{
		setBorder(new EmptyBorder(0, 2, 10, 10));
		TimeLine.SINGLETON.addListener(this);
		Layers.addNewLayerListener(this);
		setLayout(new BorderLayout());
		add(createMain(), BorderLayout.CENTER);
		optionPane = createOptionPane();
		add(optionPane, BorderLayout.SOUTH);
	}

	private JPanel createOptionPane()
	{
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
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

		SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(20, 1, 300, 1);
		final JSpinner spinner = new JSpinner(spinnerNumberModel);
		contentPanel.add(spinner, "4, 2");

		final JComboBox<PlaybackSpeedUnit> speedUnitComboBox = new JComboBox<PlaybackSpeedUnit>(
				PlaybackSpeedUnit.values());

		spinner.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				timeLine.setFPS((int) spinner.getValue() * ((PlaybackSpeedUnit) speedUnitComboBox.getSelectedItem()).factor);
			}
		});
		
		speedUnitComboBox.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				//FIXME: switching to other units doesn't work --> crash
				timeLine.setFPS((int) spinner.getValue() * ((PlaybackSpeedUnit) speedUnitComboBox.getSelectedItem()).factor);
			}
		});
		contentPanel.add(speedUnitComboBox, "6, 2, fill, default");

		JLabel lblAnimationMode = new JLabel("Animation Mode:");
		contentPanel.add(lblAnimationMode, "2, 4, 3, 1");

		final JComboBox<AnimationMode> animationModeComboBox = new JComboBox<AnimationMode>(AnimationMode.values());
		animationModeComboBox.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(ItemEvent e)
			{
				timeLine.setAnimationMode((AnimationMode) animationModeComboBox.getSelectedItem());
			}
		});
		contentPanel.add(animationModeComboBox, "6, 4, fill, default");
		contentPanel.setVisible(false);
		return contentPanel;
	}

	private JPanel createMain()
	{
		JPanel contentPanel = new JPanel();

		contentPanel.setLayout(new FormLayout(
				new ColumnSpec[]
				{
					FormFactory.RELATED_GAP_COLSPEC,
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
					FormFactory.DEFAULT_COLSPEC
				},
				new RowSpec[]
				{
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.PREF_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.PREF_ROWSPEC
				}));

		slider = new TimeSlider();
		slider.setValue(0);
		slider.setPreferredSize(new Dimension(0, 20));
		slider.setMinimum(0);
		slider.setMaximum(49);
		slider.setSnapToTicks(true);
		slider.setEnabled(false);
		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				lblFrames.setText(slider.getValue() + "/" + slider.getMaximum());
				timeLine.setCurrentFrame(slider.getValue());
			}
		});
		contentPanel.add(slider, "2, 2, 11, 1");

		btnPrevious = new JButton(ICON_BACKWARD);
		btnPrevious.setEnabled(false);
		btnPrevious.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				timeLine.previousFrame();
			}
		});
		//final int buttonSize = new JButton(ICON_PLAY).getPreferredSize().height;
		
		//btnPrevious.setPreferredSize(new Dimension(btnPrevious.getPreferredSize().width, buttonSize));
		contentPanel.add(btnPrevious, "2, 4");

		btnPlayPause = new JButton(ICON_PLAY);
		btnPlayPause.setEnabled(false);
		btnPlayPause.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				setPlaying(!timeLine.isPlaying());
			}
		});
		//btnPlayPause.setPreferredSize(new Dimension(btnPlayPause.getPreferredSize().width, buttonSize));
		contentPanel.add(btnPlayPause, "4, 4");

		btnForward = new JButton(ICON_FORWARD);
		btnForward.setEnabled(false);
		btnForward.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				timeLine.nextFrame();
			}
		});
		//btnForward.setPreferredSize(new Dimension(btnForward.getPreferredSize().width, buttonSize));
		
		contentPanel.add(btnForward, "6, 4");
		final JButton btnMoreOptions = new JButton("More Options", ICON_OPEN);
		btnMoreOptions.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (showMore)
					btnMoreOptions.setIcon(ICON_OPEN);
				else
					btnMoreOptions.setIcon(ICON_CLOSE);

				showMore = !showMore;
				optionPane.setVisible(showMore);
			}
		});
		
		contentPanel.add(btnMoreOptions, "8, 4");
		lblFrames = new JLabel();
		contentPanel.add(lblFrames, "12, 4");

		setButtonsEnabled(false);
		return contentPanel;
	}

	public void setPlaying(boolean _playing)
	{
		TimeLine.SINGLETON.setPlaying(_playing);
		if(TimeLine.SINGLETON.isPlaying())
			btnPlayPause.setIcon(ICON_PAUSE);
		else
			btnPlayPause.setIcon(ICON_PLAY);
	}

	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last)
	{
		slider.setValue(timeLine.getCurrentFrame() < 0 ? 0 : timeLine.getCurrentFrame());
	}

	public void repaintSlider()
	{
		slider.repaint();
	}

	private static class TimeSlider extends JSlider
	{
		private static final long serialVersionUID = 2053723659623341117L;

		public TimeSlider()
		{
			setUI(new TimeSliderUI(this));
		}
	}

	private static class TimeSliderUI extends BasicSliderUI
	{
		private final Color COLOR_NOT_CACHED = Color.LIGHT_GRAY;
		private final Color COLOR_PARTIALLY_CACHED = new Color(0x8080FF);
		private final Color COLOR_COMPLETELY_CACHED = new Color(0x4040FF);
		private final Color COLOR_GRAY = new Color(0xEEEEEE);

		public TimeSliderUI(JSlider slider)
		{
			super(slider);
		}

		@Override
		public void paintThumb(Graphics g)
		{
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
		public void paintTrack(Graphics g)
		{
			g.translate(trackRect.x, 0);
			
			Graphics2D g2 = (Graphics2D) g;
			
			final int Y_OFFSET = trackRect.height - 4;
			final int WIDTH = (int)trackRect.getWidth();
			final int HEIGHT = 3;

			//FIXME: this stuff is regenerated every frame --> should
			//only be recreated when something has changed
			
			ImageLayer layer = (ImageLayer) Layers.getActiveImageLayer();
			if (layer != null)
			{
				//FIXME: speed up!!!!!
				LocalDateTime[] times=layer.getLocalDateTime().toArray(new LocalDateTime[0]);
				int max=Math.min(times.length, (int)trackRect.getWidth());
				
				Match previousMatch = null;
				for(int i=0;i<max;i++)
				{
					LocalDateTime localDateTime=times[(int)Math.round(i/(double)max*times.length)];
					
					Match currentMatch = layer.getMovie(localDateTime);
					if(currentMatch==null || currentMatch.equals(previousMatch))
						g.setColor(COLOR_NOT_CACHED);
					else
						switch (currentMatch.movie.getCacheStatus())
						{
							case FILE_FULL:
								g.setColor(COLOR_COMPLETELY_CACHED);
								break;
							case KDU_PREVIEW:
								g.setColor(COLOR_PARTIALLY_CACHED);
								break;
							case NONE:
								g.setColor(COLOR_NOT_CACHED);
								break;
							default:
								throw new RuntimeException();
						}
					
					
					int xa=(int)Math.round(i/(double)max*WIDTH);
					int xb=(int)Math.round((i+1)/(double)max*WIDTH);
					g2.fillRect(xa, Y_OFFSET, xb-xa, HEIGHT);
					
					previousMatch=currentMatch;
				}
			}
			else
			{
				g.setColor(COLOR_NOT_CACHED);
				g2.fillRect(0, Y_OFFSET, WIDTH, HEIGHT);
			}

			g.translate(-trackRect.x, 0);
		}
	}

	@Override
	public void layerAdded()
	{
		boolean enable = false;
		for (AbstractLayer layer : Layers.getLayers())
			enable |= layer.isImageLayer();
		
		setButtonsEnabled(enable);
		if (Layers.getActiveImageLayer() != null)
			lblFrames.setText(slider.getValue() + "/" + slider.getMaximum());
	}

	@Override
	public void layersRemoved()
	{
		boolean enable = false;
		for (AbstractLayer layer : Layers.getLayers())
			enable |= layer.isImageLayer();
		
		setButtonsEnabled(enable);
	}

	@Override
	public void activeLayerChanged(AbstractLayer layer)
	{
		if (layer != null && layer.isImageLayer())
		{
			slider.setMaximum(((ImageLayer) layer).getLocalDateTime().size() > 0 ? ((ImageLayer) layer)
					.getLocalDateTime().size() - 1 : 0);
			lblFrames.setText(slider.getValue() + "/" + slider.getMaximum());
		}
	}

	public void setButtonsEnabled(boolean _enable)
	{
		slider.setEnabled(_enable);
		btnPrevious.setEnabled(_enable);
		btnPlayPause.setEnabled(_enable);
		btnForward.setEnabled(_enable);
		lblFrames.setText("");
	}

	@Override
	public void dateTimesChanged(int framecount)
	{
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
	public static class StaticPlayPauseAction extends AbstractAction
	{
		private static final long serialVersionUID = 7709407709240852446L;

		public StaticPlayPauseAction()
		{
			super("Play movie", ICON_PLAY);
			putValue(MNEMONIC_KEY, KeyEvent.VK_A);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_MASK));
		}

		public void actionPerformed(ActionEvent e)
		{
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
	public static class StaticPreviousFrameAction extends AbstractAction
	{
		private static final long serialVersionUID = -6342665405812226382L;

		public StaticPreviousFrameAction()
		{
			super("Step to Previous Frame", ICON_BACKWARD);
			putValue(MNEMONIC_KEY, KeyEvent.VK_P);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK));
		}

		public void actionPerformed(ActionEvent e)
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
		private static final long serialVersionUID = -8262700086251171378L;

		public StaticNextFrameAction()
		{
			super("Step to Next Frame", ICON_FORWARD);
			putValue(MNEMONIC_KEY, KeyEvent.VK_N);
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_MASK));
		}

		public void actionPerformed(ActionEvent e)
		{
			TimeLine.SINGLETON.nextFrame();
		}
	}
}
