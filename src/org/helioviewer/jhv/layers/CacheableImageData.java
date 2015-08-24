package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;

import kdu_jni.Jp2_threadsafe_family_src;
import kdu_jni.Jpx_source;
import kdu_jni.KduException;
import kdu_jni.Kdu_cache;
import kdu_jni.Kdu_global;
import kdu_jni.Kdu_region_compositor;
import kdu_jni.Kdu_thread_env;

import org.helioviewer.jhv.layers.AbstractImageLayer.CACHE_STATUS;
import org.helioviewer.jhv.opengl.texture.TextureCache;
import org.helioviewer.jhv.opengl.texture.TextureCache.CachableTexture;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class CacheableImageData {

	private LocalDateTime[] localDateTimes;

	private LocalDateTime firstDate = LocalDateTime.MAX;
	private LocalDateTime lastDate = LocalDateTime.MIN;

	private int id;
	private Kdu_cache kduCache;
	private int lastDetectedDate;

	private String fileName = null;

	private CACHE_STATUS cacheStatus = CACHE_STATUS.NONE;

	private Jp2_threadsafe_family_src family_src = new Jp2_threadsafe_family_src();
	private Jpx_source jpxSrc = new Jpx_source();

	private Kdu_region_compositor compositor = new Kdu_region_compositor();

	private static final int CODESTREAM_CACHE_THRESHOLD = 1024 * 256;

	private MetaData[] metaDatas;
	
	public CacheableImageData(int id, Kdu_cache kduCache) {
		this.kduCache = kduCache;
		this.id = id;
		try {
			family_src.Open(kduCache);
			jpxSrc.Open(family_src, true);
			compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public CacheableImageData(int id, String fileName) {
		this.kduCache = null;
		this.id = id;
		this.fileName = fileName;
		try {
			family_src.Open(fileName);
			jpxSrc.Open(family_src, true);
			compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setMetadatas(MetaData[] metaDatas){
		this.metaDatas = metaDatas;
	}

	public void setTimeRange(LocalDateTime[] localDateTimes) {
		this.localDateTimes = localDateTimes;
		for (LocalDateTime localDateTime : this.localDateTimes) {
			firstDate = localDateTime.isBefore(firstDate) ? localDateTime
					: firstDate;
			lastDate = localDateTime.isAfter(lastDate) ? localDateTime
					: lastDate;
		}
	}

	public void setLocalDateTimes(LocalDateTime[] localDateTimes) {
		this.localDateTimes = localDateTimes;
		for (LocalDateTime localDateTime : this.localDateTimes) {
			firstDate = localDateTime.isBefore(firstDate) ? localDateTime
					: firstDate;
			lastDate = localDateTime.isAfter(lastDate) ? localDateTime
					: lastDate;
		}
	}

	public boolean contains(int id, LocalDateTime currentDate) {
		if (this.id != id || currentDate.isBefore(firstDate)
				|| currentDate.isAfter(lastDate))
			return false;
		lastDetectedDate = 0;
		for (LocalDateTime localDateTime : localDateTimes) {
			if (localDateTime.isEqual(currentDate)) {
				return true;
			}
			lastDetectedDate++;
		}
		return false;
	}

	public Kdu_region_compositor getSource() {
		return compositor;
	}

	public Kdu_cache getImageData() {
		return kduCache;
	}

	public String getImageFile() {
		return fileName;
	}

	public int getLstDetectedDate() {
		return lastDetectedDate;
	}

	public void markAsChanged(boolean kdu) {
		if (kdu)
			cacheStatus = CACHE_STATUS.KDU;
		for (CachableTexture cacheableTexture : TextureCache
				.getCacheableTextures()) {
			if (cacheableTexture.compareTexture(this.id, localDateTimes)) {
				cacheableTexture.markAsChanged();
			}
		}
	}

	public void setFile(String fileName) {
		this.fileName = fileName;
		kduCache.Native_destroy();
		kduCache = null;
		markAsChanged(false);
		try {
			Kdu_region_compositor compositor = new Kdu_region_compositor();
			Jp2_threadsafe_family_src family_src = new Jp2_threadsafe_family_src();
			Jpx_source jpxSrc = new Jpx_source();
			family_src.Open(fileName);
			jpxSrc.Open(family_src, true);
			compositor.Create(jpxSrc, CODESTREAM_CACHE_THRESHOLD);
			this.compositor = compositor;
			this.family_src = family_src;
			this.jpxSrc = jpxSrc;
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getIdx(LocalDateTime localDateTime) {
		int i;
		for (i = 0; i < localDateTimes.length; i++) {
			if (localDateTime.isEqual(localDateTimes[i]))
				break;
		}
		return i;
	}

	public CACHE_STATUS getCacheStatus() {
		if (fileName != null)
			return CACHE_STATUS.FILE;
		return cacheStatus;
	}

	public int getSize() {
		return localDateTimes.length;
	}

	public Jp2_threadsafe_family_src getFamilySrc() {
		return family_src;
	}

	public int getFrameCount() throws KduException {
		int[] tempVar = new int[1];
		jpxSrc.Count_compositing_layers(tempVar);
		return tempVar[0];
	}

	public MetaData getMetaData(int idx) {
		return metaDatas[idx];
	}

}
