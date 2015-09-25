package org.helioviewer.jhv.opengl;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.UltimateLayer;

//FIXME: combine with imageRegion
class Texture
{
	private UltimateLayer sourceId;
	private ImageRegion imageRegion;
	private final int openGLTextureId;

	Texture(int _openGLTextureId)
	{
		openGLTextureId = _openGLTextureId;
	}

	void setNewImageRegion(UltimateLayer _sourceId, ImageRegion _imageRegion)
	{
		sourceId = _sourceId;
		_imageRegion.setOpenGLTextureId(openGLTextureId);
		imageRegion = _imageRegion;
	}

	public boolean compareRegion(UltimateLayer id, ImageRegion imageRegion, LocalDateTime localDateTime)
	{
		if (this.imageRegion != null)
		{
			return (this.sourceId == id
					&& this.imageRegion.contains(imageRegion)
					&& this.imageRegion.compareScaleFactor(imageRegion) && localDateTime
					.isEqual(this.imageRegion.getDateTime()));
		}
		return false;
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
		imageRegion=null;
		
		if (TimeLine.SINGLETON.getCurrentDateTime().equals(imageRegion.getDateTime()))
			MainFrame.MAIN_PANEL.repaint();
	}
}