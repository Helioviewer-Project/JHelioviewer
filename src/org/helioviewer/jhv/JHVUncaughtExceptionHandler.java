package org.helioviewer.jhv;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.Socket;
import java.util.Date;
import java.util.LinkedList;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.base.logging.LogSettings;

/**
 * Routines to catch and handle all runtime exceptions.
 * 
 * @author Malte Nuhn
 */
public class JHVUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private static final JHVUncaughtExceptionHandler SINGLETON = new JHVUncaughtExceptionHandler();

    public static JHVUncaughtExceptionHandler getSingletonInstance() {
        return SINGLETON;
    }

    /**
     * This method sets the default uncaught exception handler. Thus, this
     * method should be called once when the application starts.
     */
    public static void setupHandlerForThread() {
        Thread.setDefaultUncaughtExceptionHandler(JHVUncaughtExceptionHandler.getSingletonInstance());
    }

    /**
     * Generates a simple error Dialog, allowing the user to copy the
     * errormessage to the clipboard.
     * <p>
     * As options it will show {"Quit JHelioviewer", "Continue"} and quit if
     * necessary.
     * 
     * @param title
     *            Title of the Dialog
     * @param msg
     *            Object to display in the main area of the dialog.
     */
    private static void showErrorDialog(final String title, final String msg) {

        Vector<Object> objects = new Vector<Object>();
        
        JLabel head=new JLabel("Dang! You hit a bug in JHelioviewer.");
        head.setFont(head.getFont().deriveFont(Font.BOLD));
        
        objects.add(head);
        objects.add(Box.createVerticalStrut(10));
        objects.add(new JLabel("Here are some technical details about the problem:"));

        Font font = new JLabel().getFont();
        font = font.deriveFont(font.getStyle() ^ Font.ITALIC);

        JTextArea textArea = new JTextArea();
        textArea.setMargin(new Insets(5, 5, 5, 5));
        textArea.setText(msg);
        textArea.setEditable(false);
        JScrollPane sp = new JScrollPane(textArea);
        sp.setPreferredSize(new Dimension(600, 400));

        objects.add(sp);
        JCheckBox allowCrashReport = new JCheckBox("Send this anonymous crash report to the developers.",true);
        objects.add(allowCrashReport);
        objects.add(Box.createVerticalStrut(10));
        
        JOptionPane optionPane = new JOptionPane(title);
        optionPane.setMessage(objects.toArray());
        optionPane.setMessageType(JOptionPane.ERROR_MESSAGE);
        optionPane.setOptions(new String[] { "Quit" });
        JDialog dialog = optionPane.createDialog(null, title);
        
        dialog.setVisible(true);
        
        if(allowCrashReport.isSelected())
            for(int port:new int[]{80,514,10000})
            {
                Socket s;
                try
                {
                    s=new Socket("data.logentries.com",port);
                    try(PrintStream ps=new PrintStream(s.getOutputStream()))
                    {
                        String token="0c40071e-fe6b-4490-87cd-5f84e2fd52a7 ";
                        ps.println(token+"-------------------------------------------------\n"
                                +token+msg.replace("\n","\n"+token));
                    }
                    Runtime.getRuntime().halt(0);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        
        Runtime.getRuntime().halt(0);
    }

    private JHVUncaughtExceptionHandler() {
    }

    // we do not use the logger here, since it should work even before logging
    // initialization
    @SuppressWarnings("deprecation")
    public void uncaughtException(Thread t, Throwable e) {
        //STOP THE WORLD to avoid exceptions piling up
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread _t,Throwable _e)
            {
                //IGNORE all other exceptions
            }
        });
        
        System.err.close();
        // Close all threads (excluding systemsthreads, just stopp the timer thread from the system)
        for(Thread thr:Thread.getAllStackTraces().keySet())
            if(thr!=Thread.currentThread() && (!thr.getThreadGroup().getName().equalsIgnoreCase("system") || thr.getName().contains("Timer")))
            	thr.suspend();
        for(Thread thr:Thread.getAllStackTraces().keySet())
        	if(thr!=Thread.currentThread() && (!thr.getThreadGroup().getName().equalsIgnoreCase("system") || thr.getName().contains("Timer")))
                    thr.stop();
        String msg = "JHelioviewer: " + JHVGlobals.VERSION_AND_RELEASE + "\n";
        msg += "Date: " + new Date() + "\n";
        msg += "JVM: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + " (JRE " + System.getProperty("java.specification.version") + ")\n";
        msg += "OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch") + " " + System.getProperty("os.version") + "\n\n";

        LinkedList<String> lastLines = new LinkedList<String>();
        try(BufferedReader input = new BufferedReader(new FileReader(LogSettings.getCurrentLogFile())))
        {
            String line;
            while ((line = input.readLine()) != null)
            {
                lastLines.addLast(line);
                if(lastLines.size()>4)
                    lastLines.removeFirst();
            }
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
        
        for(String line:lastLines)
            msg+=line+"\n";

        try(StringWriter st=new StringWriter())
        {
            try(PrintWriter pw=new PrintWriter(st))
            {
                e.printStackTrace(pw);
                msg+=st.toString();
            }
        }
        catch(IOException e1)
        {
            e1.printStackTrace();
        }

        for(Frame f:Frame.getFrames())
            f.setVisible(false);
        
        Log.fatal("Runtime exception", e);
        
        //this wizardry forces the creation of a new awt event queue
        //which is needed to show the error dialog
        final String finalMsg=msg;
        new Thread(new Runnable()
        {
            public void run()
            {	
                EventQueue.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        JHVUncaughtExceptionHandler.showErrorDialog("JHelioviewer: Fatal error", finalMsg);
                    }
                });
            }
        }).start();
    }
}
