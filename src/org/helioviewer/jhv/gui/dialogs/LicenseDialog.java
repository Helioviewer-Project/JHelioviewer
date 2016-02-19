package org.helioviewer.jhv.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.annotation.Nullable;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;

public class LicenseDialog extends JDialog
{
	private JButton acceptBtn;
	private JButton cancelBtn;
	private boolean agreed=false;
	
	public boolean didAgree()
	{
		return agreed;
	}

	public LicenseDialog()
	{
		setIconImage(IconBank.getIcon(JHVIcon.HVLOGO_SMALL).getImage());
		setTitle("End User License Agreement");
		setModal(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		Telemetry.trackEvent("Dialog", "Type", getClass().getSimpleName());

		BorderLayout bl_mainPanel = new BorderLayout();
		bl_mainPanel.setVgap(10);
		bl_mainPanel.setHgap(10);
		JPanel mainPanel = new JPanel(bl_mainPanel);
		mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		final JTextArea txtpnPleaseTakeA = new JTextArea();
		txtpnPleaseTakeA.setFont(UIManager.getFont("Label.font"));
		txtpnPleaseTakeA.setLineWrap(true);
		txtpnPleaseTakeA.setWrapStyleWord(true);
		txtpnPleaseTakeA.setFocusable(false);
		mainPanel.add(txtpnPleaseTakeA, BorderLayout.NORTH);
		txtpnPleaseTakeA.setBackground(SystemColor.control);
		txtpnPleaseTakeA.setEditable(false);
		txtpnPleaseTakeA.setOpaque(false);
		
		txtpnPleaseTakeA.setText("Please take a moment to read the license agreement now. If you accept the terms below, click \"Accept\" and then \"OK\".");
		
		final JTextArea textArea = new JTextArea();
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);

		
		final JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		
		textArea.setText(Globals.loadFile("/eula.txt"));
		JPanel btnPanel = new JPanel();
		btnPanel.setLayout(new BorderLayout(0, 0));
		
		final JCheckBox chkAccept = new JCheckBox("Accept license agreement");
		btnPanel.add(chkAccept, BorderLayout.WEST);
		
		JPanel panel_2 = new JPanel();
		btnPanel.add(panel_2, BorderLayout.EAST);
				panel_2.setLayout(new GridLayout(0, 2, 15, 0));
		
				acceptBtn = new JButton("OK");
				acceptBtn.setEnabled(false);
				panel_2.add(acceptBtn);
				
						acceptBtn.addActionListener(new ActionListener()
						{
							public void actionPerformed(@Nullable ActionEvent e)
							{
								if(chkAccept.isSelected())
								{
									agreed=true;
									dispose();
								}
							}
						});
						cancelBtn = new JButton("Cancel");
						panel_2.add(cancelBtn);
						
						cancelBtn.addActionListener(new ActionListener()
						{
							public void actionPerformed(@Nullable ActionEvent e)
							{
								if (JOptionPane.showConfirmDialog(null, "Do you want to exit JHelioviewer?", "Attention",
										JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
									dispose();
							}
						});
		
		if (Globals.IS_OS_X)
		{
			panel_2.remove(cancelBtn);
			panel_2.remove(acceptBtn);
			
			panel_2.add(cancelBtn);
			panel_2.add(acceptBtn);
		}
		
		chkAccept.addItemListener(new ItemListener()
		{
			@Override
			public void itemStateChanged(@Nullable ItemEvent e)
			{
				acceptBtn.setEnabled(chkAccept.isSelected());
			}
		});

		mainPanel.add(btnPanel, BorderLayout.SOUTH);
		textArea.setSelectionStart(0);
		textArea.setSelectionEnd(0);
		
		getContentPane().add(mainPanel);

		DialogTools.setDefaultButtons(acceptBtn, cancelBtn);

		setSize(new Dimension(800, 600));
		validate();
		
		setLocationRelativeTo(null);
		setVisible(true);
	}
}
