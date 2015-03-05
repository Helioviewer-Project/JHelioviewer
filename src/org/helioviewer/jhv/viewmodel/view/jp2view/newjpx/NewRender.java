package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_thread_env;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_region_compositor;

public class NewRender {
	
	
    // The amount of cache to allocate to each codestream
    private final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;

    /**
     * The this extended version of Jp2_threadsafe_family_src can open any file
     * conforming to the jp2 specifications (.jp2, .jpx, .mj2, etc). The reason
     * for extending this class is that the Acquire/Release_lock() functions
     * needed to be implemented.
     */
	private Jp2_threadsafe_family_src family_src = new Jp2_threadsafe_family_src();
	
    /** The Jpx_source object is capable of opening jp2 and jpx sources. */
    private Jpx_source jpxSrc = new Jpx_source();

    /**
     * The compositor object takes care of all the rendering via its process
     * function.
     */
    private Kdu_region_compositor compositor = new Kdu_region_compositor();

	public NewRender() {
		Thread thread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				initKakaduMachinery();
				while(true){
				}
				
			}
		});
		thread.start();
	}
	
	
	private void initKakaduMachinery(){
		try {
			jpxSrc.Open(family_src, false);
			
			compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
			JHV_Kdu_thread_env threadEnv = new JHV_Kdu_thread_env(); 
			compositor.Set_thread_env(threadEnv, null);
			
			
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
