package org.helioviewer.jhv;

import java.io.File;

/**
 * An enum containing all the directories mapped in a system independent way. If
 * a new directory is required, just add it here and it will be created at
 * startup.
 * 
 * @author caplins
 * 
 */
public enum Directories {
    /** The image cache directory. */
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

    private static final String CACHE_DIR=System.getProperty("java.io.tmpdir")+File.separator+"jhv-cache"+File.separator;
    
    static
    {
        new File(CACHE.getPath()).mkdir();
    }
}

