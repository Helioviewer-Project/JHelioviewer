package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.JHVDirectory;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.gui.actions.filefilters.JPGFilter;
import org.helioviewer.jhv.gui.actions.filefilters.PNGFilter;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog_test;
import org.helioviewer.jhv.gui.dialogs.ExportMovieSettingsDialog;

public class ExportTestAction extends AbstractAction{
	  
	public ExportTestAction() {
	        super("Export movie ..");
	        putValue(SHORT_DESCRIPTION, "Save Movie to Chosen Folder");
	        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public void actionPerformed(ActionEvent e) {
	        new ExportMovieDialog_test();
	    }
}
