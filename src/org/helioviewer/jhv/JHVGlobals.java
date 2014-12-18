package org.helioviewer.jhv;

import java.awt.Desktop;
import java.net.URI;

/**
 * Intended to be a class for static functions and fields relevant to the
 * application as a whole.
 * 
 * @author caplins
 */
public class JHVGlobals {
    public static final double VERSION = 2.3;
    public static final String RELEASE = "Beta 6";
    public static final String VERSION_AND_RELEASE = JHVGlobals.VERSION + (JHVGlobals.RELEASE!=null ? " (" + JHVGlobals.RELEASE + ")":"");

    private JHVGlobals()
    {
    }

    /**
     * @return standard read timeout
     */
    public static int getStdReadTimeout() {
        return Integer.parseInt(Settings.getProperty("connection.read.timeout"));
    }

    /**
     * @return standard connect timeout
     */
    public static int getStdConnectTimeout() {
        return Integer.parseInt(Settings.getProperty("connection.connect.timeout"));
    }

    /**
     * Opens the specified web page in the default web browser
     * 
     * @param url
     *            A web address (URL) of a web page (e.g
     *            "http://www.jhelioviewer.org/")
     */
    public static void openURL(String url) {
        try
        {
            Desktop.getDesktop().browse(new URI(url));
        }
        catch(Exception e)
        {
            e.printStackTrace();
            try
            {
                new ProcessBuilder("x-www-browser",url).start();
            }
            catch(Exception e2)
            {
                e2.printStackTrace();
            }
        }
    }

}
