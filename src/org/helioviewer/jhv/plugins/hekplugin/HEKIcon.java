package org.helioviewer.jhv.plugins.hekplugin;

import org.helioviewer.jhv.opengl.Texture;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

class HEKIcon
{
	private static final String PATH = "/images/EventIcons/";

	private static Texture openGLHelper;

	enum HEKIcons
	{
		AR_ICON("ar_icon.png"), PB_ICON("bp_icon.png"), CD_ICON("cd_icon.png"), CE_ICON(
				"ce_icon.png"), CH_ICON("ch_icon.png"), CJ_ICON("cj_icon.png"), CW_ICON(
				"cw_icon.png"), EF_ICON("ef_icon.png"), FA_ICON("fa_icon.png"), FE_ICON(
				"fe_icon.png"), FI_ICON("fi_icon.png"), FL_ICON("fl_icon.png"), LP_ICON(
				"lp_icon.png"), NR_ICON("nr_icon.png"), OS_ICON("os_icon.png"), OT_ICON(
				"ot_icon.png"), PG_ICON("pg_icon.png"), SG_ICON("sg_icon.png"), SP_ICON(
				"sp_icon.png"), SS_ICON("ss_icon.png");

		final String name;

		HEKIcons(String _name)
		{
			name = _name;
		}
	}

	static
	{
		openGLHelper = new Texture();
		for (HEKIcons hekIcon : HEKIcons.values())
		{
			BufferedImage bufferedImage = getImage(hekIcon);
			openGLHelper.upload(bufferedImage, 0,
					hekIcon.ordinal() * bufferedImage.getHeight(),
					bufferedImage.getWidth(),
					bufferedImage.getHeight()	* HEKIcons.values().length);
		}
	}

	public static int getOpenGLTextureId()
	{
		return openGLHelper.openGLTextureId;
	}

	public static float getImageScaleFactorHeight()
	{
		return openGLHelper.textureHeight;
	}

	private static BufferedImage getImage(HEKIcons icon)
	{
		ImageIcon imageIcon = getIcon(icon);

		if (imageIcon == null)
			return null;

		Image image = imageIcon.getImage();

		if (image != null && image.getWidth(null) > 0 && image.getHeight(null) > 0)
		{
			BufferedImage bi = new BufferedImage(image.getWidth(null),
					image.getHeight(null), BufferedImage.TYPE_INT_ARGB);

			Graphics g = bi.getGraphics();
			g.drawImage(image, 0, 0, null);
			g.dispose();

			return bi;
		}

		return null;
	}

	private static ImageIcon getIcon(HEKIcons icon)
	{
		return new ImageIcon(HEKIcon.class.getResource(PATH + icon.name));
	}

}
