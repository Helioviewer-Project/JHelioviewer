package org.helioviewer.jhv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.Log;

import com.mindscapehq.raygun4java.core.RaygunClient;
import com.mindscapehq.raygun4java.core.messages.RaygunIdentifier;

public class UILatencyWatchdog
{
    //maximum time the UI thread is allowed to block
    private static final int MAX_LATENCY = 1500;
    
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
                            //collect stack trace of just the AWT dispatcher thread
                            //
                            //the limited stack trace only contains the stack, starting from a jhv-relevant method, instead of the full stack trace.
                            //this leads to better reporting since all exceptions will get grouped by raygun
                            
                            String fullStackTrace="";
                            StackTraceElement[] awtStackTrace=awtDispatcher.getStackTrace();
                            List<StackTraceElement> limitedStackTrace=new ArrayList<>();
                            boolean jhvPartFound = false;
                            for(StackTraceElement ste:awtStackTrace)
                            {
                                fullStackTrace+="  "+ste.toString()+"\n";
                                
                                jhvPartFound |= ste.getClassName().startsWith("org.helioviewer.jhv.");
                                if(jhvPartFound)
                                    limitedStackTrace.add(ste);
                            }
                            
                            //huh, jhv not even involved?! let's report the complete stack trace in that case...
                            if(!jhvPartFound)
                            {
                                for(StackTraceElement ste:awtStackTrace)
                                    limitedStackTrace.add(ste);
                            }
                            
                            if(isRMIActive())
                            {
                                //it seems that someone is debugging this app
                                //--> leads to spurious alerts
                                //--> ignore & stop further processing
                                System.out.println("UI latency watchdog: Debugger detected.");
                                return;
                            }
                            
                            //only report hangs to raygun in release builds
                            if(JHVGlobals.isReleaseVersion())
                            {
                                RaygunClient client = new RaygunClient("KHbza8sFEOIj9No3aKzzhw==");
                                client.SetVersion(JHVGlobals.VERSION);
                                Map<String, String> customData = new HashMap<String, String>();
                                customData.put("Log",Log.GetLastFewLines(6));
                                customData.put("JVM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " (JRE " + System.getProperty("java.specification.version") + ")");
                                customData.put("FullStackTrace",fullStackTrace);
                                
                                RaygunIdentifier user = new RaygunIdentifier(Settings.getProperty("UUID"));
                                client.SetUser(user);
                                ArrayList<String> tags = new ArrayList<String>();
                                tags.add(JHVGlobals.RAYGUN_TAG);
                                tags.add("latency-watchdog");
                                
                                Throwable diagThrowable = new Throwable("UI latency watchdog - UI thread hang detected");
                                diagThrowable.setStackTrace(limitedStackTrace.toArray(new StackTraceElement[0]));
                                
                                client.Send(diagThrowable,tags,customData);
                            }
                            
                            //log to the console in all cases, useful during development
                            System.err.println("UI latency watchdog - UI thread hang detected in:\n"+fullStackTrace);
                            
                            Thread.sleep(COOLDOWN_AFTER_TIMEOUT);
                        }
                    }
                }
                catch(Exception _ie)
                {
                }
            }
        });
        
        t.setName("UI latency watchdog");
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
        System.out.println("UI latency watchdog active");
    }
    
    private static boolean isRMIActive()
    {
        for(Thread t:Thread.getAllStackTraces().keySet())
            if(t.getName().startsWith("RMI "))
                return true;
        
        return false;
    }
}
