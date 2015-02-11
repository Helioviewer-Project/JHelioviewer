package org.helioviewer.jhv.layers.filter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class LUT {
	
	private LinkedHashMap<String, Integer> lutMap;
	private int nextAvaibleLut = 0;
	private int texture = -1;

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
	
	public int getTexture(GL2 gl){
		if (texture < 0){
			BufferedImage bufferedImage;
			try {
				bufferedImage = ImageIO.read(CompenentView.class.getResourceAsStream("/UltimateLookupTable.png"));
				texture = OpenGLHelper.createTexture(gl, bufferedImage, 256, 256);
				OpenGLHelper.updateTexture(gl, texture, bufferedImage);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this.texture;
	}
}
