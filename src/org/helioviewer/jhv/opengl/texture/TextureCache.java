package org.helioviewer.jhv.opengl.texture;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;

public class TextureCache {

	private static final int SIZE_TEXTURE_CACHE = 10;
	private static Queue<CachableTexture> queue;

	static {
		int[] textures = OpenGLHelper.createTextureIDs(SIZE_TEXTURE_CACHE);
		queue = new ArrayBlockingQueue<TextureCache.CachableTexture>(SIZE_TEXTURE_CACHE);

		for (int texture : textures) {
			queue.add(new CachableTexture(texture));
		}

	}

	public static void init() {

	}

	public static Queue<CachableTexture> getCacheableTextures() {
		return queue;
	}

	public static ImageRegion addElement(ImageRegion imageRegion, int id) {
		// CachableTexture texture = concurrentLinkedDeque.peekLast();
		CachableTexture texture = queue.poll();
		texture.setNewImageRegion(id, imageRegion);
		queue.add(texture);
		// concurrentLinkedDeque.addFirst(texture);
		return imageRegion;
	}

	public static void setElementAsFist(CachableTexture texture) {
		queue.remove(texture);
		queue.add(texture);
		// concurrentLinkedDeque.remove(texture);
		// concurrentLinkedDeque.addFirst(texture);
	}

	public static class CachableTexture {
		private int id = -1;
		private ImageRegion imageRegion;
		private int textureID;
		private boolean changed = false;

		private CachableTexture(int textureID) {
			this.textureID = textureID;
		}

		private void setNewImageRegion(int id, ImageRegion imageRegion) {
			this.id = id;
			this.changed = false;
			imageRegion.setTextureID(this.textureID);
			this.imageRegion = imageRegion;
		}

		public boolean compareRegion(int id, ImageRegion imageRegion,
				LocalDateTime localDateTime) {
			boolean retVal = false;
			if (this.imageRegion != null) {
				retVal = (this.id == id
						&& this.imageRegion.contains(imageRegion)
						&& this.imageRegion.compareScaleFactor(imageRegion) && localDateTime
						.isEqual(this.imageRegion.getDateTime()));
			}
			return retVal;
		}

		public boolean compareTexture(int id2, LocalDateTime[] localDateTimes) {
			boolean retVal = false;
			if (localDateTimes != null) {
				for (LocalDateTime localDateTime : localDateTimes) {
					if (this.imageRegion != null
							&& localDateTime.equals(this.imageRegion
									.getDateTime()))
						return true;
				}
			}
			return retVal;
		}

		public ImageRegion getImageRegion() {
			return this.imageRegion;
		}

		public boolean hasChanged() {
			return changed;
		}

		public void markAsChanged() {
			if (TimeLine.SINGLETON.getCurrentDateTime().equals(
					imageRegion.getDateTime()))
				MainFrame.MAIN_PANEL.repaintMain(20);
			changed = true;
		}

	}
}
