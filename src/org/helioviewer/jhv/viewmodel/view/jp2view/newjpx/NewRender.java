package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.KakaduUtils;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_compositor_buf;
import kdu_jni.Kdu_coords;
import kdu_jni.Kdu_dims;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_ilayer_ref;
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
	private static final int MAX_RENDER_SAMPLES = 500000;

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
	
	public IntBuffer getImage(int layerNumber, int quality, float zoomPercent, SubImage subImage){
		try {
			compositor.Refresh();
			compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);
			
			Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();
			
			compositor.Add_ilayer(layerNumber, dimsRef1, dimsRef2);
			
			compositor.Set_max_quality_layers(quality);
			compositor.Set_scale(false, false, false,
					zoomPercent);

			Kdu_dims requestedBufferedRegion = KakaduUtils.roiToKdu_dims(subImage);
			
			compositor.Set_buffer_surface(requestedBufferedRegion);
			
			Kdu_dims actualBufferedRegion = new Kdu_dims();
			Kdu_compositor_buf compositorBuf = compositor
					.Get_composition_buffer(actualBufferedRegion);

			Kdu_coords actualOffset = new Kdu_coords();
			actualOffset.Assign(actualBufferedRegion.Access_pos());
				        
			Kdu_dims newRegion = new Kdu_dims();
	        int region_buf_size = 0;
	        IntBuffer intBuffer = ByteBuffer.allocateDirect(subImage.height * subImage.width *4).asIntBuffer();;
	        
	        
			while (compositor.Process(MAX_RENDER_SAMPLES, newRegion)){
				
				
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(actualOffset);

				int newPixels = newSize.Get_x() * newSize.Get_y();
		          if (newPixels == 0) continue;
		          if (newPixels > 0)
		          { 
		        	  region_buf_size = newPixels;
		            int[] region_buf = new int[region_buf_size];
			          compositorBuf.Get_region(newRegion,region_buf);
			          intBuffer.put(region_buf);
		          }
			}
			
			intBuffer.flip();
			return intBuffer;
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void openImage(Kdu_cache cache){
		final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;

		try {
			family_src.Open(cache);
			jpxSrc.Open(family_src, true);
			compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
			compositor.Set_thread_env(threadEnviroment, null);
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void openLocalImage(File file) throws KduException, IOException{
		family_src.Open(file.getCanonicalPath(), true);
		jpxSrc.Open(family_src, false);
		compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
		compositor.Set_thread_env(threadEnviroment, null);
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
