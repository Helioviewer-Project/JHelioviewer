package org.helioviewer.gl3d.plugin.pfss.data.decompression;

import org.helioviewer.base.physics.Constants;

public class Line {
	public float[][] channels;

	public float[] startPoint;
	public float[] pca;
	public float[] means;
	
	public int size;
	
	
	public void backwardsPCA() {
		float[] copyX = new float[size];
		float[] copyY = new float[size];
		float[] copyZ = new float[size];
		
		System.arraycopy(channels[0], 0, copyX, 0, size);
		System.arraycopy(channels[1], 0, copyY, 0, size);
		System.arraycopy(channels[2], 0, copyZ, 0, size);
		
		for(int i = 0; i < 3;i++) {
			float[] channel = channels[i];
			int idx = i*3;
			for(int j = 0; j < channel.length;j++) {
				channel[j] = copyX[j] * pca[idx]+copyY[j]*pca[idx+1]+copyZ[j]*pca[idx+2];
				channel[j] += means[i];
			}
		}
		
		
	}
	
	/**
	 * Integrates the lines. 
	 */
	public void integrate() {
		
		for(int i = 0; i < startPoint.length;i++){
			float[] newChannel = new float[channels[i].length+1];
			
			float before = startPoint[i];
			newChannel[0] = before;
			for(int j = 1; j < newChannel.length;j++) {
				newChannel[j] = channels[i][j-1]+before;
				before = newChannel[j];
			}
			channels[i] = newChannel;
		}
		size++;
	}
	
	/**
	 * add the starting point to each line. The starting point will be converted from spherical to euler coodinate system.
	 * @param lines all lines
	 * @param radius all radii of the startpoints
	 * @param phi all phi of the startpoins
	 * @param theta all theta of the startpoints
	 * @param l0
	 * @param b0
	 */
	public static void addStartPoint(Line[] lines, int[] radius, int[] phi, int[] theta, double l0, double b0) {
		for(int i = 0; i < lines.length;i++) {
			Line l = lines[i];
			/*
			 * Not all values have been sent. All zeroes at the end of radius[], phi[] or theta[] have been cropped
			 */
			int rawR = i < radius.length ? radius[i] : 0;
			int rawPhi = i < phi.length ? phi[i] : 0;
			int rawTheta = i < theta.length ? theta[i] : 0;
			rawR += 8192;
			rawPhi += 16384;
			rawTheta += 8192;
			
            //convert spherical coordinate system to euler
            double r = rawR / 8192.0 * Constants.SunRadius;
            double p = rawPhi / 32768.0 * 2 * Math.PI;
            double t = rawTheta / 32768.0 * 2 * Math.PI;

            p -= l0 / 180.0 * Math.PI;
            t += b0 / 180.0 * Math.PI;
            l.startPoint = new float[3];
            l.startPoint[0] = (float)(r * Math.sin(t) * Math.sin(p)); 	//x
            l.startPoint[1] = (float)(r * Math.cos(t)); 				//y
            l.startPoint[2] = (float)(r * Math.sin(t) * Math.cos(p)); 	//z
		}
	}
	/**
	 * split all concatenated channels to the correspoding line. In the end, all Channels will
	 * @param lengths array of all line lengths. These lengths are before they were Run-Length Encoded
	 * @param x Channel
	 * @param y Channel
	 * @param z Channel
	 * @return
	 */
	public static Line[] splitToLines(int[] lengths, int[]x, int[] y,int[] z, short[] meansShort, short[] pcaShort) {
		 Line[] lines = new Line[lengths.length];
		 int[][] channels = new int[][]{x,y,z};
		 
		 int[] indices = new int[3];
		 int startEndIndex = 0;
		 
		 int meansIndex = 0;
		 int pcaIndex = 0;
		 //go through all lines
		 for(int i = 0; i < lines.length;i++) {
			 Line l = new Line();
			 l.size = lengths[i];
			 l.channels = new float[3][];
			 l.means = new float[6];
			 l.pca = new float[9];
			 
			 //copy and dequantize means
			 for(int j = 0; j < 3;j++) {
				 l.means[j] = meansShort[meansIndex++] * 1000;
			 }
			//copy and dequantize pca
			 for(int j = 0; j < 6;j++) {
				 l.pca[j] = pcaShort[pcaIndex++] / 32767f;
			 }
			 int minus = pcaShort[pcaIndex++];
			 l.calcLastPCA(minus);
			 
			 //for all channels
			 for(int j = 0; j < 3;j++) {
				 int index = indices[j];
				 
				 int runLength = channels[j][index++]; //decode RLE
				 float[] channel = toFloat(channels[j],index,runLength);
				 l.channels[j] = channel;
				 
				 index += runLength;
				 indices[j] = index;
			 }
			 
			 lines[i] = l;
			 
		 }
		 return lines;
	 }
	
	/**
	 * Copy from int array to float
	 * @param data
	 * @param start
	 * @param length
	 * @return
	 */
	private static float[] toFloat(int[] data,int start, int length) {
		float[] out = new float[length];
		for(int i = start; i < length+start;i++) {
			out[i-start] = data[i];
		}
		return out;
	}
	
	private void calcLastPCA(int minus) {
		//cross
		minus = minus > 0 ? -1: 1;//if minus is 1, then the last pca coefficient needs to be multiplied with -1. if not, then nothing needs to happen
		float x = pca[4] * pca[2] - pca[5] * pca[1];
        float y = pca[5] * pca[0] - pca[3] * pca[2];
        float z = pca[3] * pca[1] - pca[4] * pca[0];
        
        pca[6] = x*minus;
        pca[7] = y*minus;
        pca[8] = z*minus;
	}
}
