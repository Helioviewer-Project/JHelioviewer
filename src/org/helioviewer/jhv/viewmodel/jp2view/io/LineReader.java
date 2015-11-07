package org.helioviewer.jhv.viewmodel.jp2view.io;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    static private final int CR = 13;
    static private final int LF = 10;

   /**
     * Reads a single line of the input stream.
     * 
     * @return The new line read or <code>null</code> if there is not more data.
     * @throws java.io.IOException
     */
    public static @Nullable String readLine(@Nonnull InputStream _is) throws IOException
    {
        StringBuilder res = new StringBuilder(32);

        int c=_is.read();
        if(c==-1)
            return null;
        
        for(;;)
            switch(c)
            {
                case CR:
                    if (_is.read() != LF)
                        throw new IOException("Line breaks not according to standard");
                case -1:
                case LF:
                    return res.toString();
                default:
                    res.append((char)c);
                    c = _is.read();
            }
    }
}
