package org.helioviewer.jhv.layers;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.base.JHVUncaughtExceptionHandler;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.Texture;

import com.jogamp.opengl.GL2;

public enum LUT
{
	BLUE_GREEN_RED_YELLOW("Blue/Green/Red/Yellow"), 
	BLUE_RED("Blue/Red"),
	BLUE_WHITE_LINEAR("Blue/White Linear"), 
	GRAY("Gray"), 
	GREEN_WHITE_EXPONENTIAL("Green/White Exponential"),
	GREEN_WHITE_LINEAR("Green/White Linear"), 
	RAINBOW_1("Rainbow 1"), 
	RAINBOW_2("Rainbow 2"),
	RED_TEMPERATURE("Red Temperature"), 
	SDO_AIA_131("SDO-AIA 131 \u212B"), 
	SDO_AIA_1600("SDO-AIA 1600 \u212B"),
	SDO_AIA_1700("SDO-AIA 1700 \u212B"), 
	SDO_AIA_171("SDO-AIA 171 \u212B"), 
	SDO_AIA_193("SDO-AIA 193 \u212B"), 
	SDO_AIA_211("SDO-AIA 211 \u212B"),
	SDO_AIA_304("SDO-AIA 304 \u212B"), 
	SDO_AIA_335("SDO-AIA 335 \u212B"), 
	SDO_AIA_4500("SDO-AIA 4500 \u212B"), 
	SDO_AIA_94("SDO-AIA 94 \u212B"),
	SOHO_EIT_171("SOHO EIT 171 \u212B"), 
	SOHO_EIT_195("SOHO EIT 195 \u212B"), 
	SOHO_EIT_284("SOHO EIT 284 \u212B"), 
	SOHO_EIT_304("SOHO EIT 304 \u212B"),
	STEREO_EUVI_171("STEREO EUVI 171 \u212B"), 
	STEREO_EUVI_195("STEREO EUVI 195 \u212B"), 
	STEREO_EUVI_284("STEREO EUVI 284 \u212B"), 
	STEREO_EUVI_304("STEREO EUVI 304 \u212B"),
	YOHKOH_SXT_AL_1("YOHKOH SXT Al 1"),
	YOHKOH_SXT_AL_MG("YOHKOH SXT AlMg"),
	YOHKOH_SXT_AL_MG_MN("YOHKOH SXT AlMgMn"),
	YOHKOH_SXT_OPEN("YOHKOH SXT Open"),
	YOHKOH_SXT_THIN_AL("YOHKOH SXT thin-Al"),
	YOHKOH_SXT_WHITE_LIGHT("YOHKOH SXT white-light");

	private final String name;
	private Color avgColor;
	
	LUT(String _name)
	{
		name = _name;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	public Color getAvgColor()
	{
		return avgColor;
	}
	
	private static Texture tex;
	
	public static void loadTexture(GL2 _gl)
	{
		if(tex!=null)
			throw new IllegalStateException();
		
		try
		{
			tex = new Texture(_gl);
			BufferedImage bufferedImage = ImageIO.read(MainPanel.class.getResourceAsStream("/UltimateLookupTable.png"));
			tex.upload(_gl, bufferedImage, 256, 256);
			_gl.glEnable(GL2.GL_TEXTURE_2D);
			_gl.glBindTexture(GL2.GL_TEXTURE_2D, tex.openGLTextureId);
			_gl.getGL2().glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
			_gl.getGL2().glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
			_gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
			_gl.glDisable(GL2.GL_TEXTURE_2D);
			
			for(LUT l:LUT.values())
			{
				int w=bufferedImage.getWidth();
				int y=l.ordinal();
				long sumR=0;
				long sumG=0;
				long sumB=0;
				int maxR=0;
				int maxG=0;
				int maxB=0;
				for(int x=0;x<w;x++)
				{
					int pixel=bufferedImage.getRGB(x, y);
					int r=(pixel>>16) & 0xff;
					int g=(pixel>>8) & 0xff;
					int b=(pixel) & 0xff;
					
					if(maxR<r)
						maxR=r;
					if(maxG<g)
						maxG=g;
					if(maxB<b)
						maxB=b;
					
					sumR+=r;
					sumG+=g;
					sumB+=b;
				}
				
				l.avgColor=new Color(Math.min((int)(1*sumR/w),maxR),Math.min((int)(1*sumG/w),maxG),Math.min((int)(1*sumB/w),maxB));
			}
		}
		catch (IOException e)
		{
			JHVUncaughtExceptionHandler.SINGLETON.uncaughtException(e);
		}
	}

	public static int getTextureId()
	{
		return tex.openGLTextureId;
	}
}
