package org.helioviewer.jhv.plugins.pfssplugin.data.decompression;

/**
 * This class decodes the bytes of a PFSSData file.
 * 
 * @author Jonas Schwammberger
 */
public class ByteDecoder {
	public static final int FLAG_CONTINUE = 128;
	public static final int FLAG_SIGN = 64;
	public static final int DATA_BIT_COUNT = 7;

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
			int value = (short) (current & (FLAG_SIGN - 1));
			int minus = -(current & FLAG_SIGN);
			
			//add encoded bytes as long as the continue flag is set.
			boolean run = (current & FLAG_CONTINUE) != 0;
			while (run) {
				current = data[++i];
				run = (current & FLAG_CONTINUE) != 0;
				minus <<= DATA_BIT_COUNT;
				value <<= DATA_BIT_COUNT;
				value += current & (FLAG_CONTINUE - 1);
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
			int value = (int) (current & (FLAG_CONTINUE - 1));
			
			//add encoded bytes as long as the continue flag is set.
			boolean run = (current & FLAG_CONTINUE) != 0;
			while (run) {
				current = data[++i];
				run = (current & FLAG_CONTINUE) != 0;
				value <<= DATA_BIT_COUNT;
				value += current & (FLAG_CONTINUE - 1);
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
	 * @return number of decoded values
	 */
	private static int calcLength(byte[] data) {
		int out = 0;

		for (int i = 0; i < data.length; i++) {
			if ((data[i] & FLAG_CONTINUE) == 0)
				out++;
		}
		return out;
	}
}
