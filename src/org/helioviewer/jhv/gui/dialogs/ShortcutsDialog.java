package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;

public class ShortcutsDialog extends JDialog
{
	private final JButton closeButton = new JButton("Close");

	public ShortcutsDialog()
	{
		super(MainFrame.SINGLETON, "Shortcuts", true);
		setModal(false);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		getContentPane().setLayout(new BorderLayout());
		setResizable(false);

		final JLabel lblNewLabel_2 = new JLabel("JHelioviewer supports the following keyboard shortcuts:");
		lblNewLabel_2.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(lblNewLabel_2, BorderLayout.NORTH);
		
		final JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(panel, BorderLayout.CENTER);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0};
		gbl_panel.rowHeights = new int[]{20, 20, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_panel.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE};
		panel.setLayout(gbl_panel);
		
		final JLabel lblNewLabel = new JLabel("ALT+C");
		lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblNewLabel.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 10);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		panel.add(lblNewLabel, gbc_lblNewLabel);
		
		final JLabel lblNewLabel_1 = new JLabel("Center image");
		GridBagConstraints gbc_lblNewLabel_1 = new GridBagConstraints();
		gbc_lblNewLabel_1.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_1.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_1.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_1.gridx = 1;
		gbc_lblNewLabel_1.gridy = 0;
		panel.add(lblNewLabel_1, gbc_lblNewLabel_1);
		
		final JLabel lblAltt = new JLabel("ALT+ENTER");
		lblAltt.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblAltt.setHorizontalAlignment(SwingConstants.TRAILING);
		GridBagConstraints gbc_lblAltt = new GridBagConstraints();
		gbc_lblAltt.anchor = GridBagConstraints.EAST;
		gbc_lblAltt.fill = GridBagConstraints.VERTICAL;
		gbc_lblAltt.insets = new Insets(0, 0, 5, 10);
		gbc_lblAltt.gridx = 0;
		gbc_lblAltt.gridy = 1;
		panel.add(lblAltt, gbc_lblAltt);
		
		final JLabel lblNewLabel_3 = new JLabel("Toggle fullscreen");
		GridBagConstraints gbc_lblNewLabel_3 = new GridBagConstraints();
		gbc_lblNewLabel_3.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_3.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_3.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_3.gridx = 1;
		gbc_lblNewLabel_3.gridy = 1;
		panel.add(lblNewLabel_3, gbc_lblNewLabel_3);
		
		final JLabel lblNewLabel_4 = new JLabel("ALT+I");
		lblNewLabel_4.setFont(new Font("Tahoma", Font.BOLD, 16));
		GridBagConstraints gbc_lblNewLabel_4 = new GridBagConstraints();
		gbc_lblNewLabel_4.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_4.insets = new Insets(0, 0, 5, 10);
		gbc_lblNewLabel_4.gridx = 0;
		gbc_lblNewLabel_4.gridy = 2;
		panel.add(lblNewLabel_4, gbc_lblNewLabel_4);
		
		final JLabel lblNewLabel_8 = new JLabel("Zoom in");
		GridBagConstraints gbc_lblNewLabel_8 = new GridBagConstraints();
		gbc_lblNewLabel_8.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_8.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_8.gridx = 1;
		gbc_lblNewLabel_8.gridy = 2;
		panel.add(lblNewLabel_8, gbc_lblNewLabel_8);
		
		final JLabel lblAlto = new JLabel("ALT+O");
		lblAlto.setFont(new Font("Tahoma", Font.BOLD, 16));
		GridBagConstraints gbc_lblAlto = new GridBagConstraints();
		gbc_lblAlto.fill = GridBagConstraints.VERTICAL;
		gbc_lblAlto.insets = new Insets(0, 0, 5, 10);
		gbc_lblAlto.gridx = 0;
		gbc_lblAlto.gridy = 3;
		panel.add(lblAlto, gbc_lblAlto);
		
		final JLabel lblZoomOut = new JLabel("Zoom out");
		GridBagConstraints gbc_lblZoomOut = new GridBagConstraints();
		gbc_lblZoomOut.anchor = GridBagConstraints.WEST;
		gbc_lblZoomOut.insets = new Insets(0, 0, 5, 0);
		gbc_lblZoomOut.gridx = 1;
		gbc_lblZoomOut.gridy = 3;
		panel.add(lblZoomOut, gbc_lblZoomOut);
		
		final JLabel lblAltk = new JLabel("ALT+K");
		lblAltk.setFont(new Font("Tahoma", Font.BOLD, 16));
		GridBagConstraints gbc_lblAltk = new GridBagConstraints();
		gbc_lblAltk.fill = GridBagConstraints.VERTICAL;
		gbc_lblAltk.insets = new Insets(0, 0, 5, 10);
		gbc_lblAltk.gridx = 0;
		gbc_lblAltk.gridy = 4;
		panel.add(lblAltk, gbc_lblAltk);
		
		final JLabel lblZoomToFit = new JLabel("Zoom to fit");
		GridBagConstraints gbc_lblZoomToFit = new GridBagConstraints();
		gbc_lblZoomToFit.anchor = GridBagConstraints.WEST;
		gbc_lblZoomToFit.insets = new Insets(0, 0, 5, 0);
		gbc_lblZoomToFit.gridx = 1;
		gbc_lblZoomToFit.gridy = 4;
		panel.add(lblZoomToFit, gbc_lblZoomToFit);
		
		final JLabel lblNewLabel_6 = new JLabel("ALT+L");
		lblNewLabel_6.setFont(new Font("Tahoma", Font.BOLD, 16));
		GridBagConstraints gbc_lblNewLabel_6 = new GridBagConstraints();
		gbc_lblNewLabel_6.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_6.insets = new Insets(0, 0, 5, 10);
		gbc_lblNewLabel_6.gridx = 0;
		gbc_lblNewLabel_6.gridy = 5;
		panel.add(lblNewLabel_6, gbc_lblNewLabel_6);
		
		final JLabel lblNewLabel_7 = new JLabel("Zoom to 100%");
		GridBagConstraints gbc_lblNewLabel_7 = new GridBagConstraints();
		gbc_lblNewLabel_7.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_7.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_7.gridx = 1;
		gbc_lblNewLabel_7.gridy = 5;
		panel.add(lblNewLabel_7, gbc_lblNewLabel_7);
		
		final JLabel lblNewLabel_5 = new JLabel("ALT+P");
		lblNewLabel_5.setFont(new Font("Tahoma", Font.BOLD, 16));
		GridBagConstraints gbc_lblNewLabel_5 = new GridBagConstraints();
		gbc_lblNewLabel_5.fill = GridBagConstraints.VERTICAL;
		gbc_lblNewLabel_5.insets = new Insets(0, 0, 5, 10);
		gbc_lblNewLabel_5.gridx = 0;
		gbc_lblNewLabel_5.gridy = 6;
		panel.add(lblNewLabel_5, gbc_lblNewLabel_5);
		
		final JLabel lblNewLabel_9 = new JLabel("Play/Pause");
		GridBagConstraints gbc_lblNewLabel_9 = new GridBagConstraints();
		gbc_lblNewLabel_9.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel_9.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel_9.gridx = 1;
		gbc_lblNewLabel_9.gridy = 6;
		panel.add(lblNewLabel_9, gbc_lblNewLabel_9);
		
		final JLabel lblAltb = new JLabel("ALT+B");
		lblAltb.setFont(new Font("Tahoma", Font.BOLD, 16));
		GridBagConstraints gbc_lblAltb = new GridBagConstraints();
		gbc_lblAltb.fill = GridBagConstraints.VERTICAL;
		gbc_lblAltb.insets = new Insets(0, 0, 5, 10);
		gbc_lblAltb.gridx = 0;
		gbc_lblAltb.gridy = 7;
		panel.add(lblAltb, gbc_lblAltb);
		
		final JLabel lblPreviousFrame = new JLabel("Previous frame");
		GridBagConstraints gbc_lblPreviousFrame = new GridBagConstraints();
		gbc_lblPreviousFrame.anchor = GridBagConstraints.WEST;
		gbc_lblPreviousFrame.insets = new Insets(0, 0, 5, 0);
		gbc_lblPreviousFrame.gridx = 1;
		gbc_lblPreviousFrame.gridy = 7;
		panel.add(lblPreviousFrame, gbc_lblPreviousFrame);
		
		final JLabel lblAltn = new JLabel("ALT+N");
		lblAltn.setFont(new Font("Tahoma", Font.BOLD, 16));
		GridBagConstraints gbc_lblAltn = new GridBagConstraints();
		gbc_lblAltn.fill = GridBagConstraints.VERTICAL;
		gbc_lblAltn.insets = new Insets(0, 0, 0, 10);
		gbc_lblAltn.gridx = 0;
		gbc_lblAltn.gridy = 8;
		panel.add(lblAltn, gbc_lblAltn);
		
		final JLabel lblNextFrame = new JLabel("Next frame");
		GridBagConstraints gbc_lblNextFrame = new GridBagConstraints();
		gbc_lblNextFrame.anchor = GridBagConstraints.WEST;
		gbc_lblNextFrame.gridx = 1;
		gbc_lblNextFrame.gridy = 8;
		panel.add(lblNextFrame, gbc_lblNextFrame);

		// the buttons panel
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));
		buttonsPanel.add(closeButton);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);

		// set the action listeners for the buttons
		closeButton.addActionListener(a -> dispose());

		pack();
		setSize(getPreferredSize().width, getPreferredSize().height);
		setLocationRelativeTo(MainFrame.SINGLETON);

		DialogTools.setDefaultButtons(closeButton, closeButton);

		setVisible(true);
	}
}
