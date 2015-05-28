package org.helioviewer.jhv.viewmodel.view.opengl.texture;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Queue;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.opengl.OpenGLHelper;

public class TextureCache {

	private final int SIZE_TEXTURE_CACHE = 10;
	private Queue<CachableTexture> queue;
	public static TextureCache singleton = new TextureCache();
	
	private TextureCache() {
			int[] textures = OpenGLHelper.createTextureIDs(SIZE_TEXTURE_CACHE);
			queue = new ArrayDeque<TextureCache.CachableTexture>(SIZE_TEXTURE_CACHE);

			for (int texture : textures){
				queue.add(new CachableTexture(texture));
			}
		
	}
		
	public Queue<CachableTexture> getCacheableTextures(){
		return this.queue;
	}
	
	public ImageRegion addElement(ImageRegion imageRegion, int id){
		//CachableTexture texture = concurrentLinkedDeque.peekLast();
		CachableTexture texture = queue.peek();
		texture.setNewImageRegion(id, imageRegion);
		queue.add(texture);
		//concurrentLinkedDeque.addFirst(texture);
		return imageRegion;
	}
	
	public void setElementAsFist(CachableTexture texture){
		queue.remove(texture);
		queue.add(texture);
		//concurrentLinkedDeque.remove(texture);
		//concurrentLinkedDeque.addFirst(texture);
	}
	
	
	public class CachableTexture{
		private int id = -1;
		private ImageRegion imageRegion;
		private int textureID;
		
		private CachableTexture(int textureID) {
			this.textureID = textureID;
		}
		
		private void setNewImageRegion(int id, ImageRegion imageRegion){
			this.id = id;
			imageRegion.setTextureID(this.textureID);
			this.imageRegion = imageRegion;
		}

		public boolean compareRegion(int id, ImageRegion imageRegion, LocalDateTime localDateTime){
			boolean retVal = false;
			if (this.imageRegion != null){
				retVal = (this.id == id && this.imageRegion.contains(imageRegion) && this.imageRegion.compareScaleFactor(imageRegion) && localDateTime.isEqual(this.imageRegion.getDateTime()));
			}
			return retVal;
		}

		public ImageRegion getImageRegion() {
			return this.imageRegion;
		}
	}
}
