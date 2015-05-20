package org.helioviewer.jhv.gui.actions;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.gui.dialogs.ExportMovieDialog;
import org.helioviewer.jhv.layers.Layers;

public class ExportAction extends AbstractAction{
  private static final long serialVersionUID=-1780397745337916864L;

  public ExportAction() {
	        super("Save movie as...");
	        putValue(SHORT_DESCRIPTION, "Export a movie to a file");
	        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.SHIFT_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	    }

	    /**
	     * {@inheritDoc}
	     */
	    public void actionPerformed(ActionEvent e) {
          if(Layers.LAYERS.getLayerCount() > 0)
        	  new ExportMovieDialog();
          else
            JOptionPane.showMessageDialog(MainFrame.MAIN_PANEL, "At least one active layer must be visible.\n\nPlease add a layer before exporting movies.", "Error", JOptionPane.ERROR_MESSAGE);
	    }
}
