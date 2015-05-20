package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.layers.CacheableImageData;
import org.helioviewer.jhv.layers.NewLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.opengl.texture.TextureCache;
import org.helioviewer.jhv.viewmodel.view.opengl.texture.TextureCache.CachableTexture;
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

	public static final int MAX_FRAME_SIZE = 300;
	private static final String URL = "http://api.helioviewer.org/v2/getJPX/?";
	private static final int MAX_THREAD_PER_LAYER = 2;
	private static final int MAX_THREAD_PER_LOAD_JPIP_URLS = 4;
	public static final int RESOLUTION_LEVEL_COUNT = 8;

	private ExecutorService executorService;

	private NewCache cache;
	private NewRender render;

	private String fileName = null;
	private LocalDateTime[] localDateTimes;
	private TreeSet<LocalDateTime> treeSet;
	private NewLayer newLayer;

	private ImageRegion imageRegion;

	private int id;

	public UltimateLayer(int id, int sourceID, NewCache cache,
			NewRender render, NewLayer newLayer) {
		treeSet = new TreeSet<LocalDateTime>();
		this.id = id;
		this.newLayer = newLayer;
		this.sourceID = sourceID;
		this.cache = cache;
		this.render = render;
		this.executorService = Executors
				.newFixedThreadPool(MAX_THREAD_PER_LAYER);
	}

	public UltimateLayer(int id, String filename, NewRender render,
			NewLayer newLayer) {
		treeSet = new TreeSet<LocalDateTime>();
		this.id = id;
		this.newLayer = newLayer;
		this.sourceID = 0;
		this.render = render;
		this.fileName = filename;
		try {
			this.render.closeImage();
			this.render.openImage(filename);
			int framecount = this.render.getFrameCount();
			localDateTimes = new LocalDateTime[framecount];
			for (int i = 1; i <= framecount; i++) {
				MetaData metaData = render.getMetadata(i);
				localDateTimes[i - 1] = metaData.getLocalDateTime();
			}
			this.render.closeImage();
		} catch (KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.timeArrayChanged();
	}

	public boolean isLocalFile() {
		return fileName != null;
	}

	public UltimateLayer(int id, String observatory, String instrument,
			String measurement1, String measurement2) {
		treeSet = new TreeSet<LocalDateTime>();
		this.id = id;
		this.observatory = observatory;
		this.instrument = instrument;
		this.measurement1 = measurement1;
		this.measurement2 = measurement2;
	}

	public void setTimeRange(final LocalDateTime start,
			final LocalDateTime end, final int cadence) {		
		
		ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREAD_PER_LOAD_JPIP_URLS);		
		
		URILoader uriLoader;
		
		LocalDateTime tmp = LocalDateTime.MIN;
		LocalDateTime currentStart = start;
		
		while (tmp.isBefore(end)) {
			tmp = currentStart.plusSeconds(cadence
					* (MAX_FRAME_SIZE - 1));
			
			uriLoader = new URILoader();
			uriLoader.start = currentStart;
			uriLoader.end = tmp;
			uriLoader.cadence = cadence;
			executorService.execute(uriLoader);
			
			currentStart = tmp;
		}
		
		
		
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				LocalDateTime tmp = LocalDateTime.MIN;
				LocalDateTime currentStart = start;
				ArrayList<LocalDateTime> dateTimes = new ArrayList<LocalDateTime>();
				
				localDateTimes = dateTimes.toArray(new LocalDateTime[dateTimes
						.size()]);
				if (localDateTimes.length > 0)
					timeArrayChanged();
			}
		}, "UltimateLayer_prepare_requests");

		thread.start();

	}

	public int getFrameCount() {
		if (fileName != null)
			return localDateTimes.length;
		return 1;
	}

	public MetaData getMetaData(LocalDateTime currentDateTime)
			throws InterruptedException, ExecutionException, JHV_KduException {
		if (fileName != null)
			return this.getMetaData(TimeLine.SINGLETON.getCurrentFrame());

		if (cache.getCacheElement(this.id, currentDateTime) == null)
			return null;
		FutureTask<JHVCachable> currentLayerCache = cache.getCacheElement(
				this.id, currentDateTime).getImageData();
		if (currentLayerCache == null)
			return null;

		KakaduCache layer = ((KakaduCache) currentLayerCache.get());

		render.openImage(layer.getCache());
		MetaData metaData = this.render.getMetadata(0);
		render.closeImage();
		return metaData;
	}

	public MetaData getMetaData(int index) {
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

	public LocalDateTime getLocalDateTime(int index) {
		return localDateTimes[index];
	}

	@Deprecated
	public LocalDateTime[] getLocalDateTimes() {
		return localDateTimes;
	}

	private ByteBuffer getImageFromLocalFile(LocalDateTime currentDate,
			float zoomFactor, Rectangle imageSize) throws JHV_KduException {
		render.openImage(fileName);

		ByteBuffer intBuffer = render.getImage(
				TimeLine.SINGLETON.getCurrentFrame(), 8, zoomFactor, imageSize);
		render.closeImage();
		return intBuffer;
	}

	public ByteBuffer getImageData(LocalDateTime currentDateTime,
			ImageRegion imageRegion, MetaData metaData, boolean highResolution)
			throws InterruptedException, ExecutionException, JHV_KduException {
		// newLayer.getImageRegion().calculateScaleFactor(newLayer, camera);
		Queue<CachableTexture> textures = TextureCache.singleton
				.getCacheableTextures();
		System.out.println("id : " + this.id);
		for (CachableTexture texture : textures) {
			if (texture.compareRegion(id, imageRegion, currentDateTime)) {
				this.imageRegion = texture.getImageRegion();
				TextureCache.singleton.setElementAsFist(texture);
				System.out.println("region exist");
				return null;
			}
		}
		System.out.println("new image region");

		if (fileName != null) {
			imageRegion.setLocalDateTime(currentDateTime);
			this.imageRegion = TextureCache.singleton.addElement(imageRegion,
					id);
			this.imageRegion.setMetaData(metaData);
			return getImageFromLocalFile(currentDateTime,
					this.imageRegion.getZoomFactor(),
					this.imageRegion.getImageSize());
		}

		CacheableImageData cacheObject = cache.getCacheElement(id,
				currentDateTime);
		FutureTask<JHVCachable> currentLayerCache = cacheObject.getImageData();
		if (currentLayerCache == null)
			return null;

		KakaduCache layer = ((KakaduCache) currentLayerCache.get());

		imageRegion.setLocalDateTime(currentDateTime);
		this.imageRegion = TextureCache.singleton.addElement(imageRegion, id);
		this.imageRegion.setMetaData(metaData);

		System.out.println("size : " + layer.getSize());
		System.out
				.println("---------------------getImage---------------------");
		render.openImage(layer.getCache());

		ByteBuffer intBuffer = render.getImage(
				cacheObject.getLstDetectedDate(), 8,
				this.imageRegion.getZoomFactor(),
				this.imageRegion.getImageSize());
		render.closeImage();
		return intBuffer;
	}

	public ImageRegion getImageRegion() {
		return this.imageRegion;
	}

	@Deprecated
	private void timeArrayChanged() {
		LocalDateTime[] localDateTimes = new LocalDateTime[treeSet.size()];
		TimeLine.SINGLETON.updateLocalDateTimes(this.treeSet.toArray(localDateTimes));
	}

	private class URILoader implements Runnable{
		private LocalDateTime start, end;
		private int cadence;
		
		@Override
		public void run() {
			StringBuilder sb = new StringBuilder();
			DateTimeFormatter formatter = DateTimeFormatter
					.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			String request = "startTime="
					+ start.format(formatter) + "&endTime="
					+ end.format(formatter) + "&sourceId=" + sourceID
					+ "&jpip=true&verbose=true&cadence=" + cadence;

			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(new URL(URL + request)
							.openStream()))) {
				String line = null;

				while ((line = in.readLine()) != null) {
					sb.append(line);
				}
				JSONObject jsonObject = new JSONObject(sb.toString());

				try {
					if (jsonObject.getString("error") != null) {
						return;
					}
				} catch (JSONException e) {
					// TODO: handle exception
				}

				System.out.println(jsonObject.get("frames"));
				JSONArray frames = ((JSONArray) jsonObject
						.get("frames"));
				LocalDateTime[] framesDateTime = new LocalDateTime[frames
						.length()];
				for (int i = 0; i < frames.length(); i++) {
					Timestamp timestamp = new Timestamp(frames
							.getLong(i) * 1000L);
					framesDateTime[i] = timestamp.toLocalDateTime();
					treeSet.add(timestamp.toLocalDateTime());
				}
				String jpipURL = jsonObject.getString("uri");
				NewReader reader = new NewReader(jpipURL, sourceID);
				FutureTask<JHVCachable> futureTask = reader
						.getData(framesDateTime);
				executorService.execute(futureTask);
				CacheableImageData cacheableImageData = new CacheableImageData(
						id, framesDateTime, futureTask);
				// futureTask.get();
				cache.addCacheElement(cacheableImageData);

				timeArrayChanged();

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
			
		}
				
	}
}
