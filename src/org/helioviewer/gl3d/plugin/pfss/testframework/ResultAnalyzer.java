package org.helioviewer.gl3d.plugin.pfss.testframework;

import java.util.ArrayList;

public class ResultAnalyzer {

	
	public void analyze(ArrayList<ArrayList<ArrayList<Double>>> errors) {
		double sumSquaredError = 0;
		double maxSquaredError = 0;
		for(ArrayList<ArrayList<Double>> file : errors) {
			for(ArrayList<Double> line: file) {
				for(Double error : line) {
					sumSquaredError += error;
					maxSquaredError = Math.max(error, maxSquaredError);
				}
			}
		}
		
		double standardDeviation = Math.sqrt(sumSquaredError);
		
		
	}
}
