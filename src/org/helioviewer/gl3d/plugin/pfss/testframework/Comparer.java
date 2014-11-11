package org.helioviewer.gl3d.plugin.pfss.testframework;

import java.util.ArrayList;

/**
 * Comparer class to compare raw and lossy compressed data
 * @author Jonas Schwammberger
 */
public class Comparer {

	public ArrayList<ArrayList<Double>> compare(ArrayList<Line> raw, ArrayList<Line> compressed) {
		ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>(compressed.size());
	
		for(int i = 0; i < compressed.size();i++) 
		{
			Line compressedCurrent = compressed.get(i);
			Line rawCurrent = raw.get(i);
			
			ArrayList<Double> errors = new ArrayList<>(rawCurrent.points.size());
			
			int startIndex = 0;
			for(int j = 0; j < compressedCurrent.points.size();j++) {

				Point p = compressedCurrent.points.get(j);
				MinLine min = getMinimum(rawCurrent,p,startIndex);
				startIndex = min.p1;
				
				double err = 0;
				if(min.p1 > -1) {
					Point a = rawCurrent.points.get(min.p0);
					Point b = rawCurrent.points.get(min.p1);
					err = calcError(a,b,p);
				}
				else {
					err = min.d0;
				}
				errors.add(err*err);
			}
			result.add(errors);
			
		}
		return result;
		
	}
	
	
	private static double calcError(Point A, Point B, Point P) {
		double t = checkOnLine(A, B, P);
		
		//if perpendicular line from P to AB does not intersect with AB, then use the distance of A or B.
		if(t >= 1.0) 
			return B.getDistanceTo(P);
		if(t <= 0)
			return A.getDistanceTo(P);
		
		//perpendicular line intersects with AB. Calculate the smallest distance to the line
		Point lineVector = Point.getVector(B, A);
		Point toP = Point.getVector(B,P);
		Point cross = Point.cross(toP, lineVector);
		
		return cross.magnitude()/lineVector.magnitude();
	}
	
	/**
	 * line definition: 
	 * 		line v = A + (B-A)*t for 0 <= t <= 1
	 * 
	 * calculates the factor t for the line which starts at p and is Perpendicular to v. 
	 *   if t > 0 && t < 1, means you can draw a Perpendicular line to v through p and intersects v.
	 *   
	 *   if t < 0 && t> 1 means that the intersect point is outside of v.
	 * @param A
	 * @param B
	 * @param p
	 * @return
	 */
	private static double checkOnLine(Point A, Point B, Point p) {
		double result = 0;
		Point vec0 = Point.getVector(p, A);
		Point vec1 = Point.getVector(A, B);
		double mag = vec1.magnitude();
		result = -Point.dot(vec0, vec1)/(mag*mag);
		return result;
	}
	/**
	 * minimum heuristic. Should work for all sinus-like curves
	 * @param raw
	 * @param p
	 * @param startIndex
	 * @return
	 */
	private MinLine getMinimum(Line raw, Point p, int startIndex) {
		MinLine min = new MinLine();
		
		double lastDistance = Double.MAX_VALUE;
		for(int i = startIndex; i < raw.points.size();i++) {
			double distance = p.getDistanceTo(raw.points.get(i));
			if(distance < lastDistance) {
				min.newD(distance, i);
				lastDistance = distance;
			}
			//shot over minimum
			else
			{
				//check if current distance is smaller than p1;
				if(min.d1 > distance) {
					min.newD(distance, i);
					break;
				}
			}
				
		}
		return min;
	}
	
	private class MinLine {
		int p0 = -1;
		int p1 = -1;
		double d0 = Double.MAX_VALUE;
		double d1 = Double.MAX_VALUE;
		
		public void newD(double d, int i) {
			p1 = p0;
			d1 = d0;
			d0 = d;
			p0 = i;
		}
		
	}
	
	
	
	public static void main(String[] args) {
		Point p = new Point(10,5,7);
		Point A = new Point(-2,1,7);
		Point B = new Point(2,2,4);
		
		System.out.println(calcError(A, B, p));
		System.out.println(checkOnLine(A,B,p));
		System.out.println(calcError(A,B,B));
		
		p = new Point(0,3,0);
	    A = new Point(5,5,0);
		B = new Point(10,5,0);
		System.out.println(calcError(A, B, p));
		System.out.println(checkOnLine(A,B,p));
		p = new Point(10,3,0);
		System.out.println(calcError(A, B, p));
		System.out.println(checkOnLine(A,B,p));
	}
	
}
