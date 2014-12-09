package org.helioviewer.gl3d.plugin.pfss.data.decompression;

public class Line {
	public float[][] channels;

	public int[] start;
	public int[] end;

	public int size;
	
	public static Line[] splitToLines(int[] lengths, int[] startEnd,int[]x, int[] y,int[] z) {
		 Line[] lines = new Line[lengths.length];
		 int[][] channels = new int[][]{x,y,z};
		 
		 int[] indices = new int[3];
		 int startEndIndex = 0;
		 
		 for(int i = 0; i < lines.length;i++) {
			 Line l = new Line();
			 l.start = new int[3];
			 l.end = new int[3];
			 l.size = lengths[i];
			 l.channels = new float[3][];
			 
			 //for all channels
			 for(int j = 0; j < 3;j++) {
				 int index = indices[j];
				 l.start[j] = startEnd[startEndIndex++];
				 l.end[j] = startEnd[startEndIndex++];
				 
				 int length = channels[j][index++];
				 float[] channel = toFloat(channels[j],index,length);
				 l.channels[j] = channel;
				 
				 index += length;
				 indices[j] = index;
			 }
			 
			 lines[i] = l;
			 
		 }
		 return lines;
	 }
	
	private static float[] toFloat(int[] data,int start, int length) {
		float[] out = new float[length];
		for(int i = start; i < length+start;i++) {
			out[i-start] = data[i];
		}
		return out;
	}
}
