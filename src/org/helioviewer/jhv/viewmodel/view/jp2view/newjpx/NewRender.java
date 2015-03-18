package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_thread_env;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_compressed_source_nonnative;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
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
	
	public int[] getImage(int layerNumber, int quality){
		try {
			compositor.Refresh();
			Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();
			
			compositor.Add_ilayer(layerNumber, dimsRef1, dimsRef2);
			compositor.Set_max_quality_layers(quality);
			
			// Determine dimensions for the rendered result & start processing
	        Kdu_dims view_dims = new Kdu_dims();
	        compositor.Get_total_composition_dims(view_dims);
	        Kdu_coords view_size = view_dims.Access_size();
	        compositor.Set_buffer_surface(view_dims);
			Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(view_dims);
			int[] region_buf = null;
			Kdu_dims newRegion = new Kdu_dims();
			while (compositor.Process(100000, newRegion)){
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(view_dims.Access_pos());
				
				int newPixels = newSize.Get_x() * newSize.Get_y();
				
			}
			
			
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void openImage(Kdu_cache cache){
		try {
			family_src.Open(cache);
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
