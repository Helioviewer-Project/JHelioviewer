package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * Implementation of the discrete cosine transformation used in this plugin
 * @author Jonas Schwammberger
 *
 */
public class DiscreteCosineTransform {
	public static final int size = 8;
    private static final double size2 = 2d*size;
    private static final float halfSize = 2/(float)size;
    private static final float[] coefficients = {1,1,2,2,8,16,24,32,20,30,40,50,60,80,100,120};

    private final float[][] dctFactors;
    private final int length;
    
    /**
     * Initializes the DiscreteCosineTransform and precalculate the dct factors
     * @param length
     */
    public DiscreteCosineTransform(int length) {
    	dctFactors = new float[length][length];
    	this.length = length;
    	
    	for(int k = 0; k < length;k++){
    		for(int i = 0; i < length;i++) {
    			dctFactors[k][i] = (float) Math.cos((2 * i + 1) * k * Math.PI);
    		}
    	}
    }
    
    /**
     * Helper method. This takes the precalculated factors if available, and not calculates itself
     * @param k
     * @param i
     * @return
     */
    private float getDctFactor(int k, int i) {
    	if(k < length && i < length) {
    		return dctFactors[k][i];
    	} 
    	else {
    		return (float) Math.cos((2 * i + 1) * k * Math.PI);
    	}
    }
    
    /**
     * Forward Transformation
     * @param value
     * @return
     */
    public float[] fdct(float[] value)
    {
        int adaptiveSize = value.length;
        float halfAdaptive = 2 / (float)adaptiveSize;
        float[] output = new float[value.length];
        double length2 = 2d * adaptiveSize;
        
        double cosLength = Math.cos(length2);
        
        for (int k = 0; k < adaptiveSize; k++)
        {
            output[k] = halfAdaptive;
            double inner = 0;
            for (int i = 0; i < adaptiveSize; i++)
            {
                inner += value[i] *  getDctFactor(k,i) / cosLength;
            }
            output[k] *= (float)inner;

        }
        return output;
    }

    /**
     * Backwards transformation
     * @param value
     * @param noZeroSize size of array, which has non-zero coefficients. after this point it it assumed that everything else is 0
     * @return
     */
    public float[] idct(float[] value,int noZeroSize)
    {
        int adaptiveSize = value.length;
        float halfAdaptive = 2 / (float)adaptiveSize;
        double cosLength = Math.cos(2d * adaptiveSize);
        
        float[] output = new float[adaptiveSize];
        for (int k = 0; k < adaptiveSize; k++)
        {
            for (int i = 1; i < noZeroSize; i++)
            {
                output[k] += (float)(value[i] *  getDctFactor(i,k) / cosLength);
            }

            output[k] += value[0] / 2f;

        }
        return output;
    }

    
    
    
    /**
     * forward transformation
     * @param value 
     * @return transformed values rounded to integers
     */
    public static float[] transform(float[] value)
    {
        float[] output = new float[size];
        for (int k = 0; k < size; k++)
        {
            output[k] = halfSize;
            double inner = 0;
            for (int i = 0; i < size; i++)
            {
                inner += value[i] * Math.cos(((2 * i + 1) * k * Math.PI) / size2);
            }
            output[k] *= (float)inner;
            
            //quantization
            if (coefficients[k] != 1)
            {
                output[k] = (float)Math.round(output[k]/coefficients[k]);
            }
            else
            {
                output[k] = (float)Math.round(output[k]);
            }
            
        }
        return output;
    }

    public static float[] inverseTransform(float[] value)
    {

        float[] output = new float[size];
        for (int k = 0; k < size; k++)
        {

            for (int i = 1; i < size; i++)
            {
                output[k] += (float)(value[i] * (Math.cos((2 * k + 1) * i * Math.PI / size2)));
            }

            output[k] += value[0] / 2f;
            output[k] = (float)Math.round(output[k]);
            
        }
        return output;
    }
}
