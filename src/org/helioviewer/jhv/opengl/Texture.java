package org.helioviewer.jhv.opengl;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.TimeLine;

class Texture
{
	private int sourceId = -1;
	private ImageRegion imageRegion;
	private int openGLTextureId;
	private boolean invalid = false;

	Texture(int _openGLTextureId)
	{
		this.openGLTextureId = _openGLTextureId;
	}

	void setNewImageRegion(int _sourceId, ImageRegion _imageRegion)
	{
		this.sourceId = _sourceId;
		this.invalid = false;
		_imageRegion.setOpenGLTextureId(openGLTextureId);
		this.imageRegion = _imageRegion;
	}

	public boolean compareRegion(int id, ImageRegion imageRegion, LocalDateTime localDateTime)
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

	public boolean compareTexture(int _sourceId, LocalDateTime[] localDateTimes)
	{
		if (localDateTimes == null)
			return false;

		for (LocalDateTime localDateTime : localDateTimes)
			if (imageRegion != null && localDateTime.equals(imageRegion.getDateTime()))
				return true;
		
		return false;
	}

	public ImageRegion getImageRegion()
	{
		return imageRegion;
	}

	public boolean isValid()
	{
		return !invalid;
	}

	public void invalidate()
	{
		invalid = true;
		
		if (TimeLine.SINGLETON.getCurrentDateTime().equals(imageRegion.getDateTime()))
			MainFrame.MAIN_PANEL.repaint();
	}
}