package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.JHVCachable;

public class CacheableImageData {

	private LocalDateTime[] localDateTimes;
	private FutureTask<JHVCachable> imageCache;
	
	private LocalDateTime firstDate = LocalDateTime.MAX;
	private LocalDateTime lastDate = LocalDateTime.MIN;
	
	private int id;
	
	private int lastDetectedDate;
	
	public CacheableImageData(int id, LocalDateTime[] localDateTimes, FutureTask<JHVCachable> highResolutionData) {
		this.id = id;
		this.localDateTimes = localDateTimes;
		for (LocalDateTime localDateTime : this.localDateTimes){
			firstDate = localDateTime.isBefore(firstDate) ? localDateTime : firstDate;
			lastDate = localDateTime.isAfter(lastDate) ? localDateTime : lastDate;
		}
		this.imageCache = highResolutionData;
		
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

	public FutureTask<JHVCachable> getImageData() {
		try {
			imageCache.get(100, TimeUnit.MILLISECONDS);
			return imageCache;
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public int getLstDetectedDate(){
		return lastDetectedDate;
	}
		
	
}
