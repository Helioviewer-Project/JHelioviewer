package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_thread_env;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_compressed_source_nonnative;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

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

	private Kdu_thread_env threadEnviroment;

	public NewRender() {
	    int numberThreads;
		try {
			numberThreads = Kdu_global.Kdu_get_num_processors();
			this.threadEnviroment = new Kdu_thread_env();
		    threadEnviroment.Create();
		      for (int i = 1; i < numberThreads; i++)
		    	  threadEnviroment.Add_thread();
		      
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public int[] getImage(int layerNumber, int quality, float zoomPercent, SubImage subImage){
		try {
			compositor.Refresh();
			Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();
			
			compositor.Add_ilayer(layerNumber, dimsRef1, dimsRef2);
			compositor.Set_max_quality_layers(quality);
			
			compositor.Set_scale(false, false, false,
					zoomPercent);

			// Determine dimensions for the rendered result & start processing
	        Kdu_dims view_dims = new Kdu_dims();
	        compositor.Get_total_composition_dims(view_dims);

			Kdu_dims actualBufferedRegion = KakaduUtils.roiToKdu_dims(subImage);
			Kdu_compositor_buf compositorBuf = compositor
					.Get_composition_buffer(actualBufferedRegion);

			Kdu_coords actualOffset = new Kdu_coords();
			actualOffset.Assign(actualBufferedRegion.Access_pos());

			
	        Kdu_coords view_size = view_dims.Access_size();
	        compositor.Set_buffer_surface(view_dims);
	        
			int[] region_buf = null;
			Kdu_dims newRegion = new Kdu_dims();
	        int region_buf_size = 0;

			while (compositor.Process(100000, newRegion)){
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(view_dims.Access_pos());
				
				int newPixels = newSize.Get_x() * newSize.Get_y();
		          if (newPixels == 0) continue;
		          if (newPixels > region_buf_size)
		          { // Augment the intermediate buffer as required
		            region_buf_size = newPixels;
		            region_buf = new int[region_buf_size];
		          }
		          compositorBuf.Get_region(newRegion,region_buf);

			}
			System.out.println("regionBuf : " + region_buf);
			return region_buf;
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void openImage(Kdu_cache cache){
		try {
			family_src.Open(cache);
			jpxSrc.Open(family_src, false);
			compositor.Create(jpxSrc);
			compositor.Set_thread_env(threadEnviroment, null);
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	void abolish() {
		try
        {
            threadEnviroment.Destroy();
        }
        catch(KduException e)
        {
            e.printStackTrace();
        }
	}
}
