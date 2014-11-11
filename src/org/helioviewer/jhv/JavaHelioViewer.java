package org.helioviewer.jhv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import javax.swing.UIManager;

import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.logging.LogSettings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.layerTable.LayerTableOverlapWatcher;
import org.helioviewer.jhv.internal_plugins.InternalFilterPlugin;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.plugins.hekplugin.HEKPlugin3D;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.sdocutoutplugin.SDOCutOutPlugin3D;
import org.helioviewer.jhv.plugins.viewmodelplugin.controller.PluginManager;
import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;

/**
 * This class starts the applications.
 * 
 * @author caplins
 * @author Benjamin Wamsler
 * @author Markus Langenberg
 * @author Stephan Pagel
 * @author Andre Dau
 * @author Helge Dietert
 * 
 */
public class JavaHelioViewer {

    public static void main(String[] args) {
        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLineProcessor.getUsageMessage());
            return;
        }
        
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e2)
        {
        }
        
        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();

        // Save command line arguments
        CommandLineProcessor.setArguments(args);

        // Save current default system timezone in user.timezone
        System.setProperty("user.timezone", TimeZone.getDefault().getID());

        // Per default all times should be given in GMT
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

        // Save current default locale to user.locale
        System.setProperty("user.locale", Locale.getDefault().toString());

        // Per default, the us locale should be used
        Locale.setDefault(Locale.US);

        // init log
        LogSettings.init("/settings/log4j.initial.properties", JHVDirectory.SETTINGS.getPath() + "log4j.properties", JHVDirectory.LOGS.getPath(), CommandLineProcessor.isOptionSet("--use-existing-log-time-stamp"));

        // Information log message
        String argString = "";
        for (int i = 0; i < args.length; ++i) {
            argString += " " + args[i];
        }
        Log.info("JHelioviewer started with command-line options:" + argString);

        // This attempts to create the necessary directories for the application
        Log.info("Create directories...");
        JHVGlobals.createDirs();

        // Save the log settings. Must be done AFTER the directories are created
        LogSettings.update();

        Log.info("Initializing JHelioviewer");
        // display the splash screen
        Log.debug("Create splash screen");
        JHVSplashScreen splash = JHVSplashScreen.getSingletonInstance();

        int numProgressSteps = 10;
        Log.debug("Number of progress steps: " + numProgressSteps);
        splash.setProgressSteps(numProgressSteps);

        splash.setProgressText("Initializing JHelioviewer...");

        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been
        // initialized
        splash.setProgressText("Loading settings...");
        splash.nextStep();
        Log.info("Load settings");
        Settings.load();
        
        // Set the platform system properties
        splash.nextStep();
        splash.setProgressText("Determining platform...");

        setPlatform();
        Log.info("OS: " + System.getProperty("jhv.os") + " - arch: " + System.getProperty("jhv.arch") + " - java arch: " + System.getProperty("jhv.java.arch"));

        // Remove about menu on mac
        if (System.getProperty("jhv.os").equals("mac")) {
            try {
                Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
                Method getSingletonApplication = applicationClass.getMethod("getApplication", (Class<?>[]) null);
                Object application = getSingletonApplication.invoke(applicationClass.newInstance());
                Method removeAboutMenuItem = applicationClass.getMethod("removeAboutMenuItem", (Class<?>[]) null);
                removeAboutMenuItem.invoke(application);
            } catch (Exception e) {
                Log.warn(">> JavaHelioViewer.main(String[]) > Failed to disable native Mac OS about menu. Probably not running on Mac OS", e);
            }
        }

        // Directories where to search for lib config files
        URI libs = JHVDirectory.LIBS.getFile().toURI();
        URI libsBackup = JHVDirectory.LIBS_LAST_CONFIG.getFile().toURI();
        URI libsRemote = null;
        try {
            libsRemote = new URI(Settings.getProperty("default.remote.lib.path"));
        } catch (URISyntaxException e1) {
            Log.error("Invalid uri for remote library server");
        }

        /* ----------Setup kakadu ----------- */
        Log.debug("Instantiate Kakadu engine");
        KakaduEngine engine = new KakaduEngine();

        splash.nextStep();
        splash.setProgressText("Initializing Kakadu libraries...");
        Log.info("Try to load Kakadu libraries");
        if (null == ResourceLoader.getSingletonInstance().loadResource("kakadu", libsRemote, libs, libs, libsBackup, System.getProperties())) {
            Log.fatal("Could not load Kakadu libraries");
            Message.err("Error loading Kakadu libraries", "Fatal error! The kakadu libraries could not be loaded. The log output may contain additional information.", true);
            return;
        } else {
            Log.info("Successfully loaded Kakadu libraries");
        }

        // The following code-block attempts to start the native message
        // handling
        splash.nextStep();
        try {
            Log.debug("Setup Kakadu message handlers.");
            engine.startKduMessageSystem();
        } catch (JHV_KduException e) {
            Log.fatal("Failed to setup Kakadu message handlers.", e);
            Message.err("Error starting Kakadu message handler", e.getMessage(), true);
            return;
        }

        // Apply settings after kakadu engine has been initialized
        Log.info("Use cache directory: " + JHVDirectory.CACHE.getPath());
        JP2Image.setCachePath(JHVDirectory.CACHE.getFile());

        Log.info("Update settings");
        Settings.apply();

        /* ----------Setup OpenGL ----------- */

        splash.nextStep();
        splash.setProgressText("Loading OpenGL libraries...");

        Log.info("Try to install CG Compiler");
        if (null == ResourceLoader.getSingletonInstance().loadResource("cgc", libsRemote, libs, libs, libsBackup, System.getProperties())) {
            Log.error("Could not install CG Compiler");
            Message.err("Error installing CG Compiler", "The CG Compiler could not be installed. JHelioviewer will run in software mode.", false);
        } else {
            Log.info("Successfully installed CG Compiler");
        }

        splash.nextStep();

        // Check for updates in parallel, if newer version is available a small
        // message is displayed
        try {
            JHVUpdate update = new JHVUpdate();
            update.check();
        } catch (MalformedURLException e) {
            // Should never happen
            Log.error("Error retrieving internal update URL", e);
        }

        Log.debug("Installing Overlap Watcher");
        LayerTableOverlapWatcher overlapWatcher = new LayerTableOverlapWatcher();
        LayersModel.getSingletonInstance().addLayersListener(overlapWatcher);

        /* ----------Setup Plug-ins ----------- */

        splash.setProgressText("Loading Plugins...");
        splash.nextStep();

        // check if plug-ins have to be deleted
        final File tmpFile = new File(JHVDirectory.PLUGINS.getPath() + JHVGlobals.TEMP_FILENAME_DELETE_PLUGIN_FILES);

        if (tmpFile.exists()) {
            try {
                final BufferedReader in = new BufferedReader(new FileReader(tmpFile));

                String line = null;
                String content = "";

                while ((line = in.readLine()) != null) {
                    content += line;
                }

                in.close();

                final StringTokenizer st = new StringTokenizer(content, ";");

                while (st.hasMoreElements()) {
                    final File delFile = new File(st.nextToken());
                    delFile.delete();
                }

                tmpFile.delete();
            } catch (final Exception e) {
            }
        }

        // Load Plug ins at the very last point
        Log.info("Load plugin settings");
        PluginManager.getSingeltonInstance().loadSettings(JHVDirectory.PLUGINS.getPath());

        Log.info("Add internal plugin: " + "FilterPlugin");
        Plugin internalPlugin = new InternalFilterPlugin();
        PluginManager.getSingeltonInstance().addInternalPlugin(internalPlugin.getClass().getClassLoader(), internalPlugin);

        for(Plugin plugin:new Plugin[]{new PfssPlugin() , new HEKPlugin3D(), new SDOCutOutPlugin3D()})
            PluginManager.getSingeltonInstance().addPlugin(plugin.getClass().getClassLoader(), plugin, null);

        splash.setProgressText("Displaying main window...");
        splash.nextStep();
        // Create main view chain and display main window
        Log.info("Start main window");
        //splash.initializeViewchain();
        ImageViewerGui.getSingletonInstance().createViewchains();

    }

    /**
     * Reads the builtin Java properties to determine the platform and set
     * simplified properties used by JHelioviewer.
     */
    private static void setPlatform()
    {
            String os = System.getProperty("os.name");
            String arch = System.getProperty("os.arch");
            String javaArch = System.getProperty("sun.arch.data.model");

            System.setProperty("jhv.java.arch", javaArch);

            if (os != null && arch != null) {
                os = os.toLowerCase();
                arch = arch.toLowerCase();
                if (os.indexOf("windows") != -1) {
                    System.setProperty("jhv.os", "windows");
                    if (arch.indexOf("64") != -1)
                        System.setProperty("jhv.arch", "x86-64");
                    else if (arch.indexOf("86") != -1)
                        System.setProperty("jhv.arch", "x86-32");
                    else {
                        Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                    }
                } else if (os.indexOf("linux") != -1) {
                    System.setProperty("jhv.os", "linux");
                    if (arch.indexOf("64") != -1)
                        System.setProperty("jhv.arch", "x86-64");
                    else if (arch.indexOf("86") != -1)
                        System.setProperty("jhv.arch", "x86-32");
                    else {
                        Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                    }
                } else if (os.indexOf("mac os x") != -1) {
                    System.setProperty("jhv.os", "mac");
                    if (arch.indexOf("ppc") != -1)
                        System.setProperty("jhv.arch", "ppc");
                    else if (arch.indexOf("64") != -1)
                        System.setProperty("jhv.arch", "x86-64");
                    else if (arch.indexOf("86") != -1)
                        System.setProperty("jhv.arch", "x86-32");
                    else {
                        Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                    }
                } else {
                    Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
                }
            } else {
                Log.error(">> Platform > Could not determine platform. OS: " + os + " - arch: " + arch);
            }
    }
}
