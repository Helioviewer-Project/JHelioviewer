package org.helioviewer.jhv;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.TimeZone;

import javafx.embed.swing.JFXPanel;

import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message_formatter;

import org.helioviewer.jhv.base.Log;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.plugins.plugin.Plugins;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.JHV_Kdu_message;

import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.UpdateScheduleRegistry;
import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

public class JHelioviewer
{
	public static void main(String[] args)
	{
		try {
			Class.forName("com.sun.javafx.runtime.VersionInfo");
			JHVGlobals.USE_JAVA_FX = true;
		} catch (ClassNotFoundException e) {
			System.out.println("No JavaFX detected. Please install a Java 1.8 with JavaFX");
			//JOptionPane.showMessageDialog(null, "No JavaFX detected. Please install a Java 1.8 with JavaFX", "No JavaFX detected", JOptionPane.ERROR_MESSAGE);
			//System.exit(0);
		}
		
		SwingUtilities.invokeLater(new Runnable()
		{
		    public void run() {
		        new JFXPanel(); // initializes JavaFX environment
		    }
		});
		
		// Uncaught runtime errors are displayed in a dialog box in addition
		JHVUncaughtExceptionHandler.setupHandlerForThread();

		try
		{
			Log.redirectStdOutErr();
			if (System.getProperty("raygunTag") != null)
			{
				if (UpdateScheduleRegistry.checkAndReset())
				{
					// This will return immediately if you call it from the EDT,
					// otherwise it will block until the installer application
					// exits
					ApplicationLauncher.launchApplicationInProcess("366", null,
							new ApplicationLauncher.Callback()
							{
								public void exited(int exitValue)
								{
									// add your code here (not invoked on event dispatch thread)
								}

								public void prepareShutdown()
								{
									// add your code here (not invoked on event dispatch thread)
								}
							}, ApplicationLauncher.WindowMode.FRAME, null);
				}
			}
			JHVGlobals.RAYGUN_TAG = System.getProperty("raygunTag");

			if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help")))
			{
				System.out.println(CommandLineProcessor.getUsageMessage());
				return;
			}

			CommandLineProcessor.setArguments(args);

			// Setup Swing
			try
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			catch (Exception e2)
			{
				e2.printStackTrace();
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
			for (int i = 0; i < args.length; ++i)
				argString += " " + args[i];
			
			GLProfile.initSingleton();
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
			final SplashScreen splash = SplashScreen.getSingletonInstance();

			splash.setProgressSteps(4);

			JHVGlobals.initFileChooserAsync();

			// Load settings from file but do not apply them yet
			// The settings must not be applied before the kakadu engine has
			// been
			// initialized
			splash.progressTo("Loading settings...");
			Settings.load();

			/* ----------Setup kakadu ----------- */
			splash.progressTo("Initializing Kakadu libraries...");

			try
			{
				loadLibraries();
			}
			catch (UnsatisfiedLinkError _ule)
			{
				if (JHVGlobals.isLinux() && _ule.getMessage().contains("GLIBC"))
				{
					splash.setVisible(false);
					JOptionPane.showMessageDialog(null,
									"JHelioviewer requires a more recent version of GLIBC. Please update your distribution.\n\n"
									+ _ule.getMessage(),
									"JHelioviewer", JOptionPane.ERROR_MESSAGE);
					return;
				}

				throw _ule;
			}

			// The following code-block attempts to start the native message handling
			splash.progressTo("Setup Kakadu message handlers");
            Kdu_global.Kdu_customize_warnings(new Kdu_message_formatter(new JHV_Kdu_message(false), 80));
            Kdu_global.Kdu_customize_errors(new Kdu_message_formatter(new JHV_Kdu_message(true), 80));

			// Create main view chain and display main window
            splash.progressTo("Start Swing");
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					splash.progressTo("Initialize plugins");
					
					// force initialization of UltimatePluginInterface
					Plugins.SINGLETON.getClass();
					
					
					splash.progressTo("Open main window");

					MainFrame.SINGLETON.setVisible(true);
					splash.dispose();
					UILatencyWatchdog.startWatchdog();
				}
			});
		}
		catch (Throwable _t)
		{
			JHVUncaughtExceptionHandler.getSingletonInstance().uncaughtException(Thread.currentThread(), _t);
		}
	}

	private static void loadLibraries()
	{
		try
		{
			Path tmpLibDir = Files.createTempDirectory("jhv-libs");
			tmpLibDir.toFile().deleteOnExit();

			if (JHVGlobals.isWindows())
			{
				System.loadLibrary("msvcr120");
				System.loadLibrary("kdu_v75R");
				System.loadLibrary("kdu_a75R");
			}

			System.loadLibrary("kdu_jni");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	private static void setupOSXApplicationListener()
	{
		final AboutDialog aboutDialog = new AboutDialog();
		try
		{
			Class<?> applicationClass = Class.forName("com.apple.eawt.Application");
			Object application = applicationClass.newInstance();
			Class<?> applicationListener = Class.forName("com.apple.eawt.ApplicationListener");
			Object listenerProxy = Proxy.newProxyInstance(
					applicationListener.getClassLoader(),
					new Class[] { applicationListener },
					new InvocationHandler()
					{
						@Override
						public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
						{
							if ("handleAbout".equals(method.getName()))
							{
								aboutDialog.showDialog();
								setHandled(args[0], Boolean.TRUE);
							}
							else if ("handleQuit".equals(method.getName()))
							{
								System.exit(0);
							}
							
							return null;
						}

						private void setHandled(Object event, Boolean val) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
						{
							Method handleMethod = event.getClass().getMethod("setHandled", new Class[] { boolean.class });
							handleMethod.invoke(event, new Object[] { val });
						}
					});
			
			Method registerListenerMethod = applicationClass.getMethod("addApplicationListener", new Class[] { applicationListener });
			registerListenerMethod.invoke(application, new Object[] { listenerProxy });
		}
		catch (Exception e)
		{
			System.err.println("Failed to create native menuitems for Mac OSX");
		}
	}
}
