package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.Layers;

/**
 * Action to terminate the application.
 * 
 * @author Markus Langenberg
 */
public class ExitProgramAction extends AbstractAction {

	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public ExitProgramAction() {
		super("Quit");
		putValue(SHORT_DESCRIPTION, "Quit program");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit
				.getDefaultToolkit().getMenuShortcutKeyMask()));
	}

	/**
	 * {@inheritDoc}
	 */
	public void actionPerformed(ActionEvent e) {

		if (Layers.getLayerCount() > 0) {
			int option = JOptionPane.showConfirmDialog(
					MainFrame.SINGLETON,
					"Are you sure you want to quit?", "Confirm",
					JOptionPane.OK_CANCEL_OPTION);
			if (option == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}

		System.exit(0);
	}

}
