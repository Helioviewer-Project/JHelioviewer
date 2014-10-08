package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.dialogs.ExportMovieSettingsDialog;

public class ExportSettingsAction extends AbstractAction{
	  
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ExportSettingsAction() {
	        super("Export setting test...");
	        //putValue(SHORT_DESCRIPTION, "Save Screenshot to Chosen Folder");
	        //putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public void actionPerformed(ActionEvent e) {
	        ExportMovieSettingsDialog dialog = new ExportMovieSettingsDialog();
	        dialog.setVisible(true);
	    }
}
