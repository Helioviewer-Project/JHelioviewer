package org.helioviewer.jhv.layers.filter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

import com.jogamp.opengl.GL2;

public class LUT
{
	public enum LUT_ENTRY{
		BLUE_GREEN_RED_YELLOW("Blue/Green/Red/Yellow"), BLUE_RED("Blue/Red"),
		BLUE_WHITE_LINEAR("Blue/White Linear"), GRAY("Gray"), GREEN_WHITE_EXPONENTIAL("Green/White Exponential"),
		GREEN_WHITE_LINEAR("Green/White Linear"), RAINBOW_1("Rainbow 1"), RAINBOW_2("Rainbow 2"),
		RED_TEMPERATURE("Red Temperature"), SDO_AIA_131("SDO-AIA 131 Å"), SDO_AIA_1600("SDO-AIA 1600 Å"),
		SDO_AIA_1700("SDO-AIA 1700 Å"), SDO_AIA_171("SDO-AIA 171 Å"), SDO_AIA_193("SDO-AIA 193 Å"), SDO_AIA_211("SDO-AIA 211 Å"),
		SDO_AIA_304("SDO-AIA 304 Å"), SDO_AIA_335("SDO-AIA 335 Å"), SDO_AIA_4500("SDO-AIA 4500 Å"), SDO_AIA_94("SDO-AIA 94 Å"),
		SOHO_EIT_171("SOHO EIT 171 Å"), SOHO_EIT_195("SOHO EIT 195 Å"), SOHO_EIT_284("SOHO EIT 284 Å"), SOHO_EIT_304("SOHO EIT 304 Å"),
		STEREO_EUVI_171("STEREO EUVI 171 Å"), STEREO_EUVI_195("STEREO EUVI 195 Å"), STEREO_EUVI_284("STEREO EUVI 284 Å"), STEREO_EUVI_304("STEREO EUVI 304 Å");
		
		private String name;
		
		LUT_ENTRY(String name){
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
	
	private static LinkedHashMap<String, Integer> lutMap;
	private static int nextAvaibleLut = 0;
	private static int texture = -1;
	private static OpenGLHelper openGLHelper;
	
	public static int getLutPosition(String name)
	{
	    Integer idx=lutMap.get(name);
	    if(idx==null)
	        throw new RuntimeException("LUT: \""+name+"\" not found in LUT.");
	    
		return idx;
	}
	
	public static String[] getNames()
	{
		return lutMap.keySet().toArray(new String[0]);
	}
	
	static
	{
		lutMap = new LinkedHashMap<String, Integer>();
		openGLHelper = new OpenGLHelper();
		loadLutFromFile("/UltimateLookupTable.txt");      
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					BufferedImage bufferedImage;
					bufferedImage = ImageIO.read(MainPanel.class.getResourceAsStream("/UltimateLookupTable.png"));
					texture = openGLHelper.createTextureID();
					openGLHelper.bindBufferedImageToGLTexture(bufferedImage, 256, 256);
					OpenGLHelper.glContext.getGL().glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
			                GL2.GL_NEAREST);
					OpenGLHelper.glContext.getGL().glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
			                GL2.GL_NEAREST);
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
	}	
	
	private static void loadLutFromFile(String lutTxtName)
	{
		try(BufferedReader br=new BufferedReader(new InputStreamReader(MainPanel.class.getResourceAsStream(lutTxtName),"UTF-8")))
		{
		    String line = null;
			while ((line = br.readLine()) != null)
				lutMap.put(line, nextAvaibleLut++);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static int getTexture(GL2 gl)
	{
		return texture;
	}	
}
