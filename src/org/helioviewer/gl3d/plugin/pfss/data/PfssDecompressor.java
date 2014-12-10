package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.DeQuantization;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.Decoder;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.DiscreteCosineTransform;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.Line;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.UnRar;
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
public class PfssDecompressor implements Runnable {
	private final PfssData data;
	private final PfssFrame frame;

	public PfssDecompressor(PfssData data, PfssFrame frame) {
		this.data = data;
		this.frame = frame;
	}

	/**
	 * Reads the Pfss Data and fills out the frame object
	 */
	public void readData() {
		
		this.readFits();
		//readOldfits();
		
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
				ByteArrayOutputStream out = UnRar.unrarData(data);
				is = new ByteArrayInputStream(out.toByteArray());
				
				Fits fits = new Fits(is, false);
				BasicHDU hdus[] = fits.read();
				BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
				byte[] startEnd = ((byte[][]) bhdu.getColumn("START_END"))[0];
				byte[] line_length = ((byte[][]) bhdu.getColumn("LINE_LENGTH"))[0];
				byte[] xRaw = ((byte[][]) bhdu.getColumn("X"))[0];
				byte[] yRaw = ((byte[][]) bhdu.getColumn("Y"))[0];
				byte[] zRaw = ((byte[][]) bhdu.getColumn("Z"))[0];
				
				int[] startEndPoints = Decoder.decodeAdaptiveUnsigned(startEnd);
				int[] lengths = Decoder.decodeAdaptiveUnsigned(line_length);
				int[] xInt = Decoder.decodeAdaptive(xRaw);
				int[] yInt = Decoder.decodeAdaptive(yRaw);
				int[] zInt = Decoder.decodeAdaptive(zRaw);

				Line[] lines = Line.splitToLines(lengths, startEndPoints, xInt, yInt, zInt);
				
				//DeQuantization.MultiplyLinear(lines, 360, 1, 1);
				DeQuantization.Multiply(lines, 1000);
				DeQuantization.MultiplyPoint(lines, 800,0);
				
				DiscreteCosineTransform.inverseTransform(lines);
				
				
				//subsample for low-end graphic cards. Also count how many points there are for each line type
				Point[][] points = new Point[lines.length][];
				byte[] types = new byte[lines.length];
				int stoSize= 0;
				int stsSize = 0;
				int otsSize = 0;
				int totalSize = 0;
				for(int i = 0; i < lines.length;i++)
				{
					Line l = lines[i];
					
					Point[] linePoints = new Point[l.size];

					/*int nextIndex = 0;
					for(int j = 0; j < l.size;j+=2) {
						linePoints[nextIndex++] = new Point(l.channels[0][j],l.channels[1][j],l.channels[2][j]);
					}*/

					Point last = new Point(l.channels[0][0],l.channels[1][0],l.channels[2][0]);
					linePoints[0] = last;
					int nextIndex = 1;
					
					for(int j = 1; j < l.size;j++) {
						Point current = new Point(l.channels[0][j],l.channels[1][j],l.channels[2][j]);
						if((j + 1)< l.size) {
							//check if point should be in line or not
							Point next = new Point(l.channels[0][j+1],l.channels[1][j+1],l.channels[2][j+1]);
							boolean colinear = current.AngleTo(next, last) > PfssSettings.ANGLE_OF_LOD;
							
							if(!colinear) {
								last = current;
								linePoints[nextIndex++] = current;
							}
							
						} else {
							//last point, always add
							linePoints[nextIndex++] = current;
						}
					}
					
					//check line type
					double mag0 = linePoints[0].magnitude();
					if(mag0 < Constants.SunRadius*1.05) {
						double mag1 = linePoints[nextIndex-1].magnitude();
						if(mag1 > Constants.SunRadius*1.05) {
							stoSize += nextIndex-1;
							types[i] = 0;
						} else {
							stsSize += nextIndex-1;
							types[i] = 1;
						}
					}
					else {
						otsSize += nextIndex-1;
						types[i] = 2;
					}
					totalSize += nextIndex;
					points[i] = linePoints;
				}
				
				FloatBuffer vertices = Buffers
						.newDirectFloatBuffer(totalSize * 3 );
				IntBuffer indicesSunToOutside = Buffers
						.newDirectIntBuffer(stoSize * 2);
				IntBuffer indicesSunToSun = Buffers
						.newDirectIntBuffer(stsSize * 2);
				IntBuffer indicesOutsideToSun = Buffers
						.newDirectIntBuffer(otsSize * 2);

				int vertexIndex = 0;
				int ots = 0;
				int sts = 0;
				int sto = 0;
				for(int i = 0; i < points.length;i++) {
					Point[] line = points[i];
					IntBuffer indexBuffer = getLineType(types[i], indicesSunToOutside,
							indicesSunToSun, indicesOutsideToSun);
					
					int lineIndex = 0;
					while(lineIndex+1 < line.length && line[lineIndex+1] != null)
					{
						addPoint(vertices,line[lineIndex]);
						addLineSegment(vertexIndex, vertexIndex+1, indexBuffer);
						vertexIndex++;
						lineIndex++;
					}
					addPoint(vertices,line[lineIndex]);
					vertexIndex++;
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
			} catch (IOException e) {
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
	

	private static void addLineSegment(int from, int to, IntBuffer indices) {
		indices.put(from);
		indices.put(to);
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
	private static IntBuffer getLineType(byte type, IntBuffer sto, IntBuffer sts, IntBuffer ots) {
		if (type >= 2)
			return ots;
		else if (type == 0)
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
		
		public Point(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public double magnitude() {
			double xi = x;
			double yi = y;
			double zi = z;
			return Math.sqrt(xi*xi+yi*yi+zi*zi);
		}
		
		public double AngleTo(Point next,Point before)
        {
            return calculateAngleBetween2Vecotrs(next.x - x,
                                                    next.y - y, next.z - z, x - before.x, y - before.y, z
                                                                                 - before.z);
        }
		
		private double calculateAngleBetween2Vecotrs(double x1, double y1, double z1, double x2, double y2, double z2)
        {
            return (x1 * x2 + y1 * y2 + z1 * z2)
                                         / (Math.sqrt(x1 * x1 + y1 * y1 + z1 * z1) * Math.sqrt(x2 * x2
                                                                     + y2 * y2 + z2 * z2));
        }
		
		

	}
	


	@Override
	public void run() {
		readData();
	}
	
	public static void main(String[] args) {
		String s = "file:///C:/Users/Jonas%20Schwammberger/Documents/GitHub/PFSSCompression/test/temp/";
		FileDescriptor f = new FileDescriptor(new Date(0), new Date(1), "test.rar",0);
		PfssData d = new PfssData(f,s+"test.rar");
		d.loadData();
		PfssFrame frame = new PfssFrame(f);
		PfssDecompressor r = new PfssDecompressor(d, frame);
		r.readData();
		
	}
}
