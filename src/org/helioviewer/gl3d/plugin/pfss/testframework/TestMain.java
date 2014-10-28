package org.helioviewer.gl3d.plugin.pfss.testframework;

import java.util.ArrayList;

public class TestMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		RawDataReader raw = new RawDataReader("");
		RawDataReader compressed = new RawDataReader("");
		
		Comparer comp = new Comparer();
		ArrayList<ArrayList<Double>> errors = comp.compare(raw.getLines(), compressed.getLines());
		
		double max = 0;
		for(int i = 0; i < errors.size();i++) {
			int size = errors.get(i).size();
			ArrayList<Double> errorLines = errors.get(i);
			for(int j = 0; j < size;j++) {
				if(max < errorLines.get(j))
					max = errorLines.get(j);
			}
		}
		
		System.out.println(max);
	}

}
