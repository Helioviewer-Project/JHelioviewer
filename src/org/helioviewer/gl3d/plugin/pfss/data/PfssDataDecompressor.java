package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.plugin.pfss.data.managers.PfssFrameInitializer;
import org.helioviewer.gl3d.plugin.pfss.settings.PfssSettings;

import com.jogamp.common.nio.Buffers;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

/**
 * Reads in Memory PfssData and writes PfssFrames. Supports running in its own thread for asynchronous loading
 * 
 * @author Jonas Schwammberger
 *
 */
public class PfssDataDecompressor implements Runnable {
	private final PfssData data;
	private final PfssFrame frame;
	private final PfssFrameInitializer initializer;

	public PfssDataDecompressor(PfssData data, PfssFrame frame,
			PfssFrameInitializer initializer) {
		this.data = data;
		this.frame = frame;
		this.initializer = initializer;
	}

	/**
	 * Reads the Pfss Data and fills out the frame object
	 */
	public void readData() {
		// do decompression here
		//this.readFits();
		readOldfits();
		
		// it can happen that the readFits() could not finish loading the frame.
		// this means that the current Frame will never be loaded
		if (frame.isLoaded())
			initializer.addLoadedFrame(frame);
	}
	
	
	@Deprecated
	private enum TYPE {
		SUN_TO_OUTSIDE, SUN_TO_SUN, OUTSIDE_TO_SUN
	};
	
	@Deprecated
	private void readOldfits() {
		if (!data.isLoaded()) {
			try {
				data.awaitLoaded();
			} catch (InterruptedException e) {
				// do nothing and exit this method
			}
		}
		if (data.isLoaded()) {
			InputStream is = null;
			try {
				is = new ByteArrayInputStream(data.getData());
				Fits fits = new Fits(is, true);
				BasicHDU hdus[] = fits.read();
				BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
				double b0 = ((double[]) bhdu.getColumn("B0"))[0];
				double l0 = ((double[]) bhdu.getColumn("L0"))[0];
				short[] ptr = ((short[][]) bhdu.getColumn("PTR"))[0];
				short[] ptr_nz_len = ((short[][]) bhdu.getColumn("PTR_NZ_LEN"))[0];
				short[] ptph = ((short[][]) bhdu.getColumn("PTPH"))[0];
				short[] ptth = ((short[][]) bhdu.getColumn("PTTH"))[0];
	
				FloatBuffer vertices = Buffers
						.newDirectFloatBuffer(ptr.length * 3 + 3);
				IntBuffer indicesSunToOutside = Buffers
						.newDirectIntBuffer(ptr.length * 2);
				IntBuffer indicesSunToSun = Buffers
						.newDirectIntBuffer(ptr.length * 2);
				IntBuffer indicesOutsideToSun = Buffers
						.newDirectIntBuffer(ptr.length * 2);
				
				
					int lineEnd = ptr_nz_len[0] - 1;
					int lineCounter = 1;
					int counter = 0;
					int lineStart = 0;
	
					TYPE type = getType(ptr,lineStart, lineEnd);
	
					IntBuffer currentBuffer = this.getLineType(ptr, lineStart, lineEnd, indicesSunToOutside, indicesSunToSun, indicesOutsideToSun);
					double xStart = 0;
					double yStart = 0;
					double zStart = 0;
					boolean lineStarted = false;
					for (int i = 0; i < ptr.length; i += PfssSettings.LOD_STEPS) {
	
						if (i > lineEnd && lineCounter < ptr_nz_len.length){
							i = lineEnd;
						}
						boolean colinear = false;
	
						double r0 = ptr[i] / 8192.0 * Constants.SunRadius;
						double phi0 = ptph[i] / 32768.0 * 2 * Math.PI;
						double theta0 = ptth[i] / 32768.0 * 2 * Math.PI;
	
						phi0 -= l0 / 180.0 * Math.PI;
						theta0 += b0 / 180.0 * Math.PI;
						double z = r0 * Math.sin(theta0) * Math.cos(phi0);
						double x = r0 * Math.sin(theta0) * Math.sin(phi0);
						double y = r0 * Math.cos(theta0);
	
						if (lineStarted) {
							if (i + 1 < ptr.length) {
								double r1 = ptr[i + 1] / 8192.0 * Constants.SunRadius;
								double phi1 = ptph[i + 1] / 32768.0 * 2 * Math.PI;
								double theta1 = ptth[i + 1] / 32768.0 * 2 * Math.PI;
	
								phi1 -= l0 / 180.0 * Math.PI;
								theta1 += b0 / 180.0 * Math.PI;
	
								double zEnd = r1 * Math.sin(theta1) * Math.cos(phi1);
								double xEnd = r1 * Math.sin(theta1) * Math.sin(phi1);
								double yEnd = r1 * Math.cos(theta1);
								double angle = this.calculateAngleBetween2Vecotrs(xEnd - x,
										yEnd - y, zEnd - z, x - xStart, y - yStart, z
												- zStart);
								colinear = angle > PfssSettings.ANGLE_OF_LOD
										&& i != lineEnd;
							}
						}
	
						else {
							lineStarted = true;
							xStart = x;
							yStart = y;
							zStart = z;
						}
	
						if (!colinear) {
							if (i != lineEnd) {
								xStart = x; yStart = y; zStart = z;
								this.addIndex(currentBuffer, counter);
							}
							counter = this.addVertex(vertices,(float) x, (float) y, (float) z,
									counter);
	
						}
	
						if (i == lineEnd) {
							lineStarted = false;
							lineStart = lineEnd + 1;
							if (lineCounter < ptr_nz_len.length) {
								lineEnd += ptr_nz_len[lineCounter++];
							}
	
							type = getType(ptr,lineStart, lineEnd);
							currentBuffer = this.getLineType(ptr, lineStart, lineEnd, indicesSunToOutside, indicesSunToSun, indicesOutsideToSun);
						}
	
					}
					vertices.flip();
					indicesSunToOutside.flip();
					indicesOutsideToSun.flip();
					indicesSunToSun.flip();
					frame.setLoadedData(vertices, indicesSunToOutside,
							indicesSunToSun, indicesOutsideToSun);
				
	
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
	}
	
	@Deprecated
	private TYPE getType(short[] ptr ,int startLine, int lineEnd) {
		if (ptr[startLine] > 8192 * 1.05)
			return TYPE.OUTSIDE_TO_SUN;
		else if (ptr[lineEnd] > 8192 * 1.05)
			return TYPE.SUN_TO_OUTSIDE;
		else
			return TYPE.SUN_TO_SUN;
	}
	
	@Deprecated
	private double calculateAngleBetween2Vecotrs(double x1, double y1,
			double z1, double x2, double y2, double z2) {
		return (x1 * x2 + y1 * y2 + z1 * z2)
				/ (Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1) * Math.sqrt(x2 * x2
						+ y2 * y2 + z2 * z2));
	}
	
	@Deprecated
	private void addIndex(IntBuffer buffer, int counter) {

			buffer.put(counter);
			buffer.put(counter + 1);
		
	}
	
	@Deprecated
	private int addVertex(FloatBuffer vertices, float x, float y, float z, int counter) {
		vertices.put(x);
		vertices.put(y);
		vertices.put(z);
		return ++counter;
	}

	
	/**
	 * Reads the current fits file. The fits file has to be decompressed before
	 */
	private void readFits() {
		if (!data.isLoaded()) {
			try {
				data.awaitLoaded();
			} catch (InterruptedException e) {
				// do nothing and exit this method
			}
		}
		if (data.isLoaded()) {
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

				// conservative ASSUMPTION: memory is getting wasted here
				FloatBuffer vertices = Buffers
						.newDirectFloatBuffer(ptr.length * 3 + 3);
				IntBuffer indicesSunToOutside = Buffers
						.newDirectIntBuffer(ptr.length * 2);
				IntBuffer indicesSunToSun = Buffers
						.newDirectIntBuffer(ptr.length * 2);
				IntBuffer indicesOutsideToSun = Buffers
						.newDirectIntBuffer(ptr.length * 2);

				int vertexIndex = 0;

				// loop through all lines
				for (int i = 0; i < ptr_nz_len.length; i++) {
					int lineSize = ptr_nz_len[i];
					Point lastP = new Point(vertexIndex, ptr[vertexIndex],
							ptph[vertexIndex], ptth[vertexIndex], l0, b0);
					addPoint(vertices, lastP);
					IntBuffer indexBuffer = getLineType(ptr, vertexIndex,
							vertexIndex + lineSize - 1, indicesSunToOutside,
							indicesSunToSun, indicesOutsideToSun);

					int maxSize = vertexIndex + lineSize;
					vertexIndex++;
					for (; vertexIndex < maxSize; vertexIndex++) {
						Point current = new Point(vertexIndex, ptr[vertexIndex], ptph[vertexIndex],
								ptth[vertexIndex], l0, b0);
						addPoint(vertices, current);
						addLineSegment(lastP, current, indexBuffer);
						lastP = current;
					}
					//line has ended
				}

				vertices.flip();
				indicesSunToOutside.flip();
				indicesOutsideToSun.flip();
				indicesSunToSun.flip();
				frame.setLoadedData(vertices, indicesSunToOutside,
						indicesSunToSun, indicesOutsideToSun);

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
	}

	private static void addLineSegment(Point from, Point to, IntBuffer indices) {
		indices.put(from.index);
		indices.put(to.index);
	}

	private static void addPoint(FloatBuffer vertices, Point p) {
		vertices.put(p.x);
		vertices.put(p.y);
		vertices.put(p.z);
	}

	/**
	 * Helper function for determining the right line type
	 * 
	 * Types: Sun_to_sun Outside_to_sun Sun_to_outside
	 * 
	 * @param ptr
	 * @param startIndex
	 *            index of start point of the fieldline
	 * @param endIndex
	 *            index of the endpoint of the fieldline
	 * @param sto
	 *            sun_to_outside indexbuffer
	 * @param sts
	 *            sun_to_sun indexbuffer
	 * @param ots
	 *            outside_to_sun indexbuffer
	 * @return returns indexbuffer of the type
	 */
	private static IntBuffer getLineType(short[] ptr, int startIndex,
			int endIndex, IntBuffer sto, IntBuffer sts, IntBuffer ots) {
		if (ptr[startIndex] > 8192 * 1.05)
			return ots;
		else if (ptr[endIndex] > 8192 * 1.05)
			return sto;
		else
			return sts;
	}

	/**
	 * Helper class. It represents a point of the fieldline before it is loaded into the buffers
	 * @author Jonas Schwammberger
	 *
	 */
	private class Point {
		float x;
		float y;
		float z;
		int index;

		public Point(int index, short ptr, short ptph, short ptth, double l0,
				double b0) {
			this.index = index;
			double r0 = ptr / 8192.0 * Constants.SunRadius;
			double phi0 = ptph / 32768.0 * 2 * Math.PI;
			double theta0 = ptth / 32768.0 * 2 * Math.PI;

			phi0 -= l0 / 180.0 * Math.PI;
			theta0 += b0 / 180.0 * Math.PI;
			z = (float) (r0 * Math.sin(theta0) * Math.cos(phi0));
			x = (float) (r0 * Math.sin(theta0) * Math.sin(phi0));
			y = (float) (r0 * Math.cos(theta0));
		}

	}

	@Override
	public void run() {
		readData();
	}
	
	public static void main(String[] args) {
		String s = "file:///C:/dev/git/bachelor/tools/FITSFormatter/";
		FileDescriptor f = new FileDescriptor(new Date(0), new Date(1), "fitsOut.fits",0);
		PfssData d = new PfssData(f,s+"fitsOut.fits");
		d.loadData();
		PfssFrame frame = new PfssFrame(f);
		PfssDataDecompressor r = new PfssDataDecompressor(d, frame, null);
		r.readData();
		
	}
}
