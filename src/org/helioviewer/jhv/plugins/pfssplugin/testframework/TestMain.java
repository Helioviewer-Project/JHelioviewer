package org.helioviewer.jhv.plugins.pfssplugin.testframework;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

import org.helioviewer.jhv.plugins.pfssplugin.data.FileDescriptor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressor;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssFrame;

public class TestMain {

	public static void main(String[] args) {

		File compressedFolder = new File(TestSettings.compressedFolder);
		File[] compressedFiles = compressedFolder.listFiles();
		
		System.out.println("warm up hotspot compiler");
		for(int i = 0; i < compressedFiles.length/2;i++) {
			PfssData d = new PfssData(null,"file:///"+compressedFiles[i].getAbsolutePath());
			d.loadData();
			FileDescriptor f = new FileDescriptor(new Date(0), new Date(1), "test.rar",0);
			PfssFrame frame = new PfssFrame(f);
			PfssDecompressor comp = new PfssDecompressor(d,frame);
			comp.readData();
		}
		
		System.out.println("preparing");
		PfssData[] allData = new PfssData[compressedFiles.length];
		PfssFrame[] allFrames = new PfssFrame[compressedFiles.length];
		for(int i = 0; i < compressedFiles.length;i++) {
			PfssData d = new PfssData(null,"file:///"+compressedFiles[i].getAbsolutePath());
			d.loadData();
			allData[i] = d;
			FileDescriptor f = new FileDescriptor(new Date(0), new Date(1), "test.rar",0);
			PfssFrame frame = new PfssFrame(f);
			allFrames[i] = frame;
		}
		
		System.out.println("testing");
		double time = 0;
		for(int i = 0; i < compressedFiles.length;i++) {
			long start = System.currentTimeMillis();
			PfssDecompressor comp = new PfssDecompressor(allData[i], allFrames[i]);
			comp.readData();
			long end = System.currentTimeMillis();
			time +=end-start;
		}
		System.out.println("finished median milliseconds:");
		System.out.println(time/compressedFiles.length);
	}

}
