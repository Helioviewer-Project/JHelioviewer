package org.helioviewer.jhv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.Log;

import com.mindscapehq.raygun4java.core.RaygunClient;
import com.mindscapehq.raygun4java.core.messages.RaygunIdentifier;

public class UILatencyWatchdog
{
    //maximum time the UI thread is allowed to block
    private static final int MAX_LATENCY = 1000;
    
    //do not re-report errors within this time range
    private static final int COOLDOWN_AFTER_TIMEOUT = 30000; 
    
    
    private static volatile Thread awtDispatcher; 
    private static volatile boolean setFlag;
    
    public static void startWatchdog()
    {
        Thread t=new Thread(new Runnable()
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
                            awtDispatcher=Thread.currentThread();
                        }
                    });
                    
                    
                    for(;;)
                    {
                        setFlag=false;
                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                setFlag=true;
                            }
                        });
                        
                        Thread.sleep(MAX_LATENCY);
                        if(!setFlag)
                        {
                            String stackTraces="";
                            
                            //collect stack traces of all threads
                            /*for(Entry<Thread,StackTraceElement[]> e:Thread.getAllStackTraces().entrySet())
                            {
                                stackTraces+=e.getKey().getName()+":\n";
                                for(StackTraceElement ste:e.getValue())
                                    stackTraces+="  "+ste.toString()+"\n";
                                stackTraces+="\n\n";
                            }*/
                            
                            //collect stack trace of just the AWT dispatcher thread
                            StackTraceElement[] awtStackTrace = awtDispatcher.getStackTrace();
                            for(StackTraceElement ste:awtStackTrace)
                                stackTraces+="  "+ste.toString()+"\n";
                            
                            if(JHVGlobals.isReleaseVersion())
                            {
                                RaygunClient client = new RaygunClient("SchjoS2BvfVnUCdQ098hEA==");
                                client.SetVersion(JHVGlobals.VERSION);
                                Map<String, String> customData = new HashMap<String, String>();
                                customData.put("Log",Log.GetLastFewLines(6));
                                customData.put("JVM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " (JRE " + System.getProperty("java.specification.version") + ")");
                                
                                RaygunIdentifier user = new RaygunIdentifier(Settings.getProperty("UUID"));
                                client.SetUser(user);
                                ArrayList<String> tags = new ArrayList<String>();
                                tags.add(JHVGlobals.RAYGUN_TAG);
                                
                                Throwable diagThrowable = new Throwable("UI latency watchdog - UI thread hang detected");
                                diagThrowable.setStackTrace(awtStackTrace);
                                
                                client.Send(diagThrowable,tags,customData);
                            }
                            
                            System.err.println("UI latency watchdog - UI thread hang detected in:\n"+stackTraces);
                            
                            Thread.sleep(COOLDOWN_AFTER_TIMEOUT);
                        }
                    }
                }
                catch(Exception _ie)
                {
                }
            }
        });
        
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
        System.out.println("UI latency watchdog active");
    }
}
