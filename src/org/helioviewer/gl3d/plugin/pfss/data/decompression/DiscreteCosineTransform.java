package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * Implementation of the discrete cosine transformation used in this plugin
 * @author Jonas Schwammberger
 *
 */
public class DiscreteCosineTransform {

    public static void inverseTransform(Line[] lines) {
    	for(Line l : lines) {
    		for(int i = 0; i < l.channels.length;i++) {
    			int actualSize = l.size+l.start[i]+l.end[i];
    			float[] idct = inverseTransform(l.channels[i],actualSize);
    			
    			float[] cutOff = new float[l.size];
    			System.arraycopy(idct, l.start[i], cutOff, 0, l.size);
    			l.channels[i] = cutOff;
    		}
    	}
    }

	/**
	 * 
	 * @param value
	 * @param actualSize
	 * @return
	 */
    private static float[] inverseTransform(float[] value,int actualSize)
    {

        double adaptive2 = 2d * actualSize;
        float[] output = new float[actualSize];
        
        for (int k = 0; k < actualSize; k++)
        {
        	
            for (int i = 1; i < 20 && i < value.length; i++)
            {
            	float bla = (float)(value[i] * (Math.cos((2 * k + 1) * i * Math.PI / adaptive2)));
                output[k] += (float)(value[i] * (Math.cos((2 * k + 1) * i * Math.PI / adaptive2)));
            }

            output[k] += value[0] / 2f;
            
        }
        return output;
    }
}
