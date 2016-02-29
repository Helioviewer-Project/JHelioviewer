package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.StateParser;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.json.JSONException;

public class SaveStateAction extends AbstractAction
{
    public SaveStateAction()
    {
        super("Save state...");
        putValue(SHORT_DESCRIPTION, "Saves the current state of JHV");
    }

    public void actionPerformed(@Nullable ActionEvent e)
    {
    	try
    	{
			File selectedFile = Globals.showFileDialog(DialogType.SAVE_FILE,
					"Save state",
					Settings.getString(StringKey.STATE_DIRECTORY),
					true,
					null,
					PredefinedFileFilter.JHV);
			
			if(selectedFile!=null)
				StateParser.saveStateFile(selectedFile);
		}
    	catch (JSONException | IOException e1)
    	{
			Telemetry.trackException(e1);
			JOptionPane.showMessageDialog(MainFrame.SINGLETON.MAIN_PANEL, "Could not write file:\n" + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
        
    }
}