package org.helioviewer.gl3d.plugin.pfss;

public class DiscreteCosinusTransform {
	public static final int size = 8;
    private static final double size2 = 2d*size;
    private static final float halfSize = 2/(float)size;
    private static final float[] coefficients = {1,1,2,2,8,16,24,32,20,30,40,50,60,80,100,120};

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
        //dequantization
        for (int i = 0; i < size; i++)
        {
            if (coefficients[i] != 1)
            {
                value[i] = (float)value[i] * coefficients[i];
            }
        }

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
