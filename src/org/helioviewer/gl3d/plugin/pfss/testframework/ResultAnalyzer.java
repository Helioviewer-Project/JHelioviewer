package org.helioviewer.gl3d.plugin.pfss.testframework;

import java.util.ArrayList;

public class ResultAnalyzer {

	
	public static void analyzeError(ArrayList<ArrayList<ArrayList<Double>>> errors) {
		double sumSquaredError = 0;
		double maxSquaredError = 0;
		int pointCount = 0;
		for(ArrayList<ArrayList<Double>> file : errors) {
			for(ArrayList<Double> line: file) {
				pointCount += line.size();
				for(Double error : line) {
					sumSquaredError += error;
					maxSquaredError = Math.max(error, maxSquaredError);
				}
			}
		}
		
		sumSquaredError /= pointCount;
		double standardDeviation = Math.sqrt(sumSquaredError);
		System.out.println("standard deviation: "+ standardDeviation);
		System.out.println("max error: "+ Math.sqrt(maxSquaredError));
	}
	
	public static void analyzePerformance(ArrayList<PerformanceData> performance) {
		double sum = 0;
		for(PerformanceData p : performance) {
			sum += p.end-p.start;
		}
		
		System.out.println("Average milliseconds: "+ sum/performance.size());
	}
}
