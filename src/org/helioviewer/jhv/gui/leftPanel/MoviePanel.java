package org.helioviewer.jhv.gui.leftPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.NewLayerListener;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine.TimeLineListener;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class MoviePanel extends JPanel implements TimeLineListener, NewLayerListener{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3837685812219375888L;

	// different animation speeds
    private enum SpeedUnit {
        FRAMESPERSECOND {
            public String toString() {
                return "Frames/sec";
            }

            public int getSecondsPerSecond() {
                return 0;
            }
        },
        MINUTESPERSECOND {
            public String toString() {
                return "Solar minutes/sec";
            }

            public int getSecondsPerSecond() {
                return 60;
            }
        },
        HOURSPERSECOND {
            public String toString() {
                return "Solar hours/sec";
            }

            public int getSecondsPerSecond() {
                return 3600;
            }
        },
        DAYSPERSECOND {
            public String toString() {
                return "Solar days/sec";
            }

            public int getSecondsPerSecond() {
                return 86400;
            }
        };

        public abstract int getSecondsPerSecond();
    }
    
    private enum AnimationMode
    {
        LOOP
        {
            public String toString()
            {
                return "Loop";
            }
        },
        STOP
        {
            public String toString()
            {
                return "Stop";
            }
        },
        SWING
        {
            public String toString()
            {
                return "Swing";
            }
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
	public MoviePanel() {
		TimeLine.SINGLETON.addListener(this);
		Layers.LAYERS.addNewLayerListener(this);
		this.setLayout(new BorderLayout());
		this.add(initGUI(), BorderLayout.CENTER);
		optionPane = initOptionPane();
		this.add(optionPane, BorderLayout.SOUTH);
		this.setMinimumSize(new Dimension(200, 60));
		this.setPreferredSize(new Dimension(200, 60));
	}
	
	private JPanel initOptionPane(){
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(28dlu;default)"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(33dlu;default)"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		JLabel lblSpeed = new JLabel("Speed:");
		contentPanel.add(lblSpeed, "2, 2");
		

		SpinnerNumberModel spinnerNumberModel = new SpinnerNumberModel(20, 1, 300, 1);
		final JSpinner spinner = new JSpinner(spinnerNumberModel);
		contentPanel.add(spinner, "4, 2");
		
		final JComboBox<SpeedUnit> speedUnitComboBox = new JComboBox<SpeedUnit>(SpeedUnit.values());

		spinner.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				timeLine.setSpeedFactor((int)spinner.getValue());
			}
		});
		speedUnitComboBox.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				timeLine.setSpeedFactor((int)spinner.getValue());
			}
		});
		contentPanel.add(speedUnitComboBox, "6, 2, fill, default");
		
		JLabel lblAnimationMode = new JLabel("Animation Mode:");
		contentPanel.add(lblAnimationMode, "2, 4, 3, 1");
		
		JComboBox<AnimationMode> animationModeComboBox = new JComboBox<AnimationMode>(AnimationMode.values());
		contentPanel.add(animationModeComboBox, "6, 4, fill, default");
		contentPanel.setVisible(false);
		return contentPanel;
	}
	
	private JPanel initGUI(){
		JPanel contentPanel = new JPanel();
		
		contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));


		
		
		
		
		
		slider = new TimeSlider();
		slider.setValue(0);
		slider.setMinimum(0);
		slider.setMaximum(49);
        slider.setSnapToTicks(true);
        slider.setEnabled(false);
        slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				lblFrames.setText(slider.getValue() + "/" + timeLine.getMaxFrames());
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
		contentPanel.add(btnPrevious, "2, 4");
		
		btnPlayPause = new JButton(ICON_PLAY);
		btnPlayPause.setEnabled(false);
		btnPlayPause.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (timeLine.isPlaying()) btnPlayPause.setIcon(ICON_PLAY);
				else btnPlayPause.setIcon(ICON_PAUSE);
				timeLine.setPlaying(!timeLine.isPlaying());
			}
		});
		contentPanel.add(btnPlayPause, "4, 4");
		
		btnForward = new JButton(ICON_FORWARD);
		btnForward.setEnabled(false);
		btnForward.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				timeLine.nextFrame();
			}
		});
		contentPanel.add(btnForward, "6, 4");
		final JButton btnOptionPane = new JButton("More Options", ICON_OPEN);
		btnOptionPane.setToolTipText("More Options to Control Playback");
		btnOptionPane.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (showMore){
					btnOptionPane.setIcon(ICON_OPEN);
					setPreferredSize(new Dimension(300, 60));
				}
				else {
					btnOptionPane.setIcon(ICON_CLOSE);
					setPreferredSize(new Dimension(300, 130));
				}
				showMore = !showMore;
				optionPane.setVisible(showMore);
			}
		});
		contentPanel.add(btnOptionPane, "8, 4");
		lblFrames = new JLabel();
		contentPanel.add(lblFrames, "12, 4");
		
		
		contentPanel.setPreferredSize(new Dimension(300, 300));
		return contentPanel;
	}
	
	@Override
	public void timeStampChanged(LocalDateTime current, LocalDateTime last) {
		slider.setValue(timeLine.getCurrentFrame());
	}
	
	private class TimeSlider extends JSlider{
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 2053723659623341117L;

		public TimeSlider() {
			setUI(new TimeSliderUI(this));
		}
		
		
	}
	
	private class TimeSliderUI extends BasicSliderUI{
		public TimeSliderUI(JSlider slider) {
			super(slider);
		}

		@Override
		public void paintThumb(Graphics g) {
			g.setColor(Color.BLACK);
            g.drawRect(thumbRect.x, thumbRect.y, thumbRect.width - 1, thumbRect.height - 1);
            int x = thumbRect.x + (thumbRect.width - 1) / 2;
            g.drawLine(x, thumbRect.y, x, thumbRect.y + thumbRect.height - 1);
		}
	}

	@Override
	public void newlayerAdded() {
		setEnableButtons(true);
	}

	@Override
	public void newlayerRemoved(int idx) {
		setEnableButtons(Layers.LAYERS.getLayerCount() > 0);
	}

	@Override
	public void activeLayerChanged(LayerInterface layer) {
		slider.setValue(slider.getValue()-1);
		slider.setValue(slider.getValue());
	}

	private void setEnableButtons(boolean enable){
		slider.setEnabled(enable);
		btnPrevious.setEnabled(enable);
		btnPlayPause.setEnabled(enable);
		btnForward.setEnabled(enable);
		lblFrames.setText("");
	}

	@Override
	public void dateTimesChanged(int framecount) {
		System.out.println("dateTimesChanged");
		this.slider.setMaximum(framecount);
		lblFrames.setText(slider.getValue() + "/" + framecount);
		this.repaint();
	}
	
	   /**
     * Action to play or pause the active layer, if it is an image series.
     * 
     * Static movie actions are supposed be integrated into {@link MenuBar},
     * also to provide shortcuts. They always refer to the active layer.
     */
    public static class StaticPlayPauseAction extends AbstractAction{

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
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.ALT_MASK));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {
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
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.ALT_MASK));
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
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.ALT_MASK));
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent e) {
        	TimeLine.SINGLETON.nextFrame();
        }
    }
	
}
