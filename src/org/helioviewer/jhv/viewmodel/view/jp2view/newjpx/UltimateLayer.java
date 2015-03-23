package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.swing.text.DateFormatter;

import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
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
	
	public UltimateLayer(int sourceID, NewCache cache, NewRender render){
		this.sourceID = sourceID;
		this.cache = cache;
		this.render = render;
		this.loadResolutionSet();
		this.executorService = Executors.newFixedThreadPool(MAX_THREAD_PER_LAYER);
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
	
	
	
	
	public void getImageData(LocalDateTime currentDate, SubImage subImage) throws InterruptedException, ExecutionException{
		FutureTask<JHVCachable> currentLayerCache = cache.getCacheElement(currentDate);
		ImageLayer layer = ((ImageLayer)currentLayerCache.get());
		if (layer == null) return;
		System.out.println("size : " + layer.getSize());
		render.openImage(((ImageLayer)currentLayerCache.get()).getCache());
		float zoomPercent = this.resolutionSet.getResolutionLevel(0).getZoomPercent();
		int[] imageData = render.getImage(0, 8, zoomPercent, subImage);
		System.out.println(imageData);
	}
	
	public static void main(String[] args) {
		LocalDateTime start = LocalDateTime.of(2014, 01, 01, 0, 0, 0);
		LocalDateTime end = LocalDateTime.of(2014, 01, 01, 0, 45, 0);
		UltimateLayer ultimateLayer = new UltimateLayer(10, new NewCache(), new NewRender());
		ultimateLayer.setTimeRange(start, end, 90);
		SubImage subImage = new SubImage(new Rectangle(0, 0, 4096, 4096));
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
