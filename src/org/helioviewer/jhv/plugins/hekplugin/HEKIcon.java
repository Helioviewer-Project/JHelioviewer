package org.helioviewer.jhv.plugins.hekplugin;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.swing.ImageIcon;

import org.helioviewer.jhv.opengl.OpenGLHelper;

class HEKIcon
{
	private static final String PATH = "/images/EventIcons/";

	private static int texture = -1;
	private static OpenGLHelper openGLHelper;

	enum HEKICONS
	{
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

		public String getID() {
			return id;
		}
	}

	static
	{
		openGLHelper = new OpenGLHelper();
		texture = openGLHelper.createTextureID();
		for (HEKICONS hekIcon : HEKICONS.values())
		{
			BufferedImage bufferedImage = getImage(hekIcon);
			openGLHelper.bindBufferedImageToGLTexture(bufferedImage, 0,
					hekIcon.ordinal() * bufferedImage.getHeight(),
					bufferedImage.getWidth(), bufferedImage.getHeight()
							* HEKICONS.values().length);
		}
	}

	public static int getTexture() {
		return texture;
	}

	public static float getImageScaleFactorHeight() {
		return openGLHelper.getScaleFactorHeight();
	}

	private static BufferedImage getImage(HEKICONS icon) {

		ImageIcon imageIcon = getIcon(icon);

		if (imageIcon == null)
			return null;

		Image image = imageIcon.getImage();

		if (image != null && image.getWidth(null) > 0
				&& image.getHeight(null) > 0) {
			BufferedImage bi = new BufferedImage(image.getWidth(null),
					image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

			Graphics g = bi.getGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();

			return bi;
		}

		return null;
	}

	private static ImageIcon getIcon(HEKICONS icon) {
		URL imgURL = HEKIcon.class.getResource(PATH + icon.getName());
		return new ImageIcon(imgURL);
	}

}
