package org.helioviewer.jhv.viewmodel.view.jp2view.kakadu;

import kdu_jni.KduException;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.base.math.MathUtils;

/**
 * Thread environment enabling one decode to use all cores of the machine.
 * 
 * @author caplins
 */
final public class JHV_Kdu_thread_env extends Kdu_thread_env {

    // Singleton pattern crap
    private static final JHV_Kdu_thread_env SINGLETON = new JHV_Kdu_thread_env();

    public static JHV_Kdu_thread_env getSingletonInstance() {
        return SINGLETON;
    }

    /**
     * Method to adjust the Kdu_thread_env objects thread pool. Necessary since
     * Java's Runtime.availableProcessors method can return different values in
     * the course of program execution. It restricts the number of threads to
     * between 1 and 8.
     * 
     * TODO Testing needs to be done to determine how often the processor count
     * changes. Depending on how often the return value differs it may be
     * computationally expensive to destroy and create the threadEnv every time.
     * Currently I am under the belief that the availableProcessors method
     * changes relatively infrequently... but that could be platform dependent.
     * 
     * @throws JHV_KduException
     */
    public void updateNumThreads() {
        try {
            int processorCount = Runtime.getRuntime().availableProcessors();
            processorCount = MathUtils.clip(processorCount, 1, 2);
            if (this.Get_num_threads() != processorCount) {
                this.Destroy();
                this.Create();
                while (this.Get_num_threads() < processorCount && this.Add_thread(processorCount))
                    ;
            }

        } catch (KduException ex) {
            ex.printStackTrace();
        }
    }
    
    public void updateNumThreads(int i){
        try {
    	if (this.Get_num_threads() != i){
    		this.Destroy();
    		this.Create();
    	    while (this.Get_num_threads() < i && this.Add_thread(i))
                ;
    	}

    } catch (KduException ex) {
        ex.printStackTrace();
    }    }
}
