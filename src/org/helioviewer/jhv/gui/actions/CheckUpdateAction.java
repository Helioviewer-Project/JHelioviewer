package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.UpdateChecker;

/**
 * Checks for updates action
 * 
 * @author Helge Dietert
 */
public class CheckUpdateAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public CheckUpdateAction() {
        super("Check for Updates...");
        putValue(SHORT_DESCRIPTION, "Check for Newer Releases");
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent arg0) {
        UpdateChecker update;
        try {
            update = new UpdateChecker();
            update.setVerbose(true);
            update.check();
        } catch (MalformedURLException e) {
            // Should not happen
            System.err.println("Error while parsing update url " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}
