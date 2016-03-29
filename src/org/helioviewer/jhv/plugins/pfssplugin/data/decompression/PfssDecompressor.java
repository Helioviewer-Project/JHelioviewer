package org.helioviewer.jhv.plugins.pfssplugin.data.decompression;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssCompressed;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressed;

import com.jogamp.common.nio.Buffers;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

/**
 * Reads in Memory PfssData and writes PfssFrames. Supports running in its own
 * thread for asynchronous loading
 */
public class PfssDecompressor
{
	/**
	 * Reads the PfssData and fills out the frame object
	 */
	public static void decompress(PfssCompressed _src, PfssDecompressed _dest)
	{
		if (!_src.isLoaded())
			return;

		if (_dest.isDataAssigned())
			return;
		
		try (InputStream is = new ByteArrayInputStream(UnRar.unrarData(_src).toByteArray()))
		{
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
			for (IntermediateLineData l : lines)
				l.decodePrediction(Q1, Q2, Q3);

			ArrayList<DecompressedLine> decompressedLines = new ArrayList<>(lines.length);
			for (IntermediateLineData line : lines)
				decompressedLines.add(new DecompressedLine(line));

			convertToBuffers(decompressedLines, l0, b0, _dest);
		}
		catch (FitsException | IOException e)
		{
			Telemetry.trackException(e);
		}
	}

	/**
	 * subsamples and converts the data to the buffer representation needed for
	 * the graphics card.
	 * 
	 * Also does average filtering.
	 * 
	 * @param lines
	 */
	private static void convertToBuffers(ArrayList<DecompressedLine> lines, float _l0, float _b0, PfssDecompressed _frame)
	{
		int stoSize = 0;
		int stsSize = 0;
		int otsSize = 0;
		int totalSize = 0;
		for (int i = 0; i < lines.size(); i++)
		{
			DecompressedLine currentLine = lines.get(i);

			DecompressedLine subsampledLine = new DecompressedLine(currentLine.points, currentLine.getType());
			lines.set(i, subsampledLine);
			switch (subsampledLine.getType())
			{
				case OUTSIDE_TO_SUN:
					otsSize += subsampledLine.getSize() - 1;
					break;
				case SUN_TO_OUTSIDE:
					stoSize += subsampledLine.getSize() - 1;
					break;
				case SUN_TO_SUN:
					stsSize += subsampledLine.getSize() - 1;
					break;
				default:
					break;
			}

			totalSize += subsampledLine.getSize();
		}

		// copy to buffers
		FloatBuffer vertices = Buffers.newDirectFloatBuffer(totalSize * 3);
		IntBuffer indicesSunToOutside = Buffers.newDirectIntBuffer(stoSize * 2);
		IntBuffer indicesSunToSun = Buffers.newDirectIntBuffer(stsSize * 2);
		IntBuffer indicesOutsideToSun = Buffers.newDirectIntBuffer(otsSize * 2);

		int vertexIndex = 0;
		for (DecompressedLine line : lines)
		{
			IntBuffer indexBuffer = getLineType(line.getType(), indicesSunToOutside, indicesSunToSun, indicesOutsideToSun);

			int pointIndex = 0;
			while (pointIndex + 1 < line.getSize())
			{
				DecompressedPoint point = line.getPoint(pointIndex);
				vertices.put(point.getX());
				vertices.put(point.getY());
				vertices.put(point.getZ());
				indexBuffer.put(vertexIndex);
				indexBuffer.put(vertexIndex + 1);
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
		_frame.setLoadedData(vertices, indicesSunToOutside, indicesSunToSun, indicesOutsideToSun, _l0, _b0);
	}

	/**
	 * Helper function for determining the line type
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
	private static IntBuffer getLineType(LineType type, IntBuffer sto, IntBuffer sts, IntBuffer ots)
	{
		switch (type)
		{
			case OUTSIDE_TO_SUN:
				return ots;
			case SUN_TO_OUTSIDE:
				return sto;
			case SUN_TO_SUN:
				return sts;
			default:
				throw new RuntimeException("Unexpected line type");
		}
	}
}
