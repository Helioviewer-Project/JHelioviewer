package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.JHVUncaughtExceptionHandler;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.StateParser;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.json.JSONException;

public class LoadStateAction extends AbstractAction
{
    private final @Nullable File stateLocation;

    /**
     * Constructor specifying no location to load.
     * 
     * The user will be prompted to select the file to be loaded. The title will
     * be "Load State..." in every case.
     */
    public LoadStateAction()
    {
        super("Load state...");
        putValue(SHORT_DESCRIPTION, "Loads the saved state from a file");
        stateLocation = null;
    }

    /**
     * Constructor specifying the title of the menu item and the file to load.
     * 
     * @param title
     *            Title of the menu item
     * @param location
     *            URL specifying the state to load
     */
    public LoadStateAction(String title, URL location)
    {
        super(title);
        putValue(SHORT_DESCRIPTION, "Loads the saved state");

        try
		{
			stateLocation = new File(URLDecoder.decode(location.getFile(),"UTF-8"));
		}
		catch (UnsupportedEncodingException _e)
		{
			throw new RuntimeException(_e);
		}
    }

	public void actionPerformed(@Nullable ActionEvent e)
    {
		File loc=stateLocation;
    	try
    	{
    		if(loc==null)
    		{
    			loc = Globals.showFileDialog(
    					DialogType.OPEN_FILE,
    					"Open saved state",
    					Settings.getString(StringKey.STATE_DIRECTORY),
    					true,
    					null,
    					PredefinedFileFilter.JHV
    				);
    			
				if (loc==null)
					return;
				
				Settings.setString(StringKey.STATE_DIRECTORY, loc.getParentFile().getAbsolutePath());
    		}
    		
			StateParser.loadStateFile(loc);
		}
    	catch (IOException | JSONException _e)
    	{
			JOptionPane.showMessageDialog(MainFrame.SINGLETON.MAIN_PANEL, "Could not load file "+loc.getAbsolutePath()+":\n" + _e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			Telemetry.trackException(_e);
		}
    }
}
