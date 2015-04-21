package org.helioviewer.jhv;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFileChooser;

/**
 * Intended to be a class for static functions and fields relevant to the
 * application as a whole.
 * 
 * @author caplins
 */
public class JHVGlobals {
    public static final String VERSION = System.getProperty("jhvVersion") == null ? "developer" : System.getProperty("jhvVersion");
    public static String RAYGUN_TAG;
    public static final boolean OLD_RENDER_MODE = true;
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    private JHVGlobals()
    {
    }
    
    
    public static boolean isReleaseVersion()
    {
        return RAYGUN_TAG!=null;
    }
    
    public static boolean is64Bit()
    {
        return System.getProperty("os.arch").contains("64");
    }
    
    public static boolean isWindows()
    {
        return System.getProperty("os.name").toUpperCase().contains("WIN");
    }
    
    public static boolean isLinux()
    {
        return System.getProperty("os.name").toUpperCase().contains("LINUX");
    }

    public static boolean isOSX()
    {
        return System.getProperty("os.name").toUpperCase().contains("MAC OS X");
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
    
    private static LinkedBlockingQueue<JFileChooser> fileChooser=new LinkedBlockingQueue<>();
    
    public static JFileChooser getJFileChooser()
    {
        return getJFileChooser(null);
    }
    
    public static JFileChooser getJFileChooser(String _directory)
    {
        try
        {
            JFileChooser instance=fileChooser.take();
            fileChooser.add(instance);
            
            
            instance.setFileHidingEnabled(false);
            instance.setMultiSelectionEnabled(false);
            instance.setAcceptAllFileFilterUsed(false);
            instance.setFileSelectionMode(JFileChooser.FILES_ONLY);
            instance.resetChoosableFileFilters();
            instance.setSelectedFile(null);
            
            if(_directory==null)
                instance.setCurrentDirectory(null);
            else
                instance.setCurrentDirectory(new File(_directory));
            
            return instance;
        }
        catch(InterruptedException e)
        {
            //shouldn't happen, except on shutdown
            return null;
        }
    }
    
    public static void initFileChooserAsync()
    {
        Thread t=new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                fileChooser.add(new JFileChooser());
            }
        });
        
        t.setDaemon(true);
        t.start();
    }
}
