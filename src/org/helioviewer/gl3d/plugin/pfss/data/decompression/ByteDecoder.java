package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * This class decodes the bytes of a PFSSData file.
 * 
 * @author Jonas Schwammberger
 */
public class ByteDecoder {
	public static final int continueFlag = 128;
	public static final int signFlag = 64;
	public static final int maxValue = 63;
	public static final int minValue = -64;
	public static final int dataBitCount = 7;

	/**
	 * Decodes the length information.
	 * 
	 * @param input
	 *            RLE Encoded data
	 * @param length
	 *            actual length of the data
	 * @return
	 */
	public static int[] decodeLength(int[] input, int length) {
		int[] output = new int[length];
		System.arraycopy(input, 1, output, 0, input.length); // skip length of encoded data

		return output;
	}

	/**
	 * decode the adaptive precision encoded data.
	 * 
	 *  Note: this implementation uses integers. The current compression algorithm only saves values which an integer is able to hold.
	 * @param data encoded byte array
	 * @return decoded array
	 */
	public static int[] decodeAdaptive(byte[] data) {
		int length = calcLength(data);
		int[] output = new int[length];
		int outIndex = 0;
		
		//for each encoded byte
		for (int i = 0; i < data.length; i++) {
			byte current = data[i];
			int value = (short) (current & (signFlag - 1));
			int minus = -(current & signFlag);
			
			//add encoded bytes as long as the continue flag is set.
			boolean run = (current & continueFlag) != 0;
			while (run) {
				current = data[i++];
				run = (current & continueFlag) != 0;
				minus <<= dataBitCount;
				value <<= dataBitCount;
				value += current & (continueFlag - 1);
			}
			output[outIndex++] = (value + minus);
		}

		return output;
	}

	/**
	 * decode the adaptive precision encoded data.
	 * the encoded data is unsigned.
	 * @param data encoded byte array
	 * @return decoded values
	 */
	public static int[] decodeAdaptiveUnsigned(byte[] data) {
		int length = calcLength(data);
		int[] output = new int[length];
		int outIndex = 0;
		for (int i = 0; i < data.length; i++) {
			byte current = data[i];
			int value = (int) (current & (continueFlag - 1));
			
			//add encoded bytes as long as the continue flag is set.
			boolean run = (current & continueFlag) != 0;
			while (run) {
				current = data[i++];
				run = (current & continueFlag) != 0;
				value <<= dataBitCount;
				value += current & (continueFlag - 1);
			}
			output[outIndex++] = value;
		}

		return output;
	}

	/**
	 * Helper method to find out the number of encoded values
	 * 
	 * the byte which marks the end of a value does not have the continue flag set.
	 * 
	 * @param data encoded data
	 * @return
	 */
	private static int calcLength(byte[] data) {
		int out = 0;

		for (int i = 0; i < data.length; i++) {
			if ((data[i] & continueFlag) == 0)
				out++;
		}
		return out;
	}
}
