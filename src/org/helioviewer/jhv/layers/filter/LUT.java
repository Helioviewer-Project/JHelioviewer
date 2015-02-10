package org.helioviewer.jhv.layers.filter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class LUT {
	
	private LinkedHashMap<String, Integer> lutMap;
	private int nextAvaibleLut = 0;

	private static LUT lut = new LUT();
	
	public static LUT getLut(){
		return lut;
	}

	public static int getLutPosition(String name){
		return LUT.getLut().lutMap.get(name);
	}
	
	public String[] getNames(){
		return LUT.getLut().lutMap.keySet().toArray(new String[0]);
	}
	
	private LUT(){
		lutMap = new LinkedHashMap<String, Integer>();
		loadLutFromFile("/UltimateLookupTable.txt");        
	}	
	
	private void loadLutFromFile(String lutTxtName){
		String line = null;
		
		try(BufferedReader br=new BufferedReader(new InputStreamReader(CompenentView.class.getResourceAsStream(lutTxtName),"UTF-8")))
		{
			while ((line = br.readLine()) != null){
				lutMap.put(line, this.nextAvaibleLut++);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
