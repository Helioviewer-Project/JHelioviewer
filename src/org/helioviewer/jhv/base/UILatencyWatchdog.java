package org.helioviewer.jhv.base;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.Settings.StringKey;

import com.mindscapehq.raygun4java.core.RaygunClient;
import com.mindscapehq.raygun4java.core.messages.RaygunIdentifier;

public class UILatencyWatchdog
{
	// maximum time the UI thread is allowed to block
	private static final int MAX_LATENCY_RELEASE = 1000;
	private static final int MAX_LATENCY_DEBUG = 500;

	// do not re-report errors within this time range
	private static final int COOLDOWN_AFTER_TIMEOUT = 10000;
	
	private static volatile Thread awtDispatcher;
	private static volatile boolean setFlag;
	
	private static List<Thread> focussedThreads=new ArrayList<>();
	
	private static Thread t;

	public static void startWatchdog()
	{
		t = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					SwingUtilities.invokeAndWait(new Runnable()
					{
						@Override
						public void run()
						{
							awtDispatcher = Thread.currentThread();
						}
					});
					setFocus();

					// wait a couple of seconds to skip non-responsive parts of
					// startup
					Thread.sleep(7000);

					for (;;)
					{
						setFlag = false;
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								setFlag = true;
							}
						});
						
						if(Globals.IS_RELEASE_VERSION)
							Thread.sleep(MAX_LATENCY_RELEASE);
						else
							Thread.sleep(MAX_LATENCY_DEBUG);
						
						if (!setFlag && awtDispatcher.isAlive())
						{
							// collect stack trace of just the AWT dispatcher
							// thread
							//
							// the limited stack trace only contains the stack,
							// starting from a jhv-relevant method, instead of
							// the full stack trace.
							// this leads to better reporting since all
							// exceptions will get grouped by raygun
							
							boolean foundAThread=false;
							for(boolean onlyRunnable:new boolean[]{true,false})
								for(Thread t:focussedThreads)
								{
									if(foundAThread || (onlyRunnable && t.getState()!=State.RUNNABLE))
										continue;
									
									foundAThread=true;
									
									StringBuilder fullStackTrace = new StringBuilder();
									StackTraceElement[] awtStackTrace = t.getStackTrace();
									List<StackTraceElement> limitedStackTrace = new ArrayList<>();
									boolean jhvPartFound = false;
									for (StackTraceElement ste : awtStackTrace)
									{
										fullStackTrace.append("  ").append(ste.toString()).append("\n");
										
										jhvPartFound |= ste.getClassName().startsWith("org.helioviewer.jhv.");
										if (jhvPartFound)
											limitedStackTrace.add(ste);
									}
		
									// huh, jhv is not even involved?! let's report the
									// complete stack trace in that case...
									if (!jhvPartFound)
										Collections.addAll(limitedStackTrace, awtStackTrace);
		
									if (isRMIActive())
									{
										// it seems that someone is debugging this app
										// --> leads to spurious alerts
										// --> ignore & stop further processing
										System.out.println("UI latency watchdog: Debugger detected.");
										return;
									}
		
									// only report hangs to raygun in release builds
									if (Globals.IS_RELEASE_VERSION)
									{
										RaygunClient client = new RaygunClient("QXtNXLEKWBfClhyteqov4w==");
										client.SetVersion(Globals.VERSION);
										Map<String, String> customData = new HashMap<>();
										customData.put("Log", Log.GetLastFewLines(6));
										customData.put(
												"JVM",
												System.getProperty("java.vm.name")
														+ " "
														+ System.getProperty("java.vm.version")
														+ " (JRE "
														+ System.getProperty("java.specification.version")
														+ ")");
										customData.put("FullStackTrace", fullStackTrace.toString());
		
										RaygunIdentifier user = new RaygunIdentifier(Settings.getString(StringKey.UUID));
										client.SetUser(user);
										ArrayList<String> tags = new ArrayList<>();
										tags.add(Globals.RAYGUN_TAG);
										tags.add("latency-watchdog");
		
										Throwable diagThrowable = new Throwable("UI latency watchdog - UI thread hang detected");
										diagThrowable.setStackTrace(limitedStackTrace.toArray(new StackTraceElement[0]));
										client.Send(diagThrowable, tags, customData);
									}
									
									// log to the console in all cases, useful during development
									System.err.println("UI latency watchdog - UI thread hang detected in:\n" + fullStackTrace);
								}
							}
						Thread.sleep(COOLDOWN_AFTER_TIMEOUT);
					}
				}
				catch (Throwable _ie)
				{
				}
			}
		});

		t.setName("UI latency watchdog");
		t.setDaemon(true);
		t.setPriority(Thread.MAX_PRIORITY);
		t.start();
		
		//avoid false reports during shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stopWatchdog()));
		
		ShutdownManager.addShutdownHook(ShutdownManager.ShutdownPhase.STOP_WORK_1, () -> stopWatchdog());
		
		System.out.println("UI latency watchdog active");
	}

	private static boolean isRMIActive()
	{
		for (Thread t : Thread.getAllStackTraces().keySet())
			if (t.getName().startsWith("RMI "))
				return true;

		return false;
	}

	public static void stopWatchdog()
	{
		setFlag = true;
		t.interrupt();
	}

	public static void setFocus(Thread... _glWorkerThreads)
	{
		focussedThreads.clear();
		focussedThreads.add(awtDispatcher);
		for(Thread t:_glWorkerThreads)
			focussedThreads.add(t);
	}
}
