package org.helioviewer.jhv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.UIManager;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.logging.LogSettings;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.layerTable.LayerTableOverlapWatcher;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
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
public class JHelioviewer {

    public static void main(String[] args) {
        // Prints the usage message
        if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
            System.out.println(CommandLineProcessor.getUsageMessage());
            return;
        }
        
        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();

        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e2)
        {
        }
        
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
        try
        {
            LogSettings.init("/settings/log4j.initial.properties", Directories.LOGS.getPath(), CommandLineProcessor.isOptionSet("--use-existing-log-time-stamp"));
        }
        catch(IOException _ioe)
        {
            _ioe.
            printStackTrace();
        }
        

        // Information log message
        String argString = "";
        for (int i = 0; i < args.length; ++i) {
            argString += " " + args[i];
        }
        Log.info("JHelioviewer started with command-line options:" + argString);

        // This attempts to create the necessary directories for the application
        Log.info("Create directories...");
        Directories.createDirs();

        // Save the log settings. Must be done AFTER the directories are created
        LogSettings.update();

        Log.info("Initializing JHelioviewer");
        // display the splash screen
        SplashScreen splash = SplashScreen.getSingletonInstance();

        int numProgressSteps = 10;
        splash.setProgressSteps(numProgressSteps);

        // Load settings from file but do not apply them yet
        // The settings must not be applied before the kakadu engine has been
        // initialized
        splash.setProgressText("Loading settings...");
        splash.nextStep();
        Log.info("Load settings");
        Settings.load();
        
        // Set the platform system properties
        splash.nextStep();

        /* ----------Setup kakadu ----------- */
        Log.debug("Instantiate Kakadu engine");
        KakaduEngine engine = new KakaduEngine();

        splash.nextStep();
        splash.setProgressText("Initializing Kakadu libraries...");
		loadLibraries();

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
        Log.info("Use cache directory: " + Directories.CACHE.getPath());
        JP2Image.setCachePath(Directories.CACHE.getFile());

        Log.info("Update settings");
        Settings.apply();

        /* ----------Setup OpenGL ----------- */
        splash.nextStep();

        // Check for updates in parallel, if newer version is available a small
        // message is displayed
        try {
            UpdateChecker update = new UpdateChecker();
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

        // Load Plug ins at the very last point
        Log.info("Load plugin settings");
        PluginManager.getSingeltonInstance().loadSettings(Directories.HOME.getPath());

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

	private static void loadLibraries() {

		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		Path tmpLibDir;
		try {
			tmpLibDir = Files.createTempDirectory("jhv-libs");
			tmpLibDir.toFile().deleteOnExit();

			String directory = "/libs/";
			if (os != null && arch != null) {
				os = os.toLowerCase();
				arch = arch.toLowerCase();
				if (os.indexOf("windows") != -1) {
					directory += "windows/";
					if (arch.indexOf("64") != -1) {
						directory += "64/";
						//loadJNILibary(tmpLibDir, directory, "msvcr100.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_v63R.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_a63R.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_jni.dll");
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-windows-x86-64.exe", "cgc");
					} else if (arch.indexOf("86") != -1) {
						directory += "32/";
						//loadJNILibary(tmpLibDir, directory, "msvcr100.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_v63R.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_a63R.dll");
						loadJNILibary(tmpLibDir, directory, "kdu_jni.dll");
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-windows-x86-32.exe", "cgc");
					} else {
						Log.error(">> Platform > Could not determine platform. OS: "
								+ os + " - arch: " + arch);
					}

				} else if (os.indexOf("linux") != -1) {
					directory += "linux/";
					if (arch.indexOf("64") != -1) {
						directory += "64/";
						loadJNILibary(tmpLibDir, directory,
								"libkdu_jni-linux-x86-64-glibc-2-7.so");
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-linux-x86-64", "cgc");
					} else if (arch.indexOf("86") != -1) {
						directory += "32/";
						loadJNILibary(tmpLibDir, directory,
								"libkdu_jni-linux-x86-32-glibc-2-7.so");
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-linux-x86-32", "cgc");
					} else {
						Log.error(">> Platform > Could not determine platform. OS: "
								+ os + " - arch: " + arch);
					}
				} else if (os.indexOf("mac os x") != -1) {
					directory += "mac/";
					loadJNILibary(tmpLibDir, directory,
							"libkdu_jni-mac-x86-64.jnilib");
					loadExecuteLibary(tmpLibDir, directory, "cgc-mac", "cgc");
					setupOSXApplicationListener();
				} else {
					Log.error(">> Platform > Could not determine platform. OS: "
							+ os + " - arch: " + arch);
				}
			} else {
				Log.error(">> Platform > Could not determine platform. OS: "
						+ os + " - arch: " + arch);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void loadJNILibary(Path tmpPath, String directory,
			String name) {
		InputStream in = JHelioviewer.class.getResourceAsStream(directory
				+ name);
		byte[] buffer = new byte[1024];
		int read = -1;
		File tmp = new File(tmpPath.toFile(), name);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(tmp);
			while ((read = in.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			fos.close();
			in.close();

			System.load(tmp.getAbsolutePath());
			tmp.deleteOnExit();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void loadExecuteLibary(Path tmpPath, String directory,
			String name, String executableName) {
		InputStream in = JHelioviewer.class.getResourceAsStream(directory
				+ name);
		byte[] buffer = new byte[1024];
		int read = -1;
		File tmp = new File(tmpPath.toFile(), name);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(tmp);
			while ((read = in.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			fos.close();
			in.close();
			tmp.setExecutable(true);
			FileUtils.registerExecutable(executableName, tmp.getAbsolutePath());
			tmp.deleteOnExit();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private static void setupOSXApplicationListener(){
		final AboutDialog aboutDialog = new AboutDialog();
		try {
			Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
			Object application = applicationClass.newInstance();
			Class<?> applicationListener = Class.forName("com.apple.eawt.ApplicationListener");
			Object listenerProxy = Proxy.newProxyInstance(applicationListener.getClassLoader(), new Class[] {applicationListener}, new InvocationHandler() {
				
				@Override
				public Object invoke(Object proxy, Method method, Object[] args)
						throws Throwable {
					if("handleAbout".equals(method.getName())){
						aboutDialog.showDialog();
						setHandled(args[0], Boolean.TRUE);
					}
					// TODO Auto-generated method stub
					return null;
				}
				private void setHandled(Object event, Boolean val) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
					                      Method handleMethod =   event.getClass().getMethod("setHandled", new Class[] {boolean.class});
					                      handleMethod.invoke(event, new Object[] {val});
					}
			});
			Method registerListenerMethod = applicationClass.getMethod("addApplicationListener", new Class[] {applicationListener});
			registerListenerMethod.invoke(application, new Object[] {listenerProxy});
					
		} catch (Exception e) {
			Log.error("Failed to load native menuitems of Mac OSX");
		}
	}
}
