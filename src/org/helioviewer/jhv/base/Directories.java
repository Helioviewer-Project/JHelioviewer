package org.helioviewer.jhv.base;

import java.io.File;

/**
 * An enum containing all the directories mapped in a system independent way. If
 * a new directory is required, just add it here and it will be created at
 * startup.
 */
public enum Directories
{
    /** The remote files directory. */
    REMOTEFILES {
        public String getPath() {
            return DOWNLOAD_DIR + File.separator;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    CACHE {
        public String getPath() {
            return CACHE_DIR;
        }

        public File getFile() {
            return new File(getPath());
        }
    };
    
    /** A String representation of the path of the directory. */
    abstract public String getPath();

    /** A File representation of the path of the directory. */
    abstract public File getFile();

    private static final String CACHE_DIR=System.getProperty("java.io.tmpdir")+"jhv-cache"+File.separator;
    private static String DOWNLOAD_DIR;
    
    static
    {
        new File(CACHE.getPath()).mkdir();
        
        DOWNLOAD_DIR = System.getProperty("user.home")
                + File.separator + "JHelioviewer"
                + File.separator + "Downloads";
        if(!new File(DOWNLOAD_DIR).isDirectory())
        {
            DOWNLOAD_DIR = System.getProperty("user.home")
                    + File.separator + "Downloads";
            if(!new File(DOWNLOAD_DIR).isDirectory())
            {
                DOWNLOAD_DIR = System.getProperty("user.home")
                        + File.separator + "downloads";
                if(!new File(DOWNLOAD_DIR).isDirectory())
                {
                    DOWNLOAD_DIR = System.getProperty("user.home");
                }
            }
        }
    }
}

