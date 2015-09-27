package org.helioviewer.jhv.plugins.pfssplugin.data.decompression;

import java.util.LinkedList;

/**
 * This class is responsible for holding the intermediate data during decompression.
 */
class IntermediateLineData {
	public float[][] channels;
	public int size;
	
	private IntermediateLineData() {

	}
	
	/**
	 * decode prediction coding
	 */
	public void decodePrediction(float _Q1, float _Q2, float _Q3)
	{
		dequantizePredictionErrors(_Q1,_Q2,_Q3);
		
		for(int i = 0; i < channels.length;i++) {
			float[] decodedChannel = new float[channels[i].length];
			decodedChannel[0] = channels[i][0];
			decodedChannel[decodedChannel.length-1] = channels[i][0]+channels[i][1];
			if(decodedChannel.length > 2) {
				int channelIndex = 2;
				LinkedList<Indices> queue = new LinkedList<>();
				queue.add(new Indices(0, decodedChannel.length-1));
				while(!queue.isEmpty())
				{
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
	private void dequantizePredictionErrors(float _Q1, float _Q2, float _Q3) {
		for(int i = 0; i < channels.length;i++) { 
			float[] current = channels[i];
			
			int j=0;
			for(; j < 5 && j< current.length;j++) {
				current[j] *= _Q1;
			}
			
			for(; j < 16 && j< current.length;j++) {
				current[j] *= _Q2;
			}

			for(;  j < current.length;j++) {
				current[j] *= _Q3;
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
		
		int toPredictIndex = (i.startIndex + i.endIndex) / 2;
		float predictionError = encodedChanel[nextIndex];
		
		//predict
		float predictionFactor0 = (toPredictIndex-i.startIndex)/(float)(i.endIndex - i.startIndex);
		float prediction = (1-predictionFactor0)* start + predictionFactor0*end;
		decodedChannel[toPredictIndex] = prediction-predictionError;
		
		//add next level of indices
		if (i.startIndex + 1 != toPredictIndex)
			queue.addLast(new Indices(i.startIndex,toPredictIndex));

		if (i.endIndex - 1 != toPredictIndex)
			queue.addLast(new Indices(toPredictIndex,i.endIndex));
	}
	
	/**
	 * split all concatenated channels to the correspoding line. In the end, all Channels will
	 * @param lengths array of all line lengths. These lengths are before they were Run-Length Encoded
	 * @param radius Channel
	 * @param phi Channel
	 * @param theta Channel
	 * @return
	 */
	public static IntermediateLineData[] splitToLines(int[] lengths, int[] x, int[] y,int[] z) {
		 IntermediateLineData[] lines = new IntermediateLineData[lengths.length];
		 int[][] channels = new int[][]{x,y,z};
		 
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
