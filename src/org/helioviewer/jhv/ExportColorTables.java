package org.helioviewer.jhv;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.internal_plugins.filter.SOHOLUTFilterPlugin.LUT;

/**
 * Simple program which will take all available color tables of JHV and export
 * them to use them with the website.
 * 
 * @author Helge Dietert
 */
public class ExportColorTables {
	/**
	 * Writes out a png for every available color table in JHV
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Map<String, LUT> list = LUT.getStandardList();
		BufferedImage image = new BufferedImage(256, list.size(),
				BufferedImage.TYPE_INT_RGB);
		int counter = 0;

		try {
			FileWriter outFile = new FileWriter(new File(
					"UltimateLookupTable.txt"));
			PrintWriter out = new PrintWriter(outFile);
			for (Map.Entry<String, LUT> e : list.entrySet()) {
				for (int i = 0; i < 256; ++i) {
					image.setRGB(i, counter, e.getValue().getLut8()[i]);
					
				}
				
				out.println(e.getKey());
				counter++;
			}
			out.close();
			outFile.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			String filename = "UltimateLookupTable.png";
			ImageIO.write(image, "png", new File(filename));
			System.out.println(filename);
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}
}
