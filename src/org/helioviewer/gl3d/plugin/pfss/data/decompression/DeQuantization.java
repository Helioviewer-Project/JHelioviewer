package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * 
 * @author Jonas Schwammberger
 *
 */
public class DeQuantization {
	

	
    public static void multiplyLinear(Line[] lines, double factor, int offset)
    {
    	for(Line l : lines){
    		for(int i = 0; i < l.channels.length;i++) {
    			double div = factor;
    			float[] channel = l.channels[i];
    			
    			for(int j = offset; j < channel.length;j++) {
    				channel[j] = (float)(channel[j] * div);
    				div += factor;
    			}
    		}
    	}
     
    }
    
    public static void multiplyLinear(Line[] lines, double start, double increase, int offset, int length)
    {
        for (Line l : lines)
        {
        	for(int i = 0; i < l.channels.length;i++) {
    			double div =start;
    			float[] channel = l.channels[i];
    			
    			for(int j = offset; j < offset + length &&j < channel.length;j++) {
    				channel[j] = (float)(channel[j] * div);
    				div += increase;
    			}
    		}
        	
        }
    }

    public static void multiply(Line[] lines, double factor, int offset)
    {
    	for(Line l : lines){
    		for(int i = 0; i < l.channels.length;i++) {
    			float[] channel = l.channels[i];
    			for(int j = offset; j < channel.length;j++) {
    				channel[j] = (float)(channel[j] * factor);
    			}
    		}
    	}
    }
    
    public static void multiplyPoint(Line[] lines, double factor, int index) {
    	for(Line l : lines){
    		for(int i = 0; i < l.channels.length;i++) {
    			if(index < l.channels[i].length)
    				l.channels[i][index] = (float)(l.channels[i][index] * factor);
    		}
    	}
    }
	
}
