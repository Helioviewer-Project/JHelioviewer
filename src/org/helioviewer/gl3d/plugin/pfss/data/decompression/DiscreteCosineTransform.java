package org.helioviewer.gl3d.plugin.pfss.data.decompression;

import java.util.HashMap;

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
    	HashMap<Integer,float[][]> coefficientCache = new HashMap<>(lines.length);
    	for(IntermediateLineData l : lines) {
    		float[][] dcCache = coefficientCache.get(l.size);
    		if(dcCache == null) {
    			dcCache = new float[l.size][l.size];
    			coefficientCache.put(l.size, dcCache);
    		}

    		for(int i = 0; i < l.channels.length;i++) {
    			int actualSize = l.size;
    			float[] idct = inverseTransform(l.channels[i], actualSize,dcCache);
    			l.channels[i] = idct;
    		}
    	}
    }

	/**
	 * 
	 * @param value
	 * @param actualSize
	 * @param dcCache
	 * @return
	 */
    private static float[] inverseTransform(float[] value, int actualSize, float[][] dcCache)
    {

        double adaptive2 = 2d * actualSize;
        float[] output = new float[actualSize];
        
        for (int k = 0; k < actualSize; k++)
        {
            for (int i = 1; i < value.length; i++)
            {
            	if(dcCache[k][i] == 0)
            		dcCache[k][i] = (float)(Math.cos((2 * k + 1) * i * Math.PI / adaptive2));
            		
                output[k] += value[i] * dcCache[k][i];
            }

            output[k] += value[0] / 2f;
            
        }
        return output;
    }
}
