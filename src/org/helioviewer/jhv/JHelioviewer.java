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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.TimeZone;

import javax.media.opengl.DebugGL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.helioviewer.jhv.base.FileUtils;
import org.helioviewer.jhv.base.Log;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.components.layerTable.LayerTableOverlapWatcher;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.internal_plugins.InternalFilterPlugin;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.plugins.hekplugin.HEKPlugin3D;
import org.helioviewer.jhv.plugins.pfssplugin.PfssPlugin;
import org.helioviewer.jhv.plugins.sdocutoutplugin.SDOCutOutPlugin3D;
import org.helioviewer.jhv.plugins.viewmodelplugin.controller.PluginManager;
import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;

import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.UpdateScheduleRegistry;

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

        // Uncaught runtime errors are displayed in a dialog box in addition
        JHVUncaughtExceptionHandler.setupHandlerForThread();
        
        try
        {
        	Log.redirectStdOutErr();
        	if (System.getProperty("raygunTag") != null){
            	if (UpdateScheduleRegistry.checkAndReset()) {
            	    // This will return immediately if you call it from the EDT,
            	    // otherwise it will block until the installer application exits
            	    ApplicationLauncher.launchApplicationInProcess("366", null, new ApplicationLauncher.Callback() {
            	            public void exited(int exitValue) {
            	                //TODO add your code here (not invoked on event dispatch thread)
            	            }
            	            
            	            public void prepareShutdown() {
            	                //TODO add your code here (not invoked on event dispatch thread)
            	            }
            	        }, ApplicationLauncher.WindowMode.FRAME, null
            	    );
            	}
            }
        	JHVGlobals.RAYGUN_TAG = System.getProperty("raygunTag");
            
            // Setup Swing
            try
            {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch(Exception e2)
            {
            }
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            
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
    
            // Information log message
            String argString = "";
            for (int i = 0; i < args.length; ++i) {
                argString += " " + args[i];
            }
            
    		GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.getDefault());
    		GLProfile profile = GLProfile.get(GLProfile.GL2);
    		profile = GLProfile.getDefault();
    		GLCapabilities capabilities = new GLCapabilities(profile);
            final boolean createNewDevice = true;
            final GLAutoDrawable sharedDrawable = factory.createDummyAutoDrawable(null, createNewDevice, capabilities, null);
        	sharedDrawable.display();
    		if (System.getProperty("jhvVersion") == null)
    		    sharedDrawable.setGL(new DebugGL2(sharedDrawable.getGL().getGL2()));

        	OpenGLHelper.glContext = sharedDrawable.getContext();
        
        	System.out.println("JHelioviewer started with command-line options:" + argString);
            System.out.println("Initializing JHelioviewer");
            
            // display the splash screen
            SplashScreen splash = SplashScreen.getSingletonInstance();
    
            int numProgressSteps = 10;
            splash.setProgressSteps(numProgressSteps);
            
            JHVGlobals.initFileChooserAsync();
    
            // Load settings from file but do not apply them yet
            // The settings must not be applied before the kakadu engine has been
            // initialized
            splash.setProgressText("Loading settings...");
            splash.nextStep();
            System.out.println("Load settings");
            Settings.load();
            
            // Set the platform system propertiess
            splash.nextStep();
    
            /* ----------Setup kakadu ----------- */
            System.out.println("Instantiate Kakadu engine");
            KakaduEngine engine = new KakaduEngine();
    
            splash.nextStep();
            splash.setProgressText("Initializing Kakadu libraries...");
    		loadLibraries();
    
            // The following code-block attempts to start the native message
            // handling
            splash.nextStep();
            try {
                System.out.println("Setup Kakadu message handlers.");
                engine.startKduMessageSystem();
            } catch (JHV_KduException e) {
                System.err.println("Failed to setup Kakadu message handlers.");
                e.printStackTrace();
                Message.err("Error starting Kakadu message handler", e.getMessage(), true);
                return;
            }
    
            /* ----------Setup OpenGL ----------- */
            splash.setProgressText("Setting up the UI...");
            splash.nextStep();
            ImageViewerGui.getMainFrame();
    
            System.out.println("Installing overlap watcher");
            LayerTableOverlapWatcher overlapWatcher = new LayerTableOverlapWatcher();
            LayersModel.getSingletonInstance().addLayersListener(overlapWatcher);
    
            /* ----------Setup Plug-ins ----------- */
    
            splash.setProgressText("Loading plugins...");
            splash.nextStep();
    
            // Load Plug ins at the very last point
            System.out.println("Load plugin settings");
            //PluginManager.getSingeltonInstance().loadSettings(JHVDirectorie.HOME.getPath());
    
            System.out.println("Add internal plugin: " + "FilterPlugin");
            Plugin internalPlugin = new InternalFilterPlugin();
            PluginManager.getSingeltonInstance().addInternalPlugin(internalPlugin.getClass().getClassLoader(), internalPlugin);
    
            for(Plugin plugin:new Plugin[]{new PfssPlugin() , new HEKPlugin3D(), new SDOCutOutPlugin3D()})
                PluginManager.getSingeltonInstance().addPlugin(plugin.getClass().getClassLoader(), plugin, null);
    
            splash.setProgressText("Showing main window...");
            splash.nextStep();
            // Create main view chain and display main window
            System.out.println("Start main window");
            //splash.initializeViewchain();
            ImageViewerGui.getSingletonInstance().createViewchains();
            
            
            UILatencyWatchdog.startWatchdog();
        }
        catch(Throwable _t)
        {
            JHVUncaughtExceptionHandler.getSingletonInstance().uncaughtException(Thread.currentThread(),_t);
        }
    }

	private static void loadLibraries() {

		String os = System.getProperty("os.name");
		String arch = System.getProperty("os.arch");
		Path tmpLibDir;
		System.out.println("appDir : " + System.getProperty("appDir"));
		System.out.println("user.dir : " + System.getProperty("user.dir"));
		if(System.getProperty("raygunTag") != null){
			String s = System.getProperty("appDir");
			s = s.substring(0, s.lastIndexOf(File.separator));
			System.setProperty("user.dir", s);
		}
		else {
			System.setProperty("user.dir", "/Users/binchu/Documents/FHNW/JHelioviewer/lib/native");
		}
		String nativeLibraryPath = System.getProperty("user.dir") + File.separator + "lib" + File.separator + "native" + File.separator;
		System.out.println("user.dir : " + System.getProperty("user.dir"));
		System.out.println("user.home: " + System.getProperty("user.home"));
		System.out.println("java.c.p : " + System.getProperty("java.class.path"));
		try {
			tmpLibDir = Files.createTempDirectory("jhv-libs");
			tmpLibDir.toFile().deleteOnExit();

			String directory = "/libs/";
			if (os != null && arch != null) {
				os = os.toLowerCase();
				arch = arch.toLowerCase();
				if (os.indexOf("windows") != -1){
				    System.loadLibrary("msvcr120");
				}
				if (os.indexOf("mac os x") == -1){
					System.loadLibrary("kdu_v75R");
					System.loadLibrary("kdu_a75R");					
				}
				System.loadLibrary("kdu_jni");

				if (os.indexOf("windows") != -1) {
					directory += "windows/";
					if (arch.indexOf("64") != -1) {
						directory += "64/";
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-windows-x86-64.exe", "cgc");
					} else {
						System.err.println(">> Platform > Could not determine platform. OS: "
                        + os + " - arch: " + arch);
					}

				} else if (os.indexOf("linux") != -1) {
					directory += "linux/";
					if (arch.indexOf("64") != -1) {
						directory += "64/";
						loadExecuteLibary(tmpLibDir, directory,
								"cgc-linux-x86-64", "cgc");
					} else {
						System.err.println(">> Platform > Could not determine platform. OS: "
                        + os + " - arch: " + arch);
					}
				} else if (os.indexOf("mac os x") != -1) {
					directory += "mac/";
					loadExecuteLibary(tmpLibDir, directory, "cgc-mac", "cgc");
					setupOSXApplicationListener();
				} else {
					System.err.println(">> Platform > Could not determine platform. OS: "
                    + os + " - arch: " + arch);
				}
			} else {
				System.err.println(">> Platform > Could not determine platform. OS: "
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
			System.err.println("Failed to load native menuitems of Mac OSX");
		}
	}
}
