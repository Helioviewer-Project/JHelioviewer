package org.helioviewer.gl3d.plugin.pfss.testframework.debugClasses;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;




import org.helioviewer.gl3d.plugin.pfss.testframework.CompressedDataReader;
import org.helioviewer.gl3d.plugin.pfss.testframework.Line;
import org.helioviewer.gl3d.plugin.pfss.testframework.Point;

/**
 * This analyzes uncompressed with the lossy compressed data. This is useful for debugging and developing the compresion.
 * @author Jonas Schwammberger
 *
 */
public class DebugAnalyzer {

	public static void analyze(UncompressedReader unc, CompressedDataReader comp, String csvFile) {
		try {
			FileWriter out = new FileWriter(csvFile);
			ArrayList<Line> compLines = comp.getLines();
			ArrayList<Line> uncLines = unc.getLines();
			
			//out.write("actual r;actual phi;actual theta;should r;should phi;should theta;dr;dPhi;dTheta\n");
			double maxErrR=0;
			double maxErrP=0;
			double maxErrT=0;
			
			double varR = 0;
			double varP = 0;
			double varT = 0;
			for(int i = 0; i < compLines.size();i++) {
				Line compLine = compLines.get(i);
				Line uncLine = uncLines.get(i);
				
				for(int j = 0; j< compLine.points.size();j++) {
					Point p1 = compLine.points.get(j);
					Point p2 = uncLine.points.get(j);
					
					int errR = p1.getRawR() - p2.getRawR();
					int errP = p1.getRawPhi() - p2.getRawPhi();
					int errT = p1.getRawTheta() - p2.getRawTheta();
					Math.max(maxErrR, errR);
					Math.max(maxErrP, errP);
					Math.max(maxErrT, errT);
					
					//out.write( p1.getRawR()+";"+p1.getRawPhi()+";"+p1.getRawTheta()+";"+ p2.getRawR()+";"+p2.getRawPhi()+";"+p2.getRawTheta()+";"+errR+";"+errP+";"+errT);
					varR += errR*errR;
					varP += errP*errP;
					varT += errT*errT;
				}
			}
			varR = Math.sqrt(varR);
			varP = Math.sqrt(varP);
			varT = Math.sqrt(varT);
			out.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
