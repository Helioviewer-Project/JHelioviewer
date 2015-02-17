package org.helioviewer.gl3d.plugin.pfss.data.decompression;

import java.util.LinkedList;

import org.helioviewer.base.physics.Constants;

/**
 * This class is responsible for holding the intermediate data during decompression.
 * @author Jonas Schwammberger
 *
 */
public class IntermediateLineData {
	public float[][] channels;

	public float[] startPoint;
	public float[] endPoint;
	public int size;
	
	private IntermediateLineData() {

	}
	
	/**
	 * decode prediction coding
	 */
	public void decodePrediction() {
		multiplyPredictionError(this.channels);
		
		for(int i = 0; i < channels.length;i++) {
			float[] decodedChannel = new float[channels[i].length+2];
			decodedChannel[0] = startPoint[i];
			decodedChannel[decodedChannel.length-1] = endPoint[i];
			if(decodedChannel.length > 2) {
				int channelIndex = 0;
				LinkedList<Indices> queue = new LinkedList<>();
				queue.add(new Indices(0, decodedChannel.length-1));
				while(!queue.isEmpty()) {
					prediction(queue,decodedChannel,channels[i],channelIndex);
					channelIndex++;
				}
			}
			this.channels[i] = decodedChannel;
		}
		this.size = this.channels[0].length;
	}
	
	/**
	 * Multiplies the prediction errors
	 * @param channels
	 */
	private static void multiplyPredictionError(float[][] channels) {
		for(int i = 0; i < channels.length;i++) { 
			float[] current = channels[i];
			
			for(int j = 0; j < 5 && j< current.length;j++) {
				current[j] = current[j]*6;
			}
			
			for(int j = 5; j < 16 && j< current.length;j++) {
				current[j] *= 10;
			}

			for(int j = 16;  j < current.length;j++) {
				current[j] *= 16;
			}
		}
	}
	
	/**
	 * predict one value
	 * @param queue Breadth first indices of the next prediction
	 * @param decodedChannel decoded channel
	 * @param encodedChanel
	 * @param nextIndex
	 */
	private static void prediction(LinkedList<Indices> queue,float[] decodedChannel,float[] encodedChanel, int nextIndex) {
		Indices i = queue.pollFirst();
		float start = decodedChannel[i.startIndex];
		float end = decodedChannel[i.endIndex];
		
		int toPredictIndex = (i.endIndex - i.startIndex) / 2 + i.startIndex;
		float predictionError = encodedChanel[nextIndex];
		
		//predict
		float predictionFactor0 = (toPredictIndex-i.startIndex)/(float)(i.endIndex - i.startIndex);
		float predictionFactor1 = (i.endIndex-toPredictIndex)/(float)(i.endIndex - i.startIndex);
		float prediction = (int)(predictionFactor0* start + predictionFactor1*end);
		decodedChannel[toPredictIndex] = prediction-predictionError;
		
		//add next level of indices
		if (i.startIndex + 1 != toPredictIndex){
			Indices next = new Indices(i.startIndex,toPredictIndex);
			queue.addLast(next);
        }
		if (i.endIndex - 1 != toPredictIndex) {
			Indices next = new Indices(toPredictIndex,i.endIndex);
			queue.addLast(next);
		}
	}
	
	/**
	 * Converts the spherical coordinates to cartesian. It centers the coordinates around the viewpoint of earth.
	 * @param longitudeToEarth l0
	 * @param latitudeToEarth b0
	 */
	public void toCartesian(double longitudeToEarth, double latitudeToEarth) {
		for(int i = 0; i <this.size;i++) {
			float rawR =  channels[0][i];
			float rawPhi = channels[1][i];
			float rawTheta = channels[2][i];
			rawR += 8192;
			rawPhi += 16384;
			rawTheta += 8192;
			
	        double r = rawR / 8192.0 * Constants.SunRadius;
	        double p = rawPhi / 32768.0 * 2 * Math.PI;
	        double t = rawTheta / 32768.0 * 2 * Math.PI;
	        
	        p -= longitudeToEarth / 180.0 * Math.PI;
	        t += latitudeToEarth / 180.0 * Math.PI;
	        
	        channels[0][i] = (float)(r * Math.sin(t) * Math.sin(p)); 	//x
	        channels[1][i] = (float)(r * Math.cos(t)); 					//y
	        channels[2][i] = (float)(r * Math.sin(t) * Math.cos(p)); 	//z
		}
	}	
	
	/**
	 * add the starting point to each fieldline..
	 * @param lines all lines
	 * @param radius all radi of the startpoints
	 * @param phi all phi of the startpoins
	 * @param theta all theta of the startpoints
	 */
	public static void addStartPoint(IntermediateLineData[] lines, int[] radius, int[] phi, int[] theta) {
		for(int i = 0; i < lines.length;i++) {
			IntermediateLineData l = lines[i];
			
			l.startPoint = new float[3];
            l.startPoint[0] = radius[i];	
            l.startPoint[1] = phi[i]; 				
            l.startPoint[2] = theta[i];
		}
	}
	
	/**
	 * add the end point to each line. The starting point will be converted from spherical to euler coodinate system.
	 * @param lines all lines
	 * @param radius all radii of the startpoints
	 * @param phi all phi of the startpoins
	 * @param theta all theta of the startpoints
	 */
	public static void addEndPoint(IntermediateLineData[] lines,
			int[] radius, int[] phi, int[] theta) {
		for(int i = 0; i < lines.length;i++) {
			IntermediateLineData l = lines[i];
           
            l.endPoint = new float[3];
            l.endPoint[0] = radius[i];
            l.endPoint[1] = phi[i];
            l.endPoint[2] = theta[i];
		}
		
	}
	
	/**
	 * split all concatenated channels to the correspoding line. In the end, all Channels will
	 * @param lengths array of all line lengths. These lengths are before they were Run-Length Encoded
	 * @param radius Channel
	 * @param phi Channel
	 * @param theta Channel
	 * @return
	 */
	public static IntermediateLineData[] splitToLines(int[] lengths, int[]radius, int[] phi,int[] theta) {
		 IntermediateLineData[] lines = new IntermediateLineData[lengths.length];
		 int[][] channels = new int[][]{radius,phi,theta};
		 
		 int[] indices = new int[3];
		 
		 //go through all lines
		 for(int i = 0; i < lines.length;i++) {
			 IntermediateLineData currentLine = new IntermediateLineData();
			 currentLine.size = lengths[i];
			 currentLine.channels = new float[3][];

			 //for all channels
			 for(int j = 0; j < 3;j++) {
				 int index = indices[j];
				 
				 float[] channel = toFloat(channels[j],index,currentLine.size);
				 currentLine.channels[j] = channel;
				 
				 index += currentLine.size;
				 indices[j] = index;
			 }
			 
			 lines[i] = currentLine;
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

	/**
	 * Helper class to und Prediction coding
	 * @author Jonas Schwammberger
	 *
	 */
	private static class Indices {
		public int startIndex;
		public int endIndex;
		
		public Indices(int start, int end) {
			this.startIndex = start;
			this.endIndex = end;
		}
	}
}
