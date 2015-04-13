package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.IntBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import kdu_jni.KduException;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UltimateLayer {
	
	public String observatory;
	public String instrument;
	public String measurement1;
	public String measurement2;
	public int sourceID;
	
	public NewReader reader;
	
	public static final int MAX_FRAME_SIZE = 10;
	private static final String URL = "http://api.helioviewer.org/v2/getJPX/?";
	private static final int MAX_THREAD_PER_LAYER = 2;
	public static final int RESOLUTION_LEVEL_COUNT = 8;
	
	private ExecutorService executorService;
	
	private NewCache cache;
	private ResolutionSet resolutionSet;
	private NewRender render;
	
	private String fileName = null;
	private LocalDateTime[] localDateTimes;
	
	
	public UltimateLayer(int sourceID, NewCache cache, NewRender render){
		this.sourceID = sourceID;
		this.cache = cache;
		this.render = render;
		this.loadResolutionSet();
		this.executorService = Executors.newFixedThreadPool(MAX_THREAD_PER_LAYER);
	}
	
	public UltimateLayer(String filename, NewRender render){
		this.sourceID = 0;
		this.render = render;
		this.fileName = filename;
		this.loadResolutionSet();
		try {
			this.render.closeImage();
			this.render.openImage(filename);
			int framecount = this.render.getFrameCount();
			localDateTimes = new LocalDateTime[framecount];
			for (int i = 1; i <= framecount; i++){
				MetaData metaData = render.getMetadata(i);
				localDateTimes[i-1] = metaData.getLocalDateTime();
			}
			this.render.closeImage();
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public boolean isLocalFile(){
		return fileName != null;
	}
	
	private void loadResolutionSet() {
		this.resolutionSet = new ResolutionSet(RESOLUTION_LEVEL_COUNT);
		int resolution = 4096;
		for (int i = 0; i < RESOLUTION_LEVEL_COUNT; i++){
			resolutionSet.addResolutionLevel(i, new Rectangle(resolution, resolution));
			resolution /= 2;
		}
	}

	public UltimateLayer(String observatory, String instrument, String measurement1, String measurement2) {
		this.observatory = observatory;
		this.instrument = instrument;
		this.measurement1 = measurement1;
		this.measurement2 = measurement2;
	}	
	
	public void setTimeRange(LocalDateTime start, LocalDateTime end, int cadence){
		LocalDateTime tmp = LocalDateTime.MIN;
		while (tmp.isBefore(end)){			
			tmp = start.plusSeconds(cadence*(MAX_FRAME_SIZE-1));
			StringBuilder sb = new StringBuilder();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			System.out.println("tmp : " + tmp);
			System.out.println("start : " + start);
			System.out.println("end : " + end);
			String request = "startTime="+start.format(formatter)+"&endTime="+tmp.format(formatter)+"&sourceId="+sourceID+"&jpip=true&verbose=true&cadence="+cadence;

			System.out.println(URL+request);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(URL+request).openStream()))){
				String line = null;
				
				while((line = in.readLine()) != null)
				{
					sb.append(line);
				}
				JSONObject jsonObject = new JSONObject(sb.toString());
				System.out.println(sb.toString());
				System.out.println(jsonObject.getString("error"));
				if (jsonObject.getString("error") != null){
					break;
				}
				System.out.println(jsonObject.get("frames"));
				JSONArray frames = ((JSONArray)jsonObject.get("frames"));
				LocalDateTime[] framesDateTime = new LocalDateTime[frames.length()];
				for (int i = 0; i < frames.length(); i++){
					Timestamp timestamp = new Timestamp(frames.getLong(i)*1000L);
					framesDateTime[i] = timestamp.toLocalDateTime();
					
				}
				System.out.println(jsonObject.get("uri"));
				String jpipURL = jsonObject.getString("uri");
				NewReader reader = new NewReader(jpipURL, sourceID, resolutionSet);
				FutureTask<JHVCachable> futureTask = reader.getData(framesDateTime);
				executorService.execute(futureTask);
				//futureTask.get();
				cache.addCacheElement(futureTask);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			start = tmp;
		}
	}
	
	public int getFrameCount(){
		if (fileName != null) return localDateTimes.length;
		return 1;
	}	
	
	public MetaData getMetaData(int index){
		render.openImage(fileName);
		MetaData metaData = null;
		try {
			metaData = render.getMetadata(index);			
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		render.closeImage();
		return metaData;
	}
	
	public LocalDateTime getLocalDateTime(int index){
		return localDateTimes[index];
	}
	
	private IntBuffer getImageFromLocalFile(LocalDateTime currentDate, SubImage subImage){
		render.openImage(fileName);
		float zoomPercent = this.resolutionSet.getResolutionLevel(0).getZoomPercent();
		IntBuffer intBuffer = render.getImage(0, 8, zoomPercent, subImage);
		render.closeImage();
		return intBuffer;
	}
		
	public IntBuffer getImageData(LocalDateTime currentDate, SubImage subImage) throws InterruptedException, ExecutionException{
		if (fileName != null) return getImageFromLocalFile(currentDate, subImage);
		boolean complete = false;
		ImageLayer layer = null;
		while (!complete){
			FutureTask<JHVCachable> currentLayerCache = cache.getCacheElement(currentDate);
			layer = ((ImageLayer)currentLayerCache.get());	
			if (layer != null)
			complete = layer.isComplete();
			else
				Thread.sleep(100);
		}
		System.out.println("size : " + layer.getSize());
		System.out.println("---------------------getImage---------------------");
		render.openImage(layer.getCache());
		float zoomPercent = this.resolutionSet.getResolutionLevel(0).getZoomPercent();
		IntBuffer intBuffer = render.getImage(0, 8, zoomPercent, subImage);
		render.closeImage();
		return intBuffer;
	}
	
	public static void main(String[] args) {
		LocalDateTime start = LocalDateTime.of(2014, 01, 01, 0, 0, 0);
		LocalDateTime end = LocalDateTime.of(2014, 01, 01, 0, 45, 0);
		//UltimateLayer ultimateLayer = new UltimateLayer(10, new NewCache(), new NewRender());
		//ultimateLayer.setTimeRange(start, end, 90);
		UltimateLayer ultimateLayer = new UltimateLayer("/Users/binchu/JHelioviewer/Downloads/test.jp2", new NewRender());
		SubImage subImage = new SubImage(new Rectangle(0, 0, 4096, 4096));
		ultimateLayer.getMetaData(0);
		try {
			ultimateLayer.getImageData(start, subImage);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			System.out.println(e);
		}
		
	}

}
