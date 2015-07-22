package org.helioviewer.jhv;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import org.helioviewer.jhv.gui.opengl.MainPanel;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class PrivacyAgreementDialog extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = 615216701212549757L;

	private final JPanel contentPanel = new JPanel();

	private JCheckBox chckbxRemindMe;
	
	private final String[] args;

	public PrivacyAgreementDialog(String[] args) {
		this.args = args;
		this.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
		this.setResizable(false);
		initGui();
	}
	
	private void initGui(){
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BorderLayout(0, 0));
		{
			JTextArea textArea = new JTextArea();
			JScrollPane scrollPane = new JScrollPane(textArea);
			textArea.setEditable(false);
			textArea.setText(JHVGlobals.loadFileAsString(JHVGlobals.AGREEMENT_FILE));
			contentPanel.add(scrollPane, BorderLayout.CENTER);
		}
		{
			JPanel buttonPane = new JPanel();
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			buttonPane.setLayout(new FormLayout(new ColumnSpec[] {
					ColumnSpec.decode("279px"),
					ColumnSpec.decode("86px"),
					FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
					ColumnSpec.decode("75px"),},
				new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					RowSpec.decode("29px"),}));
			{
				chckbxRemindMe = new JCheckBox("Remind me");
				chckbxRemindMe.setSelected(true);
				buttonPane.add(chckbxRemindMe, "1, 2");
			}
			{
				JButton okButton = new JButton("Agree");
				okButton.setActionCommand("Agree");
				okButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						Settings.setProperty(JHVGlobals.AGREEMENT_VALUE, Boolean.toString(true));
						Settings.setProperty(JHVGlobals.AGREEMENT_REMIND_ME, Boolean.toString(chckbxRemindMe.isSelected()));
						dispose();
						JHelioviewer.startUpJHelioviewer(args);
					}
				});
				buttonPane.add(okButton, "4, 2, left, top");
				getRootPane().setDefaultButton(okButton);
			}
			{
				JButton cancelButton = new JButton("Disagree");
				cancelButton.setActionCommand("Disagree");
				cancelButton.addActionListener(new ActionListener() {
					
					@Override
					public void actionPerformed(ActionEvent e) {
						Settings.setProperty(JHVGlobals.AGREEMENT_VALUE, Boolean.toString(false));
						Settings.setProperty(JHVGlobals.AGREEMENT_REMIND_ME, Boolean.toString(chckbxRemindMe.isSelected()));
						System.exit(0);
					}
				});
				buttonPane.add(cancelButton, "2, 2, left, top");
			}
		}
	}	
}
