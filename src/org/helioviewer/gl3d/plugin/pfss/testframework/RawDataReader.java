package org.helioviewer.gl3d.plugin.pfss.testframework;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import org.helioviewer.base.physics.Constants;

import com.jogamp.common.nio.Buffers;


public class RawDataReader {
	private ArrayList<Line> lines;
	
	
	public RawDataReader(String filePath) {
		this.readFits(filePath);
	}
	
	public ArrayList<Line> getLines() {
		return lines;
	}
	
	private void readFits(String filePath) {

			try {
				Fits fits = new Fits(filePath, false);
				BasicHDU hdus[] = fits.read();
				BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
				double b0 = ((double[]) bhdu.getColumn("B0"))[0];
				double l0 = ((double[]) bhdu.getColumn("L0"))[0];
				short[] ptr = ((short[][]) bhdu.getColumn("PTR"))[0];
				short[] ptr_nz_len = ((short[][]) bhdu.getColumn("PTR_NZ_LEN"))[0];
				short[] ptph = ((short[][]) bhdu.getColumn("PTPH"))[0];
				short[] ptth = ((short[][]) bhdu.getColumn("PTTH"))[0];

				lines = new ArrayList<>(ptr_nz_len.length);

				int vertexIndex = 0;

				// loop through all lines
				for (int i = 0; i < ptr_nz_len.length; i++) {
					
					int lineSize = ptr_nz_len[i];
					ArrayList<Point> points = new ArrayList<>(lineSize);
					
					Point lastP = new Point(vertexIndex, ptr[vertexIndex],
							ptph[vertexIndex], ptth[vertexIndex], l0, b0);
					points.add(lastP);
					
					int maxSize = vertexIndex + lineSize;
					vertexIndex++;
					for (; vertexIndex < maxSize; vertexIndex++) {
						Point current = new Point(vertexIndex, ptr[vertexIndex], ptph[vertexIndex],
								ptth[vertexIndex], l0, b0);
						points.add(current);
						lastP = current;
					}
					
					Line l = new Line(points);
					lines.add(l);
				}

			} catch (FitsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {


			}
		
	}
}
