package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;

import org.helioviewer.jhv.opengl.OpenGLHelper;

public class HEKIcon {

	private static final int IMAGE_HEIGHT = 1024;
	private static final int IMAGE_WIDTH = 32;
	private static final String PATH = "/images/EventIcons/";
	
	private static int texture = -1;
	private static OpenGLHelper openGLHelper;
	
	enum HEKICONS {
		AR_ICON("ar_icon.png"), PB_ICON("bp_icon.png"), CD_ICON("cd_icon.png"), CE_ICON(
				"ce_icon.png"), CH_ICON("ch_icon.png"), CJ_ICON("cj_icon.png"), CW_ICON(
				"cw_icon.png"), EF_ICON("ef_icon.png"), FA_ICON("fa_icon.png"), FE_ICON(
				"fe_icon.png"), FI_ICON("fi_icon.png"), FL_ICON("fl_icon.png"), LP_ICON(
				"lp_icon.png"), NR_ICON("nr_icon.png"), OS_ICON("os_icon.png"), OT_ICON(
				"ot_icon.png"), PG_ICON("pg_icon.png"), SG_ICON("sg_icon.png"), SP_ICON(
				"sp_icon.png"), SS_ICON("ss_icon.png");

		private String name;
		private String id;
		
		private HEKICONS(String name) {
			this.name = name;
			this.id = name.substring(0, 2);
		}

		public String getName() {
			return name;
		}		
		
		public String getID(){
			return id;
		}
	}

	private static void init(){
		openGLHelper = new OpenGLHelper();
		texture = openGLHelper.createTextureID();
		for (HEKICONS hekIcon : HEKICONS.values()) {
			try {
				System.out.println("name : " + PATH + hekIcon.getName());
				BufferedImage bufferedImage = ImageIO.read(HEKPlugin3D
						.getResourceUrl(PATH + hekIcon.getName()));
				openGLHelper.bindBufferedImageToGLTexture(bufferedImage, 0,
						hekIcon.ordinal() * bufferedImage.getHeight(),
						bufferedImage.getWidth(), bufferedImage.getHeight()
								* HEKICONS.values().length);
			} catch (IOException e) {
				System.out.println("error");
				e.printStackTrace();
			}
		}
	}
	
	public static int getTexture(){
		if (texture < 0) init();
		return texture;
	}

	public static float getImageScaleFactorHeight() {
		return openGLHelper.getScaleFactorHeight();
	}
}
