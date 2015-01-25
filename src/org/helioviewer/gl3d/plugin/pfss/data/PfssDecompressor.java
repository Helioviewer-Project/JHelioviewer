package org.helioviewer.gl3d.plugin.pfss.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Date;

import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.ByteDecoder;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.DiscreteCosineTransform;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.IntermediateLineData;
import org.helioviewer.gl3d.plugin.pfss.data.decompression.UnRar;
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
	 * Reads the PfssData and fills out the frame object
	 */
	public void readData() {
		if (!data.isLoaded()) {
			try {
				data.awaitLoaded();
			} catch (InterruptedException e) {
				// do nothing and exit this method
			}
		}
		if (data.isLoaded() && !frame.isLoaded()) {
			InputStream is = null;
			IntermediateLineData[] lines = null;
			try {
				ByteArrayOutputStream out = UnRar.unrarData(data);
				is = new ByteArrayInputStream(out.toByteArray());
				
				Fits fits = new Fits(is, false);
				BasicHDU hdus[] = fits.read();
				BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];
				double b0 = ((double[]) bhdu.getColumn("B0"))[0];
				double l0 = ((double[]) bhdu.getColumn("L0"))[0];
				byte[] line_length = ((byte[][]) bhdu.getColumn("LINE_LENGTH"))[0];
				byte[] type = ((byte[][]) bhdu.getColumn("TYPE"))[0];
				byte[] startR = ((byte[][]) bhdu.getColumn("StartPointR"))[0];
				byte[] startPhi = ((byte[][]) bhdu.getColumn("StartPointPhi"))[0];
				byte[] startTheta = ((byte[][]) bhdu.getColumn("StartPointTheta"))[0];
				byte[] xRaw = ((byte[][]) bhdu.getColumn("X"))[0];
				byte[] yRaw = ((byte[][]) bhdu.getColumn("Y"))[0];
				byte[] zRaw = ((byte[][]) bhdu.getColumn("Z"))[0];
				
				int[] startRInt = ByteDecoder.decodeAdaptive(startR);
				int[] startPhiInt = ByteDecoder.decodeAdaptive(startPhi);
				int[] startThetaInt = ByteDecoder.decodeAdaptive(startTheta);
				int[] lengths = ByteDecoder.decodeAdaptiveUnsigned(line_length);
				int[] xInt = ByteDecoder.decodeAdaptive(xRaw);
				int[] yInt = ByteDecoder.decodeAdaptive(yRaw);
				int[] zInt = ByteDecoder.decodeAdaptive(zRaw);

				lines = IntermediateLineData.splitToLines(lengths, xInt, yInt, zInt);
				IntermediateLineData.addStartPoint(lines, startRInt, startPhiInt, startThetaInt, l0, b0);
				
				for(int i = 0; i < lines.length;i++) {
					switch(type[i]) {
						case 0:
							multiplyLinear(lines[i],20,5,0,10);
							multiplyLinear(lines[i],75,2,10,8);
							multiplyLinear(lines[i],85,5,18,7);
							multiplyLinear(lines[i],150,20,25,15);
							break;
						case 1:
							multiplyLinear(lines[i],10,4,0,10);
							multiplyLinear(lines[i],60,0,10,8);
							multiplyLinear(lines[i],65,4,18,52);
							break;
					}
				}
				multiply(lines,1000,0);
				DiscreteCosineTransform.inverseTransform(lines);
				
				for(IntermediateLineData l : lines) {
					l.integrate();
				}
				
				//Decompression done.
			} catch (FitsException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
				}

			}
			
			this.subsampleAndConvertToBuffers(lines);
		}	
	}
	
	private static void multiplyLinear(IntermediateLineData l, double start, double increase, int offset, int length)
    {
        	for(int i = 0; i < l.channels.length;i++) {
    			double div =start;
    			float[] channel = l.channels[i];
    			
    			for(int j = offset; j < offset + length &&j < channel.length;j++) {
    				channel[j] = (float)(channel[j] * div);
    				div += increase;
    			}
    		}
    }

    private static void multiply(IntermediateLineData[] lines, double factor, int offset)
    {
    	for(IntermediateLineData l : lines){
    		for(int i = 0; i < l.channels.length;i++) {
    			float[] channel = l.channels[i];
    			for(int j = offset; j < channel.length;j++) {
    				channel[j] = (float)(channel[j] * factor);
    			}
    		}
    	}
    }
	
	/**
	 * subsamples and converts the data to the buffer representation needed for the graphics card.
	 * 
	 * Also does average filtering.
	 * @param lines
	 */
	private void subsampleAndConvertToBuffers(IntermediateLineData[] lines) {
		Point[][] points = new Point[lines.length][];
		
		byte[] types = new byte[lines.length];
		int stoSize= 0;
		int stsSize = 0;
		int otsSize = 0;
		int totalSize = 0;
		for(int i = 0; i < lines.length;i++)
		{
			IntermediateLineData l = lines[i];
			Point[] linePoints = new Point[l.size];

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
						Point average = getAveragePoint(l, j);
						average = average == null ? current : average;
						
						last = average;
						linePoints[nextIndex++] = average;
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
		
		//copy to buffers
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
				line[lineIndex].addPointToBuffer(vertices);
				indexBuffer.put(vertexIndex);
				indexBuffer.put(vertexIndex+1);
				vertexIndex++;
				lineIndex++;
			}
			line[lineIndex].addPointToBuffer(vertices);
			vertexIndex++;
		}

		vertices.flip();
		indicesSunToOutside.flip();
		indicesOutsideToSun.flip();
		indicesSunToSun.flip();
		frame.setLoadedData(vertices, indicesSunToOutside,
				indicesSunToSun, indicesOutsideToSun);
	}
	
	private Point getAveragePoint(IntermediateLineData data, int pointIndex) {
		Point answer = null;
		int startOffset = 0;
		int length = 0;
		if(data.size > PfssSettings.AVERAGE_FILTER_MIN_LINE_SIZE) {
			startOffset = PfssSettings.AVERAGE_FILTER_SIZE/2;
			length = PfssSettings.AVERAGE_FILTER_SIZE;

		} else {
			startOffset = 1;
			length = 3;
		}
		
		if(pointIndex - startOffset < 0) {
			length -=  startOffset - pointIndex;
			startOffset = pointIndex;
		}
			
		float avX = 0;
		float avY = 0;
		float avZ = 0;
		int count = 0;
		for (int i = pointIndex - startOffset; i < (pointIndex - startOffset + length)
				& i < data.size; i++) {
			avX += data.channels[0][i];
			avY += data.channels[1][i];
			avZ += data.channels[2][i];
			count++;
		}

		answer = new Point(avX / count, avY / count, avZ / count);
		
		return answer;
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
		
		public void addPointToBuffer(FloatBuffer vertices) {
			vertices.put(x);
			vertices.put(y);
			vertices.put(z);
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
