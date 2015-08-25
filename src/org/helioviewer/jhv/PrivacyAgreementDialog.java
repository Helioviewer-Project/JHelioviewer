package org.helioviewer.jhv;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class PrivacyAgreementDialog {

	private static JTextArea textArea = new JTextArea();
	private static JScrollPane scrollPane = new JScrollPane(textArea);
	private static final String AGREEMENT_FILE = "/Agreement.txt";

	static {
		textArea.setEditable(false);
		textArea.setText(JHVGlobals.loadFileAsString(AGREEMENT_FILE));
	}

	public static boolean showDialog(Component component) {
		Object[] options = { "Agree", "Disagree" };
		int value = JOptionPane.showOptionDialog(component, scrollPane,
				"License agreement", JOptionPane.YES_NO_OPTION,
				JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
		if (value != 0) {
			int x = JOptionPane
					.showOptionDialog(
							component,
							"The application would be closed, if you aren't agree\nAre you sure to close the application?",
							"Close", JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (x == 0) {
				Settings.setProperty(JHVGlobals.AGREEMENT_VALUE,
						Boolean.toString(false));
				System.exit(0);
			}
			Settings.setProperty(JHVGlobals.AGREEMENT_VALUE,
					Boolean.toString(true));
			return showDialog(component);
		}
		Settings.setProperty(JHVGlobals.AGREEMENT_VALUE,
				Boolean.toString(true));
		return value == 0;
	}
}
