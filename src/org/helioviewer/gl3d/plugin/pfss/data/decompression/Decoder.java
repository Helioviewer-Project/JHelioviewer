package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * 
 * @author Jonas Schwammberger
 *
 */
public class Decoder {
	public static final int continueFlag = 128;
    public static final int signFlag = 64;
    public static final int maxValue = 63;
    public static final int minValue = -64;
    public static final int dataBitCount = 7;
	
	public static int[] decodeRLE(int[] input, int length) {
		int[] output = new int[length];
		System.arraycopy(input,1,output,0,input.length); //skip length description
		
		return output;
	}
	
	 public static int[] decodeAdaptive(byte[] data)
     {
         //count length
		 int length = calcLength(data);
         int[] output = new int[length];
         int outIndex = 0;
         for (int i = 0; i < data.length; i++)
         {
             byte current = data[i];
             int value = (short)(current & (signFlag-1));
             int minus = -(current & signFlag);
             boolean run = (current & continueFlag) != 0;
             while (run)
             {
                 if (run) 
                 { 
                     i++;
                     current = data[i];
                 }
                 run = (current & continueFlag) != 0;
                 minus <<= dataBitCount;
                 value <<= dataBitCount;
                 value += current & (continueFlag - 1);
             }
             output[outIndex++] = (value+minus);
         }

         return output;
     }
	 
	 public static int[] decodeAdaptiveUnsigned(byte[] data) {
		 int length = calcLength(data);
         int[] output = new int[length];
         int outIndex = 0;
         for (int i = 0; i < data.length; i++)
         {
             byte current = data[i];
             int bla = current & (continueFlag-1);
             int value = (int)(current & (continueFlag-1));
             boolean run = (current & continueFlag) != 0;
             while (run)
             {
                 if (run) 
                 { 
                     i++;
                     current = data[i];
                 }
                 run = (current & continueFlag) != 0;
                 value <<= dataBitCount;
                 value += current & (continueFlag - 1);
             }
             output[outIndex++] = value;
         }

         return output;
	 }
	 
	 private static int calcLength(byte[] data) {
		 int out = 0;
		 
		 for(int i = 0; i < data.length;i++) {
			 if((data[i] & continueFlag) == 0)
				 out++;
		 }
		 return out;
	 }
	 
	 public static void main(String[] args) {
		 byte[] data = new byte[]{2,(byte)129,0,(byte)129,127};
		 int[] adaptive = decodeAdaptiveUnsigned(data);
		 System.out.println("");
	 }
}
