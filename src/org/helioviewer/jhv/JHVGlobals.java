package org.helioviewer.jhv;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;

import javax.swing.JOptionPane;

import org.apache.log4j.Level;
import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.logging.Log;

/**
 * Intended to be a class for static functions and fields relevant to the
 * application as a whole.
 * 
 * @author caplins
 */
public class JHVGlobals {
    private static final String[] BROWSERS = { "firefox", "opera", "konqueror", "epiphany", "seamonkey", "galeon", "kazehakase", "mozilla", "netscape" };

    public static final String TEMP_FILENAME_DELETE_PLUGIN_FILES = "delete-plugins.tmp";

    public static final double VERSION = 2.3;
    public static final String RELEASE = "Beta 2";

    /** Constructor is private to prevent instantiation. */
    private JHVGlobals() {

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
     * Attempts to create the necessary directories if they do not exist. It
     * gets its list of directories to create from the JHVDirectory class.
     * 
     * @throws SecurityException
     */
    public static void createDirs() throws SecurityException {
        JHVDirectory[] dirs = JHVDirectory.values();
        for (JHVDirectory dir : dirs) {
            File f = dir.getFile();
            if (!f.exists()) {
                f.mkdirs();
            }
        }
    }

    /**
     * Opens the specified web page in the default web browser
     * 
     * @param url
     *            A web address (URL) of a web page (e.g
     *            "http://www.jhelioviewer.org/")
     */
    public static void openURL(String url) {
        Log.info("Opening URL " + url);
        String functionCall = "openURL(" + url + ")";
        String functionCallEntry = ">> " + functionCall;
        Log.trace(">> " + functionCall);

        try { // attempt to use Desktop library from JDK 1.6+ (even if on 1.5)
            Log.debug(functionCallEntry + " > Try to use java.awt.Desktop class from JDK 1.6+");
            Class<?> d = Class.forName("java.awt.Desktop");
            d.getDeclaredMethod("browse", new Class[] { java.net.URI.class }).invoke(d.getDeclaredMethod("getDesktop").invoke(null), new Object[] { java.net.URI.create(url) });
        } catch (Exception ignore) { // library not available or failed
            Log.debug(functionCallEntry + " > Loading class java.awt.Desktop failed. Trying other methods to open URL.");
            String osName = System.getProperty("os.name");
            Log.trace(functionCallEntry + " > OS: " + osName);
            try {
                if (osName.startsWith("Mac OS")) {
                    Log.debug(functionCallEntry + " > Open URL assuming MacOS");
                    Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
                    Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
                    openURL.invoke(null, new Object[] { url });

                } else if (osName.startsWith("Windows")) {
                    Log.debug(functionCallEntry + " > Open URL assuming Windows");
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else { // assume Unix or Linux
                    Log.debug(functionCallEntry + " > Open URL assuming Unix");
                    boolean found = false;
                    for (String browser : BROWSERS) {
                        if (!found) {
                            Process p = Runtime.getRuntime().exec(new String[] { "which", browser });
                            FileUtils.logProcessOutput(p, "which", Level.DEBUG, true);
                            found = p.waitFor() == 0;
                            if (found) {
                                p = Runtime.getRuntime().exec(new String[] { browser, url });
                                FileUtils.logProcessOutput(p, browser, Level.DEBUG, false);
                            }
                        }
                    }
                    if (!found) {
                        throw new Exception(Arrays.toString(BROWSERS));
                    }
                }
            } catch (Exception e) {
                Log.error("Error attempting to launch web browser", e);
                JOptionPane.showMessageDialog(null, "Error attempting to launch web browser\n" + e.toString());
            }
        }
        Log.trace("<< " + functionCall);
    }

}
