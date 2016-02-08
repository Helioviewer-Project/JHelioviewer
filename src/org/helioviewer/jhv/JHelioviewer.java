package org.helioviewer.jhv;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import org.helioviewer.jhv.base.Settings.BooleanKey;
import org.helioviewer.jhv.base.Settings.IntKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.UILatencyWatchdog;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.SplashScreen;
import org.helioviewer.jhv.gui.actions.ExitProgramAction;
import org.helioviewer.jhv.gui.dialogs.AboutDialog;
import org.helioviewer.jhv.gui.dialogs.AddLayerDialog;
import org.helioviewer.jhv.gui.dialogs.LicenseDialog;
import org.helioviewer.jhv.io.CommandLineProcessor;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.LUT;
import org.helioviewer.jhv.opengl.NoImageScreen;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.plugins.hekplugin.HEKIcon;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KduErrorHandler;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.MovieCache;

import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.UpdateSchedule;
import com.install4j.api.update.UpdateScheduleRegistry;
import com.jogamp.opengl.DebugGL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_message_formatter;

public class JHelioviewer
{
	static SplashScreen splash;
	
	public static void main(final String[] args) throws InvocationTargetException, InterruptedException, KduException
	{
		CommandLineProcessor.setArguments(args);
		
		// Uncaught runtime errors are displayed in a dialog box in addition
		JHVUncaughtExceptionHandler.setupHandlerForThread();
		
		System.out.println("JHelioviewer started with command-line options:" + String.join(" ", args));
		if (args.length == 1 && (args[0].equals("-h") || args[0].equals("--help")))
		{
			System.out.println(CommandLineProcessor.USAGE_MESSAGE);
			return;
		}
		
		Log.redirectStdOutErr();
		
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				// Setup Swing
				try
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}
				catch (Exception e2)
				{
					Telemetry.trackException(e2);
				}
		
				//display EULA, if needed
				if(!Globals.isWindows() && Settings.getInt(IntKey.STARTUP_LICENSE_SHOWN)!=Globals.LICENSE_VERSION)
				{
					LicenseDialog ld=new LicenseDialog();
					
					if(!ld.didAgree())
						System.exit(0);
					
					Settings.setInt(IntKey.STARTUP_LICENSE_SHOWN, Globals.LICENSE_VERSION);
				}
				
				// display the splash screen
				SplashScreen splash = new SplashScreen(17);

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
					
				splash.progressTo("Installing universal locale");
				System.setProperty("user.timezone", TimeZone.getDefault().getID());
				TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
				System.setProperty("user.locale", Locale.getDefault().toString());
				Locale.setDefault(Locale.US);
				
		
				splash.progressTo("Initializing OpenGL");
				GLProfile.initSingleton();
				GLProfile profile = GLProfile.get(GLProfile.GL2);
				GLDrawableFactory factory = GLDrawableFactory.getFactory(profile);
		
				
				splash.progressTo("Creating drawable");
				GLCapabilities capabilities = new GLCapabilities(profile);
				final GLAutoDrawable sharedDrawable = factory.createDummyAutoDrawable(null, true, capabilities, null);
				sharedDrawable.display();
							
				if (System.getProperty("jhvVersion") == null)
					sharedDrawable.setGL(new DebugGL2(sharedDrawable.getGL().getGL2()));
		
				// Load settings from file but do not apply them yet
				// The settings must not be applied before the kakadu engine has
				// been initialized
				splash.progressTo("Loading settings");
				Settings.init();
		
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
		        try
				{
					Kdu_global.Kdu_customize_warnings(keepReference(new Kdu_message_formatter(new KduErrorHandler(false), 80)));
			        Kdu_global.Kdu_customize_errors(keepReference(new Kdu_message_formatter(new KduErrorHandler(true), 80)));
				}
				catch (KduException e)
				{
					Telemetry.trackException(e);
				}
		        
				splash.progressTo("Setting up caches");
				MovieCache.init();
				
				splash.progressTo("Compiling shaders");
				sharedDrawable.getContext().makeCurrent();
				ImageLayer.init(sharedDrawable.getGL().getGL2());
				
		        splash.progressTo("Starting Swing");
				ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
				JPopupMenu.setDefaultLightWeightPopupEnabled(false);
				MainFrame.init(sharedDrawable.getContext());
				
				splash.progressTo("Loading textures");
				NoImageScreen.init(sharedDrawable.getGL().getGL2());
				LUT.loadTexture(sharedDrawable.getGL().getGL2());
				HEKIcon.init(sharedDrawable.getGL().getGL2());
				
				// force initialization of UltimatePluginInterface
				splash.progressTo("Initializing plugins");
				Plugins.SINGLETON.getClass();
				sharedDrawable.getContext().release();
				
				splash.progressTo("Restoring settings");
				if(Settings.getBoolean(BooleanKey.STARTUP_3DCAMERA))
					CameraMode.set3DMode();
				else
					CameraMode.set2DMode();
				
				splash.progressTo("Show main window");
				MainFrame.SINGLETON.setVisible(true);
				
	            splash.progressTo("Loading observatories");
				if (Settings.getBoolean(Settings.BooleanKey.STARTUP_LOADMOVIE))
		            Observatories.addUpdateListener(new Runnable()
		            {
						@Override
						public void run()
						{
							AddLayerDialog.addDefaultStartupLayer();
						}
					});
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

	private static List<Object> keepAlive=new ArrayList<Object>();
	private static <T> T keepReference(T _x)
	{
		keepAlive.add(_x);
		return _x;
	}

	private static void loadLibraries()
	{
		String suffix = Globals.isReleaseVersion() ? "R":"D";
		
		
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
			System.loadLibrary("kdu_v77"+suffix);
			System.loadLibrary("kdu_a77"+suffix);
		}
		
		System.loadLibrary("kdu_jni"+suffix);
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
								new ExitProgramAction().actionPerformed(null);
							
							return null;
						}

						private void setHandled(Object event, Boolean val) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException
						{
							Method handleMethod = event.getClass().getMethod("setHandled", boolean.class);
							handleMethod.invoke(event, val);
						}
					});
			
			Method registerListenerMethod = applicationClass.getMethod("addApplicationListener", applicationListener);
			registerListenerMethod.invoke(application, listenerProxy);
		}
		catch (Throwable t)
		{
			System.err.println("Failed to create native menuitems for Mac OSX");
			Telemetry.trackException(t);
		}
	}
}
