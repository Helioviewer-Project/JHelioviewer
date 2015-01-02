package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * Implementation of the discrete cosine transformation used in this plugin
 * @author Jonas Schwammberger
 *
 */
public class DiscreteCosineTransform {

	/**
	 * calculates the inverse DCT for all channels of all lines.
	 * @param lines lines to decompress
	 */
    public static void inverseTransform(IntermediateLineData[] lines) {
    	for(IntermediateLineData l : lines) {
    		for(int i = 0; i < l.channels.length;i++) {
    			int actualSize = l.size;
    			float[] idct = inverseTransform(l.channels[i], actualSize);
    			l.channels[i] = idct;
    		}
    	}
    }

	/**
	 * 
	 * @param value
	 * @param actualSize
	 * @return
	 */
    private static float[] inverseTransform(float[] value, int actualSize)
    {

        double adaptive2 = 2d * actualSize;
        float[] output = new float[actualSize];
        
        for (int k = 0; k < actualSize; k++)
        {
        	
            for (int i = 1; i < value.length; i++)
            {
            	float bla = (float)(value[i] * (Math.cos((2 * k + 1) * i * Math.PI / adaptive2)));
                output[k] += (float)(value[i] * (Math.cos((2 * k + 1) * i * Math.PI / adaptive2)));
            }

            output[k] += value[0] / 2f;
            
        }
        return output;
    }
}
