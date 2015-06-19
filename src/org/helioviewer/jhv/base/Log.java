package org.helioviewer.jhv.base;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class Log
{
    private static final int LOG_SIZE=8192;
    private static StringBuffer log=new StringBuffer();
    
    private static final PrintStream originalErr = System.err;
    private static final PrintStream originalOut = System.out;
    
    public static void stopStdOutErrRedirection()
    {
        System.setErr(originalErr);
        System.setOut(originalOut);
    }
    
    public static void redirectStdOutErr()
    {
        log.setLength(0);
        try {
			System.setErr(new PrintStream(new OutputStream()
			{
			    @Override
			    public void write(int _b) throws IOException
			    {
			        if(log.length()>LOG_SIZE*2)
			            log.delete(0,LOG_SIZE);
			        log.append((char)_b);
			        originalErr.write(_b);
			    }},true,"UTF-8"));
        
	        System.setOut(new PrintStream(new OutputStream()
	        {
	            @Override
	            public void write(int _b) throws IOException
	            {
	                if(log.length()>LOG_SIZE*2)
	                    log.delete(0,LOG_SIZE);
	                log.append((char)_b);
	                originalOut.write(_b);
	            }},true,"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    }
    
    public static String GetLastFewLines(int _numberOfLines)
    {
        String[] lines=log.toString().split("\\n");
        String res="";
        
        for(int i=Math.min(_numberOfLines, lines.length)-1;i>=0;i--)
            res+=lines[lines.length-1-i]+"\n";
        
        return res;
    }
}
