package org.helioviewer.gl3d.plugin.pfss.testframework;

import java.io.File;
import java.util.ArrayList;

import javax.swing.plaf.metal.MetalIconFactory.FolderIcon16;

public class TestMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		File rawFolder = new File(TestSettings.rawFolder);
		File compressedFolder = new File(TestSettings.compressedFolder);
		
		//File[] rawFiles = rawFolder.listFiles();
		//File[] compressedFiles = compressedFolder.listFiles();
		File[] rawFiles = new File[1];
		rawFiles[0] = new File("C:/dev/git/bachelor/test/testdata/raw/2005-11-06_12-04-00.000_pfss_field_data_flt.fits");
		
		File[] compressedFiles = new File[1];
		compressedFiles[0] = new File("C:/dev/git/bachelor/test/testdata/without_subsampling/2005-11-06_12-04-00.000_pfss_field_data.fits");
		
		ArrayList<ArrayList<ArrayList<Double>>> allErrors = new ArrayList<>(rawFiles.length);
		ArrayList<PerformanceData> performance = new ArrayList<>(rawFiles.length);
		
		//warm up hotspot compiler
		for(int i = 0; i < rawFiles.length/2;i++) {
			CompressedDataReader compressed = new CompressedDataReader(compressedFiles[i].getAbsolutePath());
			compressed.readFile();
		}
		
		Comparer comp = new Comparer();
		for(int i = 0; i < rawFiles.length;i++) {
			System.out.println(rawFiles[i].getAbsolutePath());
			System.out.println(compressedFiles[i].getAbsolutePath());
			System.out.println();
			RawDataReader raw = new RawDataReader(rawFiles[i].getAbsolutePath());
			CompressedDataReader compressed = new CompressedDataReader(compressedFiles[i].getAbsolutePath());
			performance.add(compressed.readFile());
			
			ArrayList<ArrayList<Double>> errors = comp.compare(raw.getLines(), compressed.getLines());
			allErrors.add(errors);
		}
		
		ResultAnalyzer.analyzeError(allErrors);
		ResultAnalyzer.analyzePerformance(performance);
	}

}
