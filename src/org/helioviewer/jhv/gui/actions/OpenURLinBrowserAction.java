package org.helioviewer.jhv.gui.actions;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.Telemetry;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Action to open a browser showing any given URL.
 */
public class OpenURLinBrowserAction extends AbstractAction
{
    private String urlToOpen;

    /**
     * Default constructor.
     * 
     * @param name
     *            name of the action that should be displayed on a button
     * @param url
     *            URL to open on click
     */
    public OpenURLinBrowserAction(String name, String url)
    {
        super(name);
        urlToOpen = url;
    }

    public void actionPerformed(@Nullable ActionEvent e)
    {
        Telemetry.trackEvent("Open browser","URL",urlToOpen);
        Globals.openURL(urlToOpen);
    }
}
