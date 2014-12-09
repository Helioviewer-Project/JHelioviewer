package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * 
 * @author Jonas Schwammberger
 *
 */
public class DeQuantization {
	
	public static float[] toFloat(int[] data) {
		float[] out = new float[data.length];
		for(int i = 0; i < data.length;i++) {
			out[i] = data[i];
		}
		return out;
	}
	
    public static void MultiplyLinearExtra(float[] data, double factor, int offset, int start)
    {

            double div = factor*start;
            for (int i = offset; i < data.length; i++)
            {
            	data[i] = (float)(data[i] * div);
                div += factor;
            }
     
    }

    public static void MultiplyExtra(float[] data, double factor, int offset)
    {
        	for (int i = offset; i < data.length; i++)
            {

        		data[i] = (float)(data[i] * factor);
            }
    }
	
}
