package org.helioviewer.gl3d.plugin.pfss.data.decompression;

/**
 * 
 * @author Jonas Schwammberger
 *
 */
public class DeQuantization {
	

	
    public static void multiplyLinear(Line[] lines, double factor, int offset, int start)
    {

    	for(Line l : lines){
    		for(int i = 0; i < l.channels.length;i++) {
    			double div = factor*start;
    			float[] channel = l.channels[i];
    			
    			for(int j = offset; j < channel.length;j++) {
    				channel[j] = (float)(channel[j] * div);
    				div += factor;
    			}
    		}
    	}
     
    }

    public static void multiply(Line[] lines, double factor)
    {
    	for(Line l : lines){
    		for(int i = 0; i < l.channels.length;i++) {
    			float[] channel = l.channels[i];
    			for(int j = 0; j < channel.length;j++) {
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
