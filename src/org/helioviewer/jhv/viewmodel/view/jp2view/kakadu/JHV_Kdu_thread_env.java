package org.helioviewer.jhv.viewmodel.view.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.base.math.MathUtils;

/**
 * Thread environment enabling one decode to use all cores of the machine.
 * 
 * @author caplins
 */
final public class JHV_Kdu_thread_env extends Kdu_thread_env
{
    public JHV_Kdu_thread_env()
    {
        try {
            int processorCount = Runtime.getRuntime().availableProcessors();
            processorCount = MathUtils.clip(processorCount, 1, 4);
            
            this.Create();
            for(int i=0;i<processorCount;i++)
                this.Add_thread(0);
        }
        catch (KduException ex) 
        {
            ex.printStackTrace();
        }
    }
}
