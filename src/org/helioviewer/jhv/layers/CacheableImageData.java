package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.viewmodel.view.opengl.texture.TextureCache;
import org.helioviewer.jhv.viewmodel.view.opengl.texture.TextureCache.CachableTexture;

public class CacheableImageData {

	private LocalDateTime[] localDateTimes;
	
	private LocalDateTime firstDate = LocalDateTime.MAX;
	private LocalDateTime lastDate = LocalDateTime.MIN;
	
	private int id;
	private final Kdu_cache kduCache;
	private int lastDetectedDate;	
	
	public CacheableImageData(int id, LocalDateTime[] localDateTimes, Kdu_cache kduCache) {
		this.kduCache = kduCache;
		this.id = id;
		this.localDateTimes = localDateTimes;
		for (LocalDateTime localDateTime : this.localDateTimes){
			firstDate = localDateTime.isBefore(firstDate) ? localDateTime : firstDate;
			lastDate = localDateTime.isAfter(lastDate) ? localDateTime : lastDate;
		}
		
	}

	public boolean contains(int id, LocalDateTime currentDate) {
		if (this.id != id || currentDate.isBefore(firstDate) || currentDate.isAfter(lastDate)) return false;
		lastDetectedDate = 0;
		for (LocalDateTime localDateTime : localDateTimes){
			if (localDateTime.isEqual(currentDate)){
				return true;
			}
			lastDetectedDate++;
		}
		return false;
	}

	public Kdu_cache getImageData() {
		return kduCache;
	}
	
	public int getLstDetectedDate(){
		return lastDetectedDate;
	}
	
	public void markAsChanged(){
		for (CachableTexture cacheableTexture : TextureCache.getCacheableTextures()){
			if (cacheableTexture.compareTexture(this.id, localDateTimes)){
				cacheableTexture.markAsChanged();
			}
		}
	}
	
}
