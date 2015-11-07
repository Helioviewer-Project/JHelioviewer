package org.helioviewer.jhv.layers;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.Texture;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class LUT
{
	private static final String ANGSTROM = "\u212B";
	public enum Lut
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
		SDO_AIA_131("SDO-AIA 131 "+ANGSTROM), 
		SDO_AIA_1600("SDO-AIA 1600 "+ANGSTROM),
		SDO_AIA_1700("SDO-AIA 1700 "+ANGSTROM), 
		SDO_AIA_171("SDO-AIA 171 "+ANGSTROM), 
		SDO_AIA_193("SDO-AIA 193 "+ANGSTROM), 
		SDO_AIA_211("SDO-AIA 211 "+ANGSTROM),
		SDO_AIA_304("SDO-AIA 304 "+ANGSTROM), 
		SDO_AIA_335("SDO-AIA 335 "+ANGSTROM), 
		SDO_AIA_4500("SDO-AIA 4500 "+ANGSTROM), 
		SDO_AIA_94("SDO-AIA 94 "+ANGSTROM),
		SOHO_EIT_171("SOHO EIT 171 "+ANGSTROM), 
		SOHO_EIT_195("SOHO EIT 195 "+ANGSTROM), 
		SOHO_EIT_284("SOHO EIT 284 "+ANGSTROM), 
		SOHO_EIT_304("SOHO EIT 304 "+ANGSTROM),
		STEREO_EUVI_171("STEREO EUVI 171 "+ANGSTROM), 
		STEREO_EUVI_195("STEREO EUVI 195 "+ANGSTROM), 
		STEREO_EUVI_284("STEREO EUVI 284 "+ANGSTROM), 
		STEREO_EUVI_304("STEREO EUVI 304 "+ANGSTROM),
		YOHKOH_SXT_AL_1("YOHKOH SXT Al 1"),
		YOHKOH_SXT_AL_MG("YOHKOH SXT AlMg"),
		YOHKOH_SXT_AL_MG_MN("YOHKOH SXT AlMgMn"),
		YOHKOH_SXT_OPEN("YOHKOH SXT Open"),
		YOHKOH_SXT_THIN_AL("YOHKOH SXT thin-Al"),
		YOHKOH_SXT_WHITE_LIGHT("YOHKOH SXT white-light");

		private final String name;
		
		Lut(String _name)
		{
			name = _name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
	
	private static final Texture tex = new Texture();
	
	static
	{
		try
		{
			BufferedImage bufferedImage = ImageIO.read(MainPanel.class.getResourceAsStream("/UltimateLookupTable.png"));
			tex.upload(bufferedImage, 256, 256);
			GLContext.getCurrentGL().glEnable(GL2.GL_TEXTURE_2D);
			GLContext.getCurrentGL().glBindTexture(GL2.GL_TEXTURE_2D, tex.openGLTextureId);
			GLContext.getCurrentGL().getGL2().glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
			GLContext.getCurrentGL().getGL2().glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
			GLContext.getCurrentGL().glBindTexture(GL2.GL_TEXTURE_2D, 0);
			GLContext.getCurrentGL().glDisable(GL2.GL_TEXTURE_2D);
		}
		catch (IOException e)
		{
			Telemetry.trackException(e);
		}
	}	
	
	public static int getTextureId()
	{
		return tex.openGLTextureId;
	}	
}
