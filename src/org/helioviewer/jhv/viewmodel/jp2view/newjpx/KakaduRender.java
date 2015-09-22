package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

import org.helioviewer.jhv.viewmodel.jp2view.kakadu.KakaduUtils;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataContainer;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataFactory;
import org.w3c.dom.Document;

public class KakaduRender
{
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
	private static final int MAX_RENDER_SAMPLES = 128000;

	public KakaduRender()
	{
		int numberThreads;
		try
		{
			numberThreads = Kdu_global.Kdu_get_num_processors();
			this.threadEnviroment = new Kdu_thread_env();
			threadEnviroment.Create();
			for (int i = 0; i < numberThreads; i++)
				threadEnviroment.Add_thread();
		}
		catch (KduException e)
		{
			
			e.printStackTrace();
		}
	}

	public ByteBuffer getImage(int layerNumber, int quality, float zoomPercent, Rectangle imageSize)
	{
		try
		{
			compositor.Refresh();
			compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);

			Kdu_dims dimsRef1 = new Kdu_dims(), dimsRef2 = new Kdu_dims();

			compositor.Add_ilayer(layerNumber, dimsRef1, dimsRef2);

			//FIXME: downgrade quality first, before resolution when having speed problems
			compositor.Set_max_quality_layers(quality);
			
			compositor.Set_scale(false, false, false, zoomPercent);
			Kdu_dims requestedBufferedRegion = KakaduUtils.rectangleToKdu_dims(imageSize);

			compositor.Set_buffer_surface(requestedBufferedRegion);

			Kdu_dims actualBufferedRegion = new Kdu_dims();
			Kdu_compositor_buf compositorBuf = compositor.Get_composition_buffer(actualBufferedRegion);
			Kdu_coords actualOffset = new Kdu_coords();
			actualOffset.Assign(actualBufferedRegion.Access_pos());

			Kdu_dims newRegion = new Kdu_dims();
			int region_buf_size = 0;
			ByteBuffer byteBuffer = ByteBuffer.allocateDirect(imageSize.height
					* imageSize.width * 4);
			IntBuffer intBuffer = byteBuffer.asIntBuffer();

			while (compositor.Process(MAX_RENDER_SAMPLES, newRegion))
			{
				Kdu_coords newOffset = newRegion.Access_pos();
				Kdu_coords newSize = newRegion.Access_size();
				newOffset.Subtract(actualOffset);

				int newPixels = newSize.Get_x() * newSize.Get_y();
				if (newPixels == 0)
					continue;
				if (newPixels > 0)
				{
					region_buf_size = newPixels;
					//FIXME: don't reallocate int-array
					int[] region_buf = new int[region_buf_size];
					compositorBuf.Get_region(newRegion, region_buf);
					intBuffer.put(region_buf);
				}
			}

			intBuffer.flip();
			compositor.Remove_ilayer(new Kdu_ilayer_ref(), true);

			return byteBuffer;
		}
		catch (KduException e)
		{
			
			e.printStackTrace();
		}
		return null;
	}

	public void openImage(String filename)
	{
		final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;
		try {
			compositor.Pre_destroy();
			compositor = new Kdu_region_compositor();
			jpxSrc.Close();
			family_src.Close();
			family_src.Open(filename);
			jpxSrc.Open(family_src, true);
			compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
			compositor.Set_thread_env(threadEnviroment, null);
		} catch (KduException e) {
			
			e.printStackTrace();
		}
	}
	
	public void openImage(Kdu_region_compositor compositor)
	{
		try {
			compositor.Set_thread_env(threadEnviroment, null);
		} catch (KduException e) {
			
			e.printStackTrace();
		}
		this.compositor = compositor;
	}

	public void closeImage()
	{
		//FIXME: ??? perhaps compositor.finalize(); oder .Pre_destroy() ???
		compositor = null;
	}

	public void openImage(Kdu_cache cache)
	{
		final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;

		try
		{
			compositor.Pre_destroy();
			compositor = new Kdu_region_compositor();
			jpxSrc.Close();
			family_src.Close();
			family_src.Open(cache);
			jpxSrc.Open(family_src, true);
			compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
			compositor.Set_thread_env(threadEnviroment, null);
		} catch (KduException e) {
			
			e.printStackTrace();
		}
	}
	


	public MetaData getMetadata(int index, Jp2_threadsafe_family_src family_src) throws KduException
	{
		String xmlText = KakaduUtils.getXml(family_src, index);
		if (xmlText == null)
			return null;
		xmlText = xmlText.trim().replace("&", "&amp;").replace("$OBS", "");

		InputStream in = null;
		try
		{
			in = new ByteArrayInputStream(xmlText.getBytes("UTF-8"));
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(in);
			doc.getDocumentElement().normalize();

			MetaData metaData = MetaDataFactory.getMetaData(new MetaDataContainer(doc));
			return metaData;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}

	public int getFrameCount() throws KduException
	{
		int[] tempVar = new int[1];
		jpxSrc.Count_compositing_layers(tempVar);
		return tempVar[0];
	}
}
