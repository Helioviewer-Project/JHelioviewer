package org.helioviewer.jhv.viewmodel.view.jp2view.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * The <code>StringInputStream</code> class allows reading of single lines from
 * an input stream. The end of a line is either LF (10), CR (13), or CR followed
 * immediately by LF. The end of stream is also considered a legal end of line.
 * This stream is not buffered.
 */
public class LineReader
{
    static public final int CR = 13;
    static public final int LF = 10;

    private LineReader()
    {
    }
    
    /**
     * Reads a single line of the input stream.
     * 
     * @return The new line read or <code>null</code> if there is not more data.
     * @throws java.io.IOException
     */
    public static String readLine(InputStream _is) throws IOException {
        StringBuffer res = new StringBuffer(32);

        int c=_is.read();
        if(c==-1)
            return null;
        
        for(;;)
        {
            if (c == LF)
                break;
            
            if (c == CR)
            {
                if (_is.read() != LF)
                    throw new IOException("Line breaks not according to standard");
                
                break;
            }
            
            res.append((char)c);
            
            c = _is.read();
            if (c == -1)
                break;
        }
        
        return res.toString();
    }
}
