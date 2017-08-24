package org.helioviewer.jhv.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.annotation.Nullable;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.helioviewer.jhv.base.MultiClickAdapter;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.WheelSupport;
import org.helioviewer.jhv.layers.LUT;
import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.layers.LayerListener;
import org.helioviewer.jhv.layers.Layers;

public class FilterPanel extends JPanel
{
	private JSlider opacitySlider;
	private JSlider sharpenSlider;
	private JSlider gammaSlider;
	private JSlider contrastSlider;
	private JComboBox<LUT> comboBoxColorTable;
	private JCheckBox red;
	private JCheckBox green;
	private JCheckBox blue;
	private JToggleButton btnInverseColorTable;
	private JLabel lblOpacity, lblSharpen, lblGamma, lblContrast, lblOpacityTitle;
	private JToggleButton coronaVisibilityButton;

	private @Nullable Layer activeLayer;
	private JLabel lblSharpenTitle;
	private JLabel lblGammaTitle;
	private JLabel lblContrastTitle;
	private JLabel lblColorTitle;
	private JLabel lblChannelsTitle;
	private static final double GAMMA_FACTOR = 0.01 * Math.log(10);

	private static final Icon ICON_INVERT = IconBank.getIcon(JHVIcon.INVERT, 16, 16);

	public FilterPanel()
	{
		setBorder(new EmptyBorder(10, 10, 10, 10));
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0, 0, 40, 40};
		gridBagLayout.rowHeights = new int[] {0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 1.0, 1.0, 0.0, 0.0};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		setLayout(gridBagLayout);
						
								lblOpacityTitle = new JLabel("Opacity");
								GridBagConstraints gbc_lblOpacityTitle = new GridBagConstraints();
								gbc_lblOpacityTitle.fill = GridBagConstraints.HORIZONTAL;
								gbc_lblOpacityTitle.insets = new Insets(0, 0, 5, 5);
								gbc_lblOpacityTitle.gridx = 0;
								gbc_lblOpacityTitle.gridy = 0;
								add(lblOpacityTitle, gbc_lblOpacityTitle);
				
						opacitySlider = new JSlider();
						lblOpacityTitle.setLabelFor(opacitySlider);
						opacitySlider.setValue(100);
						opacitySlider.setMaximum(200);
						opacitySlider.setMinorTickSpacing(20);
						opacitySlider.setPaintTicks(true);
						GridBagConstraints gbc_opacitySlider = new GridBagConstraints();
						gbc_opacitySlider.fill = GridBagConstraints.HORIZONTAL;
						gbc_opacitySlider.insets = new Insets(0, 0, 5, 5);
						gbc_opacitySlider.gridwidth = 4;
						gbc_opacitySlider.gridx = 1;
						gbc_opacitySlider.gridy = 0;
						add(opacitySlider, gbc_opacitySlider);
						WheelSupport.installMouseWheelSupport(opacitySlider);
						
								opacitySlider.addChangeListener(new ChangeListener()
								{
									@Override
									public void stateChanged(@Nullable ChangeEvent e)
									{
										lblOpacity.setText(opacitySlider.getValue() + "%");
										if (activeLayer != null && activeLayer.opacity != opacitySlider.getValue() / 100.0 && activeLayer.supportsFilterOpacity())
										{
											activeLayer.opacity = opacitySlider.getValue() / 100.0;
											MainFrame.SINGLETON.MAIN_PANEL.repaint();
										}
									}
								});
								opacitySlider.addMouseListener(new MultiClickAdapter()
								{
									@Override
									public void doubleClick(@Nullable MouseEvent e)
									{
										opacitySlider.setValue(100);
									}
								});
								lblOpacity = new JLabel("100%");
								GridBagConstraints gbc_lblOpacity = new GridBagConstraints();
								gbc_lblOpacity.anchor = GridBagConstraints.EAST;
								gbc_lblOpacity.insets = new Insets(0, 0, 5, 0);
								gbc_lblOpacity.gridx = 5;
								gbc_lblOpacity.gridy = 0;
								add(lblOpacity, gbc_lblOpacity);
						
								lblSharpenTitle = new JLabel("Sharpen");
								GridBagConstraints gbc_lblSharpenTitle = new GridBagConstraints();
								gbc_lblSharpenTitle.fill = GridBagConstraints.HORIZONTAL;
								gbc_lblSharpenTitle.insets = new Insets(0, 0, 5, 5);
								gbc_lblSharpenTitle.gridx = 0;
								gbc_lblSharpenTitle.gridy = 1;
								add(lblSharpenTitle, gbc_lblSharpenTitle);
				
						sharpenSlider = new JSlider();
						lblSharpenTitle.setLabelFor(sharpenSlider);
						sharpenSlider.setValue(0);
						sharpenSlider.setMinorTickSpacing(20);
						sharpenSlider.setPaintTicks(true);
						GridBagConstraints gbc_sharpenSlider = new GridBagConstraints();
						gbc_sharpenSlider.fill = GridBagConstraints.HORIZONTAL;
						gbc_sharpenSlider.insets = new Insets(0, 0, 5, 5);
						gbc_sharpenSlider.gridwidth = 4;
						gbc_sharpenSlider.gridx = 1;
						gbc_sharpenSlider.gridy = 1;
						add(sharpenSlider, gbc_sharpenSlider);
						WheelSupport.installMouseWheelSupport(sharpenSlider);
						sharpenSlider.addChangeListener(new ChangeListener()
						{
							@Override
							public void stateChanged(@Nullable ChangeEvent e)
							{
								lblSharpen.setText(sharpenSlider.getValue() + "%");
								if (activeLayer != null && activeLayer.sharpness != sharpenSlider.getValue() / 100.0 && activeLayer.supportsFilterSharpness())
								{
									activeLayer.sharpness = sharpenSlider.getValue() / 100.0;
									MainFrame.SINGLETON.MAIN_PANEL.repaint();
								}
							}
						});
						sharpenSlider.addMouseListener(new MultiClickAdapter()
						{
							@Override
							public void doubleClick(@Nullable MouseEvent e)
							{
								sharpenSlider.setValue(0);
							}
						});
						lblSharpen = new JLabel("0%");
						GridBagConstraints gbc_lblSharpen = new GridBagConstraints();
						gbc_lblSharpen.anchor = GridBagConstraints.EAST;
						gbc_lblSharpen.insets = new Insets(0, 0, 5, 0);
						gbc_lblSharpen.gridx = 5;
						gbc_lblSharpen.gridy = 1;
						add(lblSharpen, gbc_lblSharpen);
						
								lblGammaTitle = new JLabel("Gamma");
								GridBagConstraints gbc_lblGammaTitle = new GridBagConstraints();
								gbc_lblGammaTitle.fill = GridBagConstraints.HORIZONTAL;
								gbc_lblGammaTitle.insets = new Insets(0, 0, 5, 5);
								gbc_lblGammaTitle.gridx = 0;
								gbc_lblGammaTitle.gridy = 2;
								add(lblGammaTitle, gbc_lblGammaTitle);
				
						gammaSlider = new JSlider(JSlider.HORIZONTAL, -100, 100, 0);
						lblGammaTitle.setLabelFor(gammaSlider);
						gammaSlider.setMinorTickSpacing(20);
						gammaSlider.setPaintTicks(true);
						gammaSlider.setValue(0);
						GridBagConstraints gbc_gammaSlider = new GridBagConstraints();
						gbc_gammaSlider.fill = GridBagConstraints.HORIZONTAL;
						gbc_gammaSlider.insets = new Insets(0, 0, 5, 5);
						gbc_gammaSlider.gridwidth = 4;
						gbc_gammaSlider.gridx = 1;
						gbc_gammaSlider.gridy = 2;
						add(gammaSlider, gbc_gammaSlider);
						WheelSupport.installMouseWheelSupport(gammaSlider);
						gammaSlider.addChangeListener(new ChangeListener()
						{
							@Override
							public void stateChanged(@Nullable ChangeEvent e)
							{
								double gammaValue = Math.exp(gammaSlider.getValue() * GAMMA_FACTOR);
								String label = Double.toString(Math.round(gammaValue * 10.0) * 0.1);
								if (gammaSlider.getValue() == 100)
									label = label.substring(0, 4);
								else
									label = label.substring(0, 3);

								lblGamma.setText(label);
								if (activeLayer != null && activeLayer.gamma != gammaValue && activeLayer.supportsFilterContrastGamma())
								{
									activeLayer.gamma = gammaValue;
									MainFrame.SINGLETON.MAIN_PANEL.repaint();
								}
							}
						});
						gammaSlider.addMouseListener(new MultiClickAdapter()
						{
							@Override
							public void doubleClick(@Nullable MouseEvent e)
							{
								gammaSlider.setValue((int) (Math.log(1) / GAMMA_FACTOR));
							}
						});
				
						lblGamma = new JLabel("1.0");
						GridBagConstraints gbc_lblGamma = new GridBagConstraints();
						gbc_lblGamma.anchor = GridBagConstraints.EAST;
						gbc_lblGamma.insets = new Insets(0, 0, 5, 0);
						gbc_lblGamma.gridx = 5;
						gbc_lblGamma.gridy = 2;
						add(lblGamma, gbc_lblGamma);
				
						lblContrastTitle = new JLabel("Contrast");
						GridBagConstraints gbc_lblContrastTitle = new GridBagConstraints();
						gbc_lblContrastTitle.fill = GridBagConstraints.HORIZONTAL;
						gbc_lblContrastTitle.insets = new Insets(0, 0, 5, 5);
						gbc_lblContrastTitle.gridx = 0;
						gbc_lblContrastTitle.gridy = 3;
						add(lblContrastTitle, gbc_lblContrastTitle);
		
				contrastSlider = new JSlider();
				lblContrastTitle.setLabelFor(contrastSlider);
				contrastSlider.setMinorTickSpacing(20);
				contrastSlider.setPaintTicks(true);
				contrastSlider.setMaximum(100);
				contrastSlider.setMinimum(-100);
				contrastSlider.setValue(0);
				GridBagConstraints gbc_contrastSlider = new GridBagConstraints();
				gbc_contrastSlider.fill = GridBagConstraints.HORIZONTAL;
				gbc_contrastSlider.insets = new Insets(0, 0, 5, 5);
				gbc_contrastSlider.gridwidth = 4;
				gbc_contrastSlider.gridx = 1;
				gbc_contrastSlider.gridy = 3;
				add(contrastSlider, gbc_contrastSlider);
				WheelSupport.installMouseWheelSupport(contrastSlider);
				
						contrastSlider.addChangeListener(new ChangeListener()
						{
							@Override
							public void stateChanged(@Nullable ChangeEvent e)
							{
								lblContrast.setText(contrastSlider.getValue() + "");
								if (activeLayer != null && activeLayer.contrast != contrastSlider.getValue() / 10.0 && activeLayer.supportsFilterContrastGamma())
								{
									activeLayer.contrast = contrastSlider.getValue() / 10.0;
									MainFrame.SINGLETON.MAIN_PANEL.repaint();
								}
							}
						});
						contrastSlider.addMouseListener(new MultiClickAdapter()
						{
							@Override
							public void doubleClick(@Nullable MouseEvent e)
							{
								contrastSlider.setValue(0);
							}
						});
								
										lblContrast = new JLabel("0");
										GridBagConstraints gbc_lblContrast = new GridBagConstraints();
										gbc_lblContrast.anchor = GridBagConstraints.EAST;
										gbc_lblContrast.insets = new Insets(0, 0, 5, 0);
										gbc_lblContrast.gridx = 5;
										gbc_lblContrast.gridy = 3;
										add(lblContrast, gbc_lblContrast);
						
								lblColorTitle = new JLabel("Color");
								GridBagConstraints gbc_lblColorTitle = new GridBagConstraints();
								gbc_lblColorTitle.fill = GridBagConstraints.HORIZONTAL;
								gbc_lblColorTitle.insets = new Insets(0, 0, 5, 5);
								gbc_lblColorTitle.gridx = 0;
								gbc_lblColorTitle.gridy = 4;
								add(lblColorTitle, gbc_lblColorTitle);
						
								comboBoxColorTable = new JComboBox<LUT>();
								lblColorTitle.setLabelFor(comboBoxColorTable);
								comboBoxColorTable.setModel(new DefaultComboBoxModel<>(LUT.values()));
								comboBoxColorTable.setSelectedItem(LUT.GRAY);
								GridBagConstraints gbc_comboBoxColorTable = new GridBagConstraints();
								gbc_comboBoxColorTable.fill = GridBagConstraints.HORIZONTAL;
								gbc_comboBoxColorTable.insets = new Insets(0, 0, 5, 0);
								gbc_comboBoxColorTable.gridwidth = 5;
								gbc_comboBoxColorTable.gridx = 1;
								gbc_comboBoxColorTable.gridy = 4;
								add(comboBoxColorTable, gbc_comboBoxColorTable);
								comboBoxColorTable.addItemListener(new ItemListener()
								{
									@Override
									public void itemStateChanged(@Nullable ItemEvent e)
									{
										if (activeLayer != null && activeLayer.getLUT() != comboBoxColorTable.getSelectedItem() && activeLayer.supportsFilterLUT())
										{
											activeLayer.setLUT((LUT) comboBoxColorTable.getSelectedItem());
											MainFrame.SINGLETON.MAIN_PANEL.repaint();
										}
									}
								});
						
								lblChannelsTitle = new JLabel("Channels");
								GridBagConstraints gbc_lblChannelsTitle = new GridBagConstraints();
								gbc_lblChannelsTitle.fill = GridBagConstraints.HORIZONTAL;
								gbc_lblChannelsTitle.insets = new Insets(0, 0, 0, 5);
								gbc_lblChannelsTitle.gridx = 0;
								gbc_lblChannelsTitle.gridy = 5;
								add(lblChannelsTitle, gbc_lblChannelsTitle);
				
						red = new JCheckBox("");
						red.setHorizontalAlignment(SwingConstants.CENTER);
						red.setContentAreaFilled(false);
						red.setForeground(Color.RED);
						red.addItemListener(e ->
						{
							if(activeLayer!=null && activeLayer.redChannel!=red.isSelected() && activeLayer.supportsFilterRGB())
							{
								activeLayer.redChannel=red.isSelected();
								MainFrame.SINGLETON.MAIN_PANEL.repaint();
							}
						});

						
						GridBagConstraints gbc_red = new GridBagConstraints();
						gbc_red.fill = GridBagConstraints.BOTH;
						gbc_red.insets = new Insets(0, 0, 0, 5);
						gbc_red.gridx = 1;
						gbc_red.gridy = 5;
						add(red, gbc_red);
		
				green = new JCheckBox("");
				green.setHorizontalAlignment(SwingConstants.CENTER);
				green.setContentAreaFilled(false);
				green.setForeground(Color.GREEN.darker());
				GridBagConstraints gbc_green = new GridBagConstraints();
				gbc_green.fill = GridBagConstraints.BOTH;
				gbc_green.insets = new Insets(0, 0, 0, 5);
				gbc_green.gridx = 2;
				gbc_green.gridy = 5;
				add(green, gbc_green);
				
				green.addItemListener(e ->
				{
					if(activeLayer!=null && activeLayer.greenChannel!=green.isSelected() && activeLayer.supportsFilterRGB())
					{
						activeLayer.greenChannel=green.isSelected();
						MainFrame.SINGLETON.MAIN_PANEL.repaint();
					}
				});
		
		
		coronaVisibilityButton = new JToggleButton();
		coronaVisibilityButton.setBackground(Color.WHITE);
		coronaVisibilityButton.setIcon(IconBank.getIcon(JHVIcon.SUN_WITHOUT_128x128, 24, 24));
		coronaVisibilityButton.setSelectedIcon(IconBank.getIcon(JHVIcon.SUN_WITH_128x128, 24, 24));
		coronaVisibilityButton.setToolTipText("Toggle Corona Visibility");
		coronaVisibilityButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(@Nullable ActionEvent e)
			{
				if(activeLayer!=null && activeLayer.supportsFilterCorona())
				{
					activeLayer.toggleCoronaVisibility();
					MainFrame.SINGLETON.MAIN_PANEL.repaint();
				}
			}
		});
		
				blue = new JCheckBox("");
				blue.setHorizontalAlignment(SwingConstants.CENTER);
				blue.setContentAreaFilled(false);
				blue.setForeground(Color.BLUE);
				GridBagConstraints gbc_blue = new GridBagConstraints();
				gbc_blue.fill = GridBagConstraints.BOTH;
				gbc_blue.insets = new Insets(0, 0, 0, 5);
				gbc_blue.gridx = 3;
				gbc_blue.gridy = 5;
				add(blue, gbc_blue);
				blue.addItemListener(e ->
				{
					if(activeLayer!=null && activeLayer.blueChannel!=blue.isSelected() && activeLayer.supportsFilterRGB())
					{
						activeLayer.blueChannel=blue.isSelected();
						MainFrame.SINGLETON.MAIN_PANEL.repaint();
					}
				});

		
		GridBagConstraints gbc_coronaVisibilityButton = new GridBagConstraints();
		gbc_coronaVisibilityButton.fill = GridBagConstraints.HORIZONTAL;
		gbc_coronaVisibilityButton.insets = new Insets(0, 0, 0, 5);
		gbc_coronaVisibilityButton.gridx = 4;
		gbc_coronaVisibilityButton.gridy = 5;
		add(coronaVisibilityButton, gbc_coronaVisibilityButton);
		
				btnInverseColorTable = new JToggleButton(ICON_INVERT);
				GridBagConstraints gbc_btnInverseColorTable = new GridBagConstraints();
				gbc_btnInverseColorTable.fill = GridBagConstraints.BOTH;
				gbc_btnInverseColorTable.gridx = 5;
				gbc_btnInverseColorTable.gridy = 5;
				add(btnInverseColorTable, gbc_btnInverseColorTable);
				btnInverseColorTable.addChangeListener(new ChangeListener()
				{
					@Override
					public void stateChanged(@Nullable ChangeEvent e)
					{
						if (activeLayer != null && activeLayer.invertedLut != btnInverseColorTable.isSelected() && activeLayer.supportsFilterLUT())
						{
							activeLayer.invertedLut = btnInverseColorTable.isSelected();
							MainFrame.SINGLETON.MAIN_PANEL.repaint();
						}
					}
				});
		
		Layers.addLayerListener(new LayerListener()
		{
			@Override
			public void layerAdded()
			{
			}

			@Override
			public void layersRemoved()
			{
			}

			@Override
			public void activeLayerChanged(@Nullable Layer layer)
			{
				activeLayer = layer;
				update();
			}
		});
		
		BGLabel lblBGRed = new BGLabel(JHVIcon.BG_RED);
		lblBGRed.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_lblBGRed = new GridBagConstraints();
		gbc_lblBGRed.fill = GridBagConstraints.BOTH;
		gbc_lblBGRed.insets = new Insets(0, 0, 0, 5);
		gbc_lblBGRed.gridx = 1;
		gbc_lblBGRed.gridy = 5;
		add(lblBGRed, gbc_lblBGRed);

		BGLabel lblBGGreen = new BGLabel(JHVIcon.BG_GREEN);
		lblBGGreen.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_lblBGGreen = new GridBagConstraints();
		gbc_lblBGGreen.fill = GridBagConstraints.BOTH;
		gbc_lblBGGreen.insets = new Insets(0, 0, 0, 5);
		gbc_lblBGGreen.gridx = 2;
		gbc_lblBGGreen.gridy = 5;
		add(lblBGGreen, gbc_lblBGGreen);
	
		BGLabel lblBGBlue = new BGLabel(JHVIcon.BG_BLUE);
		lblBGBlue.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_lblBGBlue = new GridBagConstraints();
		gbc_lblBGBlue.fill = GridBagConstraints.BOTH;
		gbc_lblBGBlue.insets = new Insets(0, 0, 0, 5);
		gbc_lblBGBlue.gridx = 3;
		gbc_lblBGBlue.gridy = 5;
		add(lblBGBlue, gbc_lblBGBlue);
	}
	
	
	class BGLabel extends JLabel
	{
		BufferedImage bg;
		
		BGLabel(JHVIcon _icon)
		{
			bg=IconBank.getImage(_icon);
		}
		
		@Override
		protected void paintComponent(Graphics _g)
		{
			int w=getWidth()-2;
			int h=getHeight();
			if(w<=2*h)
				_g.drawImage(bg, 0, 0, w, h, null);
			else
			{
				super.paintComponent(_g);
				_g.drawImage(bg, w/2-h, 0, 2*h, h, null);
			}
		}
	}

	public void update()
	{
		for (Component c : getComponents())
			c.setEnabled(false);
		
		if(activeLayer != null && activeLayer.supportsFilterContrastGamma())
		{
			lblContrastTitle.setEnabled(true);
			lblContrast.setEnabled(true);
			contrastSlider.setEnabled(true);
			contrastSlider.setValue((int) (activeLayer.contrast * 10));
			
			lblGammaTitle.setEnabled(true);
			lblGamma.setEnabled(true);
			gammaSlider.setEnabled(true);
			gammaSlider.setValue((int) (Math.log(activeLayer.gamma) / GAMMA_FACTOR));
		}
		
		if(activeLayer != null && activeLayer.supportsFilterCorona())
		{
			lblChannelsTitle.setEnabled(true);
			coronaVisibilityButton.setEnabled(true);
			coronaVisibilityButton.setSelected(activeLayer.isCoronaVisible());
		}
		else
			coronaVisibilityButton.setSelected(false);
		
		if(activeLayer != null && activeLayer.supportsFilterLUT())
		{
			lblChannelsTitle.setEnabled(true);
			comboBoxColorTable.setEnabled(true);
			comboBoxColorTable.setSelectedItem(activeLayer.getLUT());
			
			btnInverseColorTable.setEnabled(true);
			btnInverseColorTable.setSelected(activeLayer.invertedLut);
		}
		else
			btnInverseColorTable.setSelected(false);
		
		if(activeLayer != null && activeLayer.supportsFilterOpacity())
		{
			lblOpacityTitle.setEnabled(true);
			lblOpacity.setEnabled(true);
			opacitySlider.setEnabled(true);
			opacitySlider.setValue((int) (activeLayer.opacity * 100));
		}
		
		if(activeLayer != null && activeLayer.supportsFilterRGB())
		{
			lblChannelsTitle.setEnabled(true);
			
			red.setEnabled(true);
			green.setEnabled(true);
			blue.setEnabled(true);
			
			red.setSelected(activeLayer.redChannel);
			green.setSelected(activeLayer.greenChannel);
			blue.setSelected(activeLayer.blueChannel);
		}
		else
		{
			red.setSelected(false);
			green.setSelected(false);
			blue.setSelected(false);
		}
		
		if(activeLayer != null && activeLayer.supportsFilterSharpness())
		{
			lblSharpenTitle.setEnabled(true);
			lblSharpen.setEnabled(true);
			sharpenSlider.setEnabled(true);
			sharpenSlider.setValue((int) (activeLayer.sharpness * 100));
		}
	}
}
