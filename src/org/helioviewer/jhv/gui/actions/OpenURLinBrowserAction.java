package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.Globals;

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

    public void actionPerformed(ActionEvent e)
    {
        Globals.openURL(urlToOpen);
    }
}
