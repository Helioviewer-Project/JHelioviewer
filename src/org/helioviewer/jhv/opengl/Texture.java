package org.helioviewer.jhv.opengl;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;

//FIXME: combine with imageRegion
class Texture
{
	private KakaduLayer sourceId;
	private ImageRegion imageRegion;
	private final int openGLTextureId;

	Texture(int _openGLTextureId)
	{
		openGLTextureId = _openGLTextureId;
	}

	void setNewImageRegion(KakaduLayer _sourceId, ImageRegion _imageRegion)
	{
		sourceId = _sourceId;
		_imageRegion.setOpenGLTextureId(openGLTextureId);
		imageRegion = _imageRegion;
	}

	public boolean compareRegion(KakaduLayer id, ImageRegion imageRegion, LocalDateTime localDateTime)
	{
		return imageRegion != null
				&& this.sourceId == id
				&& this.imageRegion.contains(imageRegion)
				&& this.imageRegion.equalOrHigherResolution(imageRegion)
				&& localDateTime.isEqual(this.imageRegion.getDateTime());
	}

	public boolean compareTexture(int _sourceId, LocalDateTime ldt)
	{
		return imageRegion != null && ldt.equals(imageRegion.getDateTime());
	}

	public ImageRegion getImageRegion()
	{
		return imageRegion;
	}

	public void invalidate()
	{
		if(imageRegion==null)
			return;
		
		LocalDateTime ldt=imageRegion.getDateTime();
		imageRegion=null;
		
		if (TimeLine.SINGLETON.getCurrentDateTime().equals(ldt))
			MainFrame.MAIN_PANEL.repaint();
	}
}