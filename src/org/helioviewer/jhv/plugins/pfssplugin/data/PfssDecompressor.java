package org.helioviewer.jhv.plugins.pfssplugin.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import org.helioviewer.jhv.plugins.pfssplugin.data.decompression.ByteDecoder;
import org.helioviewer.jhv.plugins.pfssplugin.data.decompression.DecompressedLine;
import org.helioviewer.jhv.plugins.pfssplugin.data.decompression.DecompressedPoint;
import org.helioviewer.jhv.plugins.pfssplugin.data.decompression.IntermediateLineData;
import org.helioviewer.jhv.plugins.pfssplugin.data.decompression.LineType;
import org.helioviewer.jhv.plugins.pfssplugin.data.decompression.UnRar;

import com.jogamp.common.nio.Buffers;

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
			ArrayList<DecompressedLine> decompressedLines = null;
			try {
				ByteArrayOutputStream out = UnRar.unrarData(data);
				is = new ByteArrayInputStream(out.toByteArray());
				
				Fits fits = new Fits(is, false);
				BasicHDU hdus[] = fits.read();
				BinaryTableHDU bhdu = (BinaryTableHDU) hdus[1];

				float b0 = bhdu.getHeader().getFloatValue("B0");
				float l0 = bhdu.getHeader().getFloatValue("L0");
				float Q1 = bhdu.getHeader().getFloatValue("Q1");
				float Q2 = bhdu.getHeader().getFloatValue("Q2");
				float Q3 = bhdu.getHeader().getFloatValue("Q3");
				
				byte[] line_length = ((byte[][]) bhdu.getColumn("LEN"))[0];
				byte[] xRaw = ((byte[][]) bhdu.getColumn("X"))[0];
				byte[] yRaw = ((byte[][]) bhdu.getColumn("Y"))[0];
				byte[] zRaw = ((byte[][]) bhdu.getColumn("Z"))[0];
				
				int[] lengths = ByteDecoder.decodeAdaptiveUnsigned(line_length);
				int[] xInt = ByteDecoder.decodeAdaptive(xRaw);
				int[] yInt = ByteDecoder.decodeAdaptive(yRaw);
				int[] zInt = ByteDecoder.decodeAdaptive(zRaw);

				IntermediateLineData[] lines = IntermediateLineData.splitToLines(lengths, xInt, yInt, zInt);
				for(IntermediateLineData l : lines)
					l.decodePrediction(Q1, Q2, Q3);
			
				decompressedLines = new ArrayList<DecompressedLine>(lines.length);
				for(IntermediateLineData line : lines)
					decompressedLines.add(new DecompressedLine(line));
				
	            this.subsampleAndConvertToBuffers(decompressedLines, l0, b0);
			}
			catch (FitsException | IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					is.close();
				}
				catch (Exception e)
				{
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
	private void subsampleAndConvertToBuffers(ArrayList<DecompressedLine> lines, float _l0, float _b0)
	{
		int stoSize= 0;
		int stsSize = 0;
		int otsSize = 0;
		int totalSize = 0;
		for(int i = 0; i < lines.size();i++)
		{
			DecompressedLine currentLine = lines.get(i);
			ArrayList<DecompressedPoint> subsampledPoints = new ArrayList<>();

			DecompressedPoint last = currentLine.getPoint(0);
			subsampledPoints.add(last);
			
			for(int j = 1; j < currentLine.getSize();j++) {
				DecompressedPoint current = currentLine.getPoint(j);
				
				/*if((j + 1)< currentLine.getSize()) {
					//check if point should be in line or not
					DecompressedPoint next = currentLine.getPoint(j+1);
					boolean colinear = current.AngleTo(next, last) > PfssSettings.ANGLE_OF_LOD;
					colinear = false;
					if(!colinear) {
						DecompressedPoint average = getAveragePoint(currentLine, j);
						average = average == null ? current : average;
						
						last = average;
						subsampledPoints.add(average);
					}
					
				}
				else*/
				{
					//last point, always add
					subsampledPoints.add(current);
				}
			}
			
			DecompressedLine subsampledLine = new DecompressedLine(subsampledPoints,currentLine.getType());
			lines.set(i, subsampledLine);
			switch(subsampledLine.getType())
			{
				case OUTSIDE_TO_SUN:
					otsSize += subsampledLine.getSize()-1;
					break;
				case SUN_TO_OUTSIDE:
					stoSize += subsampledLine.getSize()-1;
					break;
				case SUN_TO_SUN:
					stsSize += subsampledLine.getSize()-1;
					break;
				default:
					break;
			}
			
			totalSize += subsampledLine.getSize();
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
		for(int i = 0; i < lines.size();i++) {
			DecompressedLine line = lines.get(i);
			IntBuffer indexBuffer = getLineType(line.getType(), indicesSunToOutside,
					indicesSunToSun, indicesOutsideToSun);
			
			int pointIndex = 0;
			while(pointIndex+1 < line.getSize())
			{
				DecompressedPoint point = line.getPoint(pointIndex);
				vertices.put(point.getX());
				vertices.put(point.getY());
				vertices.put(point.getZ());
				indexBuffer.put(vertexIndex);
				indexBuffer.put(vertexIndex+1);
				vertexIndex++;
				pointIndex++;
			}
			DecompressedPoint point = line.getPoint(pointIndex);
			vertices.put(point.getX());
			vertices.put(point.getY());
			vertices.put(point.getZ());
			vertexIndex++;
		}

		vertices.flip();
		indicesSunToOutside.flip();
		indicesOutsideToSun.flip();
		indicesSunToSun.flip();
		frame.setLoadedData(vertices, indicesSunToOutside,
				indicesSunToSun, indicesOutsideToSun,
				_l0, _b0);
	}
	
	/**
	 * Returns the average point from the line.
	 * @param line
	 * @param pointIndex
	 * @return
	 */
	/*private DecompressedPoint getAveragePoint(DecompressedLine line, int pointIndex) {
		DecompressedPoint answer = null;
		int startOffset = 0;
		int length = 0;

		startOffset = 1;
		length = PfssSettings.SMOOTH_FILTER_SIZE;
		if(pointIndex - startOffset < 0) {
			length -=  startOffset - pointIndex;
			startOffset = pointIndex;
		}
			
		float avX = 0;
		float avY = 0;
		float avZ = 0;
		int count = 0;
		for (int i = pointIndex - startOffset; i < (pointIndex - startOffset + length)
				& i < line.getSize(); i++) {
			DecompressedPoint current = line.getPoint(i);
			avX += current.getX();
			avY += current.getY();
			avZ += current.getZ();
			count++;
		}

		answer = new DecompressedPoint(avX / count, avY / count, avZ / count);
		
		return answer;
	}*/

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
	private static IntBuffer getLineType(LineType type, IntBuffer sto, IntBuffer sts, IntBuffer ots) {
		switch(type)
		{
			case OUTSIDE_TO_SUN:
				return ots;
			case SUN_TO_OUTSIDE:
				return sto;
			case SUN_TO_SUN:
				return sts;
			default:
				return null;
		}
	}

	@Override
	public void run()
	{
		readData();
	}
}
