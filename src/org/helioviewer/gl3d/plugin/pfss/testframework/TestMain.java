package org.helioviewer.gl3d.plugin.pfss.testframework;

import java.io.File;
import java.util.ArrayList;

import javax.swing.plaf.metal.MetalIconFactory.FolderIcon16;

public class TestMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		File rawFolder = new File(TestSettings.rawFolder);
		File compressedFolder = new File(TestSettings.compressedFolder);
		
		File[] rawFiles = rawFolder.listFiles();
		File[] compressedFiles = compressedFolder.listFiles();
		
		ArrayList<ArrayList<ArrayList<Double>>> allErrors = new ArrayList<>(rawFiles.length);
		
		Comparer comp = new Comparer();
		for(int i = 0; i < rawFiles.length;i++) {
			System.out.println(rawFiles[i].getAbsolutePath());
			System.out.println(compressedFiles[i].getAbsolutePath());
			System.out.println();
			RawDataReader raw = new RawDataReader(rawFiles[i].getAbsolutePath());
			CompressedDataReader compressed = new CompressedDataReader(compressedFiles[i].getAbsolutePath());
			
			ArrayList<ArrayList<Double>> errors = comp.compare(raw.getLines(), compressed.getLines());
			allErrors.add(errors);
		}
		

		double max = 0;
		int maxK = 0;
		int maxI = 0;
		int maxJ = 0;
		for(int k = 0; k < allErrors.size();k++){
			ArrayList<ArrayList<Double>> errors = allErrors.get(k);
			for(int i = 0; i < errors.size();i++) {
				int size = errors.get(i).size();
				ArrayList<Double> errorLines = errors.get(i);
				for(int j = 0; j < size;j++) {
					if(max < errorLines.get(j)) {
						max =  errorLines.get(j);
						maxK = k;
						maxI = i;
						maxJ = j;
					}
					
				}
			}
		}
		
		System.out.println(max);
		System.out.println(maxK);
		System.out.println(maxI);
		System.out.println(maxJ);
	}

}
