package org.helioviewer.jhv.layers.filter;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;

import javax.imageio.ImageIO;
import javax.media.opengl.GL2;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class LUT
{
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
					bufferedImage = ImageIO.read(CompenentView.class.getResourceAsStream("/UltimateLookupTable.png"));
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
		try(BufferedReader br=new BufferedReader(new InputStreamReader(CompenentView.class.getResourceAsStream(lutTxtName),"UTF-8")))
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
