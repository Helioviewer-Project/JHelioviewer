package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.base.StateParser;
import org.helioviewer.jhv.gui.MainFrame;
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
     * Constructor specifying the file to load.
     * 
     * The title of the menu item will be formed from the file name.
     * 
     * @param location
     *            URL specifying the state to load
     */
    public LoadStateAction(URL location)
    {
        this("Load state " + location.getPath().substring(location.getPath().lastIndexOf('/') + 1), location);
        putValue(SHORT_DESCRIPTION, "Loads the state saved in " + location.toString());
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

        stateLocation = new File(location.getFile());
    }

    @SuppressWarnings("null")
	public void actionPerformed(@Nullable ActionEvent e)
    {
    	try
    	{
    		if(stateLocation==null)
    			StateParser.loadStateFile();
    		else
    			StateParser.loadStateFile(stateLocation);
		}
    	catch (IOException | JSONException _e)
    	{
			JOptionPane.showMessageDialog(MainFrame.SINGLETON.MAIN_PANEL, "Could not load file: " + _e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			Telemetry.trackException(_e);
		}
    }
}
