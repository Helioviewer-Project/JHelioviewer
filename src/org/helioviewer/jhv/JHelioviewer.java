package org.helioviewer.jhv;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.JHVUncaughtExceptionHandler;
import org.helioviewer.jhv.base.Log;
import org.helioviewer.jhv.base.Observatories;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.SplashScreen;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.UILatencyWatchdog;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KduErrorHandler;

import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.UpdateSchedule;
import com.install4j.api.update.UpdateScheduleRegistry;
import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message_formatter;

public class JHelioviewer
{
	public static void main(final String[] args)
	{
		CommandLineProcessor.setArguments(args);
		
		// Setup Swing
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e2)
		{
			Telemetry.trackException(e2);
		}
		
		// display the splash screen
		final SplashScreen splash = new SplashScreen(19);
		
		splash.progressTo("Installing crash monitoring");
		
		// Uncaught runtime errors are displayed in a dialog box in addition
		JHVUncaughtExceptionHandler.setupHandlerForThread();
		
		try
		{
			splash.progressTo("Redirecting standard streams");
			Log.redirectStdOutErr();
			
			splash.progressTo("Checking for updates");
			if (Globals.isReleaseVersion())
			{
				UpdateScheduleRegistry.setUpdateSchedule(UpdateSchedule.DAILY);
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
								}

								public void prepareShutdown()
								{
								}
							}, ApplicationLauncher.WindowMode.FRAME, null);
				}
			}

			if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help")))
			{
				System.out.println(CommandLineProcessor.USAGE_MESSAGE);
				return;
			}

			//start app insights on a separate thread, because it usually
			//takes a while to load
			splash.progressTo("Starting Application Insights");
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					Telemetry.trackEvent("Startup","args",Arrays.toString(args));
				}
			}).start();
			
			ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);

			// initializes JavaFX environment
			splash.progressTo("Initializing JavaFX");
			if(Globals.JAVA_FX_AVAILABLE)
				Platform.runLater(new Runnable()
				{
				    public void run()
				    {
				        new JFXPanel();
				    }
				});
			
			splash.progressTo("Installing universal locale");
			System.setProperty("user.timezone", TimeZone.getDefault().getID());
			TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
			System.setProperty("user.locale", Locale.getDefault().toString());
			Locale.setDefault(Locale.US);
			
			splash.progressTo("Initializing OpenGL");
			GLProfile.initSingleton();
			GLDrawableFactory factory = GLDrawableFactory.getFactory(GLProfile.getDefault());
			GLProfile profile = GLProfile.get(GLProfile.GL2);
			
			splash.progressTo("Creating drawable");
			GLCapabilities capabilities = new GLCapabilities(profile);
			final GLAutoDrawable sharedDrawable = factory.createDummyAutoDrawable(null, true, capabilities, null);
			sharedDrawable.display();
			
			if (System.getProperty("jhvVersion") == null)
				sharedDrawable.setGL(new DebugGL2(sharedDrawable.getGL().getGL2()));
			
			System.out.println("JHelioviewer started with command-line options:" + String.join(" ", args));
			System.out.println("Initializing JHelioviewer");

			// Load settings from file but do not apply them yet
			// The settings must not be applied before the kakadu engine has
			// been initialized
			splash.progressTo("Loading settings");
			Settings.load();

			splash.progressTo("Initializing Kakadu");
			try
			{
				loadLibraries();
			}
			catch (UnsatisfiedLinkError _ule)
			{
				if (Globals.isLinux() && _ule.getMessage().contains("GLIBC"))
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

			// The following code-block attempts to start the native message handling, otherwise
			// KDU just terminates our process when something goes wrong... (!?!)
			splash.progressTo("Setting up Kakadu message handlers");
            Kdu_global.Kdu_customize_warnings(new Kdu_message_formatter(new KduErrorHandler(false), 80));
            Kdu_global.Kdu_customize_errors(new Kdu_message_formatter(new KduErrorHandler(true), 80));

            final long startTime=System.currentTimeMillis();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
            {
				@Override
				public void run()
				{
					//TODO: move to action instead (more stable environment & add collection of ui state)
					Telemetry.trackMetric("Session duration", (System.currentTimeMillis()-startTime)/1000);
				}
			}));
            
			// Create main view chain and display main window
            splash.progressTo("Starting Swing");
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					sharedDrawable.getContext().makeCurrent();
					
					splash.progressTo("Creating OpenGL context");
					MainFrame.SINGLETON.getClass();
					
					splash.progressTo("Setting up texture cache");
					//TextureCache.init();
					
					splash.progressTo("Compiling shaders");
					AbstractImageLayer.init();
					
					// force initialization of UltimatePluginInterface
					splash.progressTo("Initializing plugins");
					Plugins.SINGLETON.getClass();
					
					splash.progressTo("Opening main window");
					MainFrame.SINGLETON.setVisible(true);
					
		            splash.progressTo("Loading observatories");
		            Observatories.getObservatories();
		            
		            splash.progressTo("");
					new Thread(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								Thread.sleep(1000);
							}
							catch (InterruptedException _e)
							{
								Telemetry.trackException(_e);
							}
							SwingUtilities.invokeLater(new Runnable()
							{
								@Override
								public void run()
								{
									splash.dispose();
									UILatencyWatchdog.startWatchdog();
								}
							});
						}
					}).start();
				}
			});
		}
		catch (Throwable _t)
		{
			JHVUncaughtExceptionHandler.SINGLETON.uncaughtException(Thread.currentThread(), _t);
		}
	}

	private static void loadLibraries()
	{
		try
		{
			Path tmpLibDir = Files.createTempDirectory("jhv-libs");
			tmpLibDir.toFile().deleteOnExit();

			if (Globals.isWindows())
			{
				try
				{
					System.loadLibrary("msvcr120");
				}
				catch(UnsatisfiedLinkError _ule)
				{
					//ignore inability to load msvcr120. if there's really
					//a problem, it will be caught by the outer try/catch
				}
				System.loadLibrary("kdu_v75R");
				System.loadLibrary("kdu_a75R");
			}

			System.loadLibrary("kdu_jni");
		}
		catch (IOException e)
		{
			Telemetry.trackException(e);
		}
	}

	@SuppressWarnings("unused")
	private static void setupOSXApplicationListener()
	{
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
						@SuppressWarnings("null")
						@Override
						public Object invoke(@Nullable Object proxy, @Nullable Method method, @Nullable Object[] args) throws Throwable
						{
							if ("handleAbout".equals(method.getName()))
							{
								SwingUtilities.invokeLater(new Runnable()
								{
									@Override
									public void run()
									{
										new AboutDialog();
									}
								});
								
								if(args!=null && args[0]!=null)
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
		catch (Throwable t)
		{
			System.err.println("Failed to create native menuitems for Mac OSX");
			Telemetry.trackException(t);
		}
	}
}
