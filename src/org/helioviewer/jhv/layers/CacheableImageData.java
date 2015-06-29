package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.opengl.texture.TextureCache;
import org.helioviewer.jhv.opengl.texture.TextureCache.CachableTexture;

public class CacheableImageData {

	private LocalDateTime[] localDateTimes;
	
	private LocalDateTime firstDate = LocalDateTime.MAX;
	private LocalDateTime lastDate = LocalDateTime.MIN;
	
	private int id;
	private Kdu_cache kduCache;
	private int lastDetectedDate;
	
	private String fileName = null;
	
	public CacheableImageData(int id, LocalDateTime[] localDateTimes, Kdu_cache kduCache) {
		this.kduCache = kduCache;
		this.id = id;
		this.localDateTimes = localDateTimes;
		for (LocalDateTime localDateTime : this.localDateTimes){
			firstDate = localDateTime.isBefore(firstDate) ? localDateTime : firstDate;
			lastDate = localDateTime.isAfter(lastDate) ? localDateTime : lastDate;
		}
		
	}
	
	public CacheableImageData(int id, Kdu_cache kduCache) {
		this.kduCache = kduCache;
		this.id = id;
	}
	
	
	public CacheableImageData(int id, LocalDateTime[] localDateTimes, String fileName){
		this.kduCache = null;
		this.id = id;
		this.localDateTimes = localDateTimes;
		this.fileName = fileName;
		for (LocalDateTime localDateTime : this.localDateTimes){
			firstDate = localDateTime.isBefore(firstDate) ? localDateTime : firstDate;
			lastDate = localDateTime.isAfter(lastDate) ? localDateTime : lastDate;
		}
	}
	
	public void setLocalDateTimes(LocalDateTime[] localDateTimes){
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
	
	public String getImageFile(){
		return fileName;
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

	public void setFile(String fileName) {
		this.fileName = fileName;
		kduCache.Native_destroy();
		kduCache = null;
		markAsChanged();
	}

	public int getIdx(LocalDateTime localDateTime) {
		int i;
		for (i = 0; i < localDateTimes.length; i++){
			if (localDateTime.isEqual(localDateTimes[i])) break;
		}
		return i+1;
	}
	
}
