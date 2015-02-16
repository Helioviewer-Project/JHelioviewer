package org.helioviewer.jhv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.Log;

import com.mindscapehq.raygun4java.core.RaygunClient;
import com.mindscapehq.raygun4java.core.messages.RaygunIdentifier;

public class UILatencyWatchdog
{
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
                        
                        Thread.sleep(1000);
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
                            for(StackTraceElement ste:awtDispatcher.getStackTrace())
                                stackTraces+="  "+ste.toString()+"\n";
                            
                            RaygunClient client = new RaygunClient("SchjoS2BvfVnUCdQ098hEA==");
                            client.SetVersion(JHVGlobals.VERSION);
                            Map<String, String> customData = new HashMap<String, String>();
                            customData.put("Log",Log.GetLastFewLines(6));
                            customData.put("JVM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " (JRE " + System.getProperty("java.specification.version") + ")");
    
                            RaygunIdentifier user = new RaygunIdentifier(Settings.getProperty("UUID"));
                            client.SetUser(user);
                            ArrayList<String> tags = new ArrayList<String>();
                            tags.add(JHVGlobals.tag);
                            //client.Send(new Throwable("UI latency watchdog - UI thread hangs in:\n"+stackTraces),tags,customData);
                            
                            System.err.println("UI latency watchdog - UI thread hangs in:\n"+stackTraces);
                            
                            Thread.sleep(10000);
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
