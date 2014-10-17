package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import com.jogamp.common.nio.Buffers;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

/**
 * Reads in Memory PfssData and writes PfssFrames
 * @author Jonas Schwammberger
 *
 */
public class PfssDataReader {

	public PfssDataReader() {
		
	}
	
	public void readData(PfssDataNew data,PfssFrame[] frames) {
		
	}
	
	private void readFits(PfssDataNew data,PfssFrame frame) {
			InputStream is = null;
			try {
				is = new ByteArrayInputStream(data.getData());
				Fits fits = new Fits(is, false);
				BasicHDU hdus[] = fits.read();
				BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
				double b0 = ((double[]) bhdu.getColumn("B0"))[0];
				double l0 = ((double[]) bhdu.getColumn("L0"))[0];
				short[] ptr = ((short[][]) bhdu.getColumn("PTR"))[0];
				short[] ptr_nz_len = ((short[][]) bhdu.getColumn("PTR_NZ_LEN"))[0];
				short[] ptph = ((short[][]) bhdu.getColumn("PTPH"))[0];
				short[] ptth = ((short[][]) bhdu.getColumn("PTTH"))[0];

				FloatBuffer vertices = Buffers.newDirectFloatBuffer(ptr.length * 3 + 3);;
				IntBuffer indicesSunToOutside = Buffers.newDirectIntBuffer(ptr_nz_len.length * 2);
				IntBuffer indicesSunToSun = Buffers.newDirectIntBuffer(ptr_nz_len.length * 2);
				IntBuffer indicesOutsideToSun = Buffers.newDirectIntBuffer(ptr_nz_len.length * 2);
				
				for (int i = 0; i < ptr.length; i++)
				{
					
				}
				
			} catch (FitsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	}
	
	
	private class Point {
		float x;
		float y;
		float z;
		
	}
	
	private enum TYPE {
		SUN_TO_OUTSIDE, SUN_TO_SUN, OUTSIDE_TO_SUN
	};
}
