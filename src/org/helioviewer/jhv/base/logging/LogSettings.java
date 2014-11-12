package org.helioviewer.jhv.base.logging;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.helioviewer.jhv.base.FileUtils;

/**
 * Class which manages the loading and saving of the log settings. This class
 * uses the singleton pattern.
 * 
 * @author Andre Dau
 * 
 */
public class LogSettings {
    public static final Logger LOGGER = Logger.getRootLogger();

    /**
     * Log levels sorted from ALL to OFF
     */
    public static final Level[] LEVELS = { Level.ALL, Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL, Level.OFF };

    /**
     * Identifier for the file appender
     */
    public static final String FILE_LOGGER = "file";

    /**
     * Identifier for the console appender
     */
    public static final String CONSOLE_LOGGER = "console";

    private static Properties defaultSettings;

    private static boolean modified;

    /**
     * Initializes the root logger. Must be called at least once before using
     * the logger.
     * 
     * @param defaultLogSettingsPath
     *            Resource path to the default settings file
     * @param logsDirectory
     *            Directory to which the log files are written
     * @param useExistingTimeStamp
     *            If true, use timestamp from setting file instead of current
     *            time
     */
    public static void init(String defaultLogSettingsPath, String logsDirectory, boolean useExistingTimeStamp) {
        new LogSettings(defaultLogSettingsPath, logsDirectory, useExistingTimeStamp);

    }

    /**
     * Private constructor
     * 
     * @param defaultLogSettingsPath
     *            Path to the default log settings
     * @param logSettingsPath
     *            Path to the custom user log settings
     * @param logsDirectory
     *            Path to the directory where the log files are stored
     * @param useExistingTimeStamp
     *            If true, use timestamp from setting file instead of current
     *            time
     */
    private LogSettings(String defaultLogSettingsPath, String logsDirectory, boolean useExistingTimeStamp) {
        // Use default log4j settings as a basis
        BasicConfigurator.configure();

        // Default settings file
        InputStream defaultSettingsInputStream = FileUtils.getResourceInputStream(defaultLogSettingsPath);

        // Try to open default settings and to read the default values into a
        // Properties object
        defaultSettings = new Properties();
        try {
            if (defaultSettingsInputStream == null) {
            } else {
                defaultSettings.load(defaultSettingsInputStream);
                // Set file path
                defaultSettings.setProperty("log4j.appender." + FILE_LOGGER + ".Directory", logsDirectory);
            }
        } catch (IOException e) {
        }

        // Configure settings
        SimpleDateFormat formatter = new SimpleDateFormat(get("log4j.appender." + FILE_LOGGER + ".Pattern"));
        formatter.setTimeZone(TimeZone.getTimeZone(System.getProperty("user.timezone")));
        if (!useExistingTimeStamp || get("log4j.appender." + FILE_LOGGER + ".TimeStamp") == null) {
            set("log4j.appender." + FILE_LOGGER + ".TimeStamp", formatter.format(new Date()));
        }
        
        applySettings();
    }

    private static void applySettings()
    {
        Properties p=new Properties(defaultSettings);
        try
        {
            for(String key:Preferences.userRoot().node("LogSettings").keys())
                p.setProperty(key,get(key));
        }
        catch(BackingStoreException _bse)
        {
            _bse.printStackTrace();
        }
        PropertyConfigurator.configure(p);
    }
    
    private static String get(String key)
    {
        return Preferences.userRoot().node("LogSettings").get(key,defaultSettings.getProperty(key));
    }
    
    private static void set(String key,String val)
    {
        Preferences.userRoot().node("LogSettings").put(key,val);
    }

    /**
     * Checks if settings were changed and performs an update if necessary
     */
    public static void update() {
        if (!modified)
            return;
        
        Log.debug(">> LogSettings.update() > Log settings modified. Update changes.");
        applySettings();
        
        try
        {
            Preferences.userRoot().node("LogSettings").flush();
            modified = false;
        }
        catch(Exception _e)
        {
            _e.printStackTrace();
        }
    }

    /**
     * Returns the current log level of an appender
     * 
     * @param logger
     *            identifier of the appender
     * @return the log level of the appender
     */
    public static Level getLoggingLevel(String logger)
    {
        String level = get("log4j.appender." + logger + ".threshold");
        if(level==null)
            return null;
        return Level.toLevel(level);
    }

    /**
     * Sets the log level of an appender
     * 
     * @param logger
     *            the identifier of the appender
     * @param level
     *            the new log level
     */
    public static void setLoggingLevel(String logger, Level level) {
        Log.info("Set " + logger + " logging level to " + level);
        set("log4j.appender." + logger + ".threshold", level.toString());

        Level minLevel;
        if (getLoggingLevel(FILE_LOGGER).toInt() < getLoggingLevel(CONSOLE_LOGGER).toInt()) {
            minLevel = getLoggingLevel(FILE_LOGGER);
        } else {
            minLevel = getLoggingLevel(CONSOLE_LOGGER);
        }

        String rootLoggerSetting = get("log4j.rootLogger");
        rootLoggerSetting = minLevel.toString() + rootLoggerSetting.substring(rootLoggerSetting.indexOf(','));
        set("log4j.rootLogger", rootLoggerSetting);
        modified = true;
    }

    /**
     * Returns the default log level of an appender
     * 
     * @param logger
     *            identifier of the appender
     * @return the log level of the appender
     */
    public static Level getDefaultLoggingLevel(String logger) {
        String level = defaultSettings.getProperty("log4j.appender." + logger + ".threshold");
        if(level==null)
            return null;
        return Level.toLevel(level);
    }

    /**
     * Log files older than the maxium age (in days) are deleted when
     * initializing the file logger
     * 
     * @return Number of days to keep log files
     */
    public static int getMaxiumLogFileAge(String logger) {
        String maxAge=get("log4j.appender." + logger + ".Days");
        if(maxAge==null)
            return -1;
        return Integer.parseInt(maxAge);
    }

    /**
     * Log files older than the maxium age (in days) are deleted when
     * initializing the file logger
     * 
     * @return Default number of days to keep log files
     */
    public static int getDefaultMaxiumLogFileAge(String logger) {
        if (defaultSettings == null) {
            return -1;
        }
        return Integer.parseInt(defaultSettings.getProperty("log4j.appender." + logger + ".Days"));
    }

    /**
     * Log files older than the maxium age (in days) are deleted when
     * initializing the file logger
     * 
     * @param days
     *            Number of days to keep log files before thay are deleted
     */
    public static void setMaxiumLogFileAge(String logger, int days) {
        set("log4j.appender." + logger + ".Days", Integer.toString(days));
    }

    /**
     * Returns the name of current log file.
     * 
     * If the name could not be retrieved, returns null.
     * 
     * @return name of the current log file
     */
    public static String getCurrentLogFile() {
        Appender appender = LOGGER.getAppender("file");
        if (appender instanceof FileAppender) {
            return ((FileAppender) appender).getFile();
        }
        return null;
    }
}
