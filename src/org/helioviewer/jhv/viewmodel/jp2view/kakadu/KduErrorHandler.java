package org.helioviewer.jhv.viewmodel.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_thread_safe_message;

/**
 * This class allows to print Kakadu error messages, throwing Java exceptions if
 * it is necessary.
 */
public class KduErrorHandler extends Kdu_thread_safe_message
{
    public KduErrorHandler()
    {
    }
    
    @Override
    public void Put_text(String text)
    {
    	System.err.println(text);
    }

    @Override
    public void Flush(boolean endOfMessage) throws KduException
    {
        if (endOfMessage)
            throw new KduException(Kdu_global.KDU_ERROR_EXCEPTION, "Kakadu error");
    }
}
