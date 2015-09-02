package org.helioviewer.jhv.opengl;

import java.time.LocalDateTime;
import java.util.LinkedList;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.TimeLine;

//FIXME: handle concurrency, by using the cache only from awt/gl thread
public class TextureCache
{
	//FIXME: handle pool running out
	private static final int TEXTURE_CACHE_SIZE = 10;
	private static LinkedList<CachedTexture> cache = new LinkedList<CachedTexture>();
	
	static
	{
		int[] textures = OpenGLHelper.createTextureIDs(TEXTURE_CACHE_SIZE);
		for (int texture : textures)
			cache.add(new CachedTexture(texture));
	}
	
	public static void init()
	{
	}

	public synchronized static ImageRegion addElement(ImageRegion imageRegion, int id)
	{
		CachedTexture texture = cache.removeFirst();
		texture.setNewImageRegion(id, imageRegion);
		cache.add(texture);
		return imageRegion;
	}

	private static void moveElementToFront(CachedTexture texture)
	{
		cache.remove(texture);
		cache.add(texture);
	}
	
	public synchronized static void markChanged(int id2, LocalDateTime[] localDateTimes)
	{
		for (CachedTexture cacheableTexture : cache)
			if (cacheableTexture.compareTexture(id2, localDateTimes))
				cacheableTexture.markAsChanged();
	}

	public synchronized static ImageRegion search(int id, ImageRegion imageRegion, LocalDateTime localDateTime)
	{
		for (CachedTexture texture : cache)
		{
			if (texture.compareRegion(id, imageRegion, localDateTime) && !texture.hasChanged())
			{
				TextureCache.moveElementToFront(texture);
				return texture.getImageRegion();
			}
		}
		
		return null;
	}

	public static class CachedTexture
	{
		private int id = -1;
		private ImageRegion imageRegion;
		private int textureID;
		private boolean changed = false;

		private CachedTexture(int textureID) {
			this.textureID = textureID;
		}

		private void setNewImageRegion(int id, ImageRegion imageRegion) {
			this.id = id;
			this.changed = false;
			imageRegion.setTextureID(this.textureID);
			this.imageRegion = imageRegion;
		}

		public boolean compareRegion(int id, ImageRegion imageRegion, LocalDateTime localDateTime)
		{
			if (this.imageRegion != null)
			{
				return (this.id == id
						&& this.imageRegion.contains(imageRegion)
						&& this.imageRegion.compareScaleFactor(imageRegion) && localDateTime
						.isEqual(this.imageRegion.getDateTime()));
			}
			return false;
		}

		public boolean compareTexture(int id2, LocalDateTime[] localDateTimes)
		{
			if (localDateTimes == null)
				return false;

			for (LocalDateTime localDateTime : localDateTimes)
				if (this.imageRegion != null && localDateTime.equals(this.imageRegion.getDateTime()))
					return true;
			
			return false;
		}

		public ImageRegion getImageRegion()
		{
			return this.imageRegion;
		}

		public boolean hasChanged()
		{
			return changed;
		}

		public void markAsChanged()
		{
			changed = true;
			
			if (TimeLine.SINGLETON.getCurrentDateTime().equals(imageRegion.getDateTime()))
				MainFrame.MAIN_PANEL.repaint();
		}
	}
}
