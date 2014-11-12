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
public enum JHVDirectory {
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
            return HOME.getPath() + "Cache" + File.separator;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The shared library directory. */
    LIBS {
        public String getPath() {
            return HOME.getPath() + "Libs" + File.separator;
        }

        public File getFile() {
            return new File(getPath());
        }
    },
    /** The shared library directory. */
    LIBS_LAST_CONFIG {
        public String getPath() {
            return HOME.getPath() + "Libs" + File.separator + "LastConfig" + File.separator;
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
    /** The log directory. */
    LOGS {
        public String getPath() {
            return HOME.getPath() + "Logs" + File.separator;
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
    },
    /** Directory of automatically loaded GIMP gradient files. */
    COLOR_PLUGINS {
        public String getPath() {
            return HOME.getPath() + "Colortables" + File.separator;
        }

        public File getFile() {
            return new File(getPath());
        }
    };
    
    /** A String representation of the path of the directory. */
    abstract public String getPath();

    /** A File representation of the path of the directory. */
    abstract public File getFile();

};