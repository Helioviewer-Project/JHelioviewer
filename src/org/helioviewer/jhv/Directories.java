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
    /** The home directory. */
    HOME {
        private final String path = System.getProperty("user.home");

        public String getPath() {
            return path + File.separator + "JHelioviewer" + File.separator;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The image cache directory. */
    CACHE {
        public String getPath() {
            return CACHE_DIR;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The JHV state directory. */
    STATES {
        public String getPath() {
            return HOME.getPath() + "States" + File.separator;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The remote files directory. */
    REMOTEFILES {
        public String getPath() {
            return HOME.getPath() + "Downloads" + File.separator;
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
    
    /**
     * Attempts to create the necessary directories if they do not exist. It
     * gets its list of directories to create from the JHVDirectory class.
     * 
     * @throws SecurityException
     */
    public static void createDirs() throws SecurityException {
        Directories[] dirs = Directories.values();
        for (Directories dir : dirs) {
            File f = dir.getFile();
            if (!f.exists()) {
                f.mkdirs();
            }
        }
    }
}

