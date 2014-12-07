package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * 
 * @author Jonas Schwammberger
 *
 */
public class Decoder {
	

	public static int[] decodeAdaptive(byte[] input) {
		int[] output = null;
		
		return output;
	}
	
	private static int countContinues(byte[] input) {
		return 0;
	}
	
	public static int[] decodeRLE(int[] input, int length) {
		int[] output = new int[length];
		System.arraycopy(input,1,output,0,input.length); //skip length description
		
		return output;
	}
}
