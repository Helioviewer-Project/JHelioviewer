package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.base.StateParser;
import org.helioviewer.jhv.gui.MainFrame;
import org.json.JSONException;

public class SaveStateAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public SaveStateAction() {
        super("Save state...");
        putValue(SHORT_DESCRIPTION, "Saves the current state of JHV");
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e)
    {
    	try
    	{
			StateParser.writeStateFile();
		}
    	catch (JSONException | IOException e1)
    	{
			Telemetry.trackException(e1);
			JOptionPane.showMessageDialog(MainFrame.MAIN_PANEL, "Could not write file:\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
        
    }
}