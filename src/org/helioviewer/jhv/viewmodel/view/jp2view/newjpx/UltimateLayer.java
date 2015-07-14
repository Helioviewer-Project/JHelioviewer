package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.JHVException;
import org.helioviewer.jhv.JHVException.CacheException;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.layers.CacheableImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.texture.TextureCache;
import org.helioviewer.jhv.opengl.texture.TextureCache.CachableTexture;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
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

	public static final int MAX_FRAME_SIZE = 10;
	private static final String URL = "http://api.helioviewer.org/v2/getJPX/?";
	private static final String URL_MOVIES = "http://api.helioviewer.org/jp2";
	private static final int MAX_THREAD_PER_LAYER = 2;
	private static final int MAX_THREAD_PER_LOAD_JPIP_URLS = 4;

	private KakaduRender render;

	private String fileName = null;
	private TreeSet<LocalDateTime> treeSet;

	private ImageRegion imageRegion;
	private Thread jpipURLLoader;
	private boolean localFile = false;
	private ImageLayer imageLayer;
	private int id;
	
	private ArrayList<AbstractRequest> requests = new ArrayList<AbstractRequest>();

	private ArrayList<AbstractRequest> badRequests = new ArrayList<AbstractRequest>();
	
	public UltimateLayer(int id, int sourceID, KakaduRender render,
			ImageLayer newLayer) {
		treeSet = new TreeSet<LocalDateTime>();
		this.id = id;
		this.imageLayer = newLayer;
		this.sourceID = sourceID;
		this.render = render;
	}

	public UltimateLayer(int id, String filename, KakaduRender render,
			ImageLayer newLayer) {
		treeSet = new TreeSet<LocalDateTime>();
		this.id = id;
		this.sourceID = 0;
		this.imageLayer = newLayer;
		this.render = render;
		this.localFile = true;
		this.fileName = filename;
		try {
			this.render.closeImage();
			this.render.openImage(filename);
			int framecount = this.render.getFrameCount();
			LocalDateTime[] localDateTimes = new LocalDateTime[framecount];
			for (int i = 1; i <= framecount; i++) {
				MetaData metaData = render.getMetadata(i);
				treeSet.add(metaData.getLocalDateTime());
				localDateTimes[i - 1] = metaData.getLocalDateTime();
			}
			Cache.addCacheElement(new CacheableImageData(id, localDateTimes,
					filename));

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

	public void setTimeRange(final LocalDateTime start,
			final LocalDateTime end, final int cadence) {

		jpipURLLoader = new Thread(new Runnable() {
			private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			private ArrayList<HTTPRequest> httpRequests = new ArrayList<HTTPRequest>();
			private HashMap<HTTPRequest, JPIPDownloadRequest> jpipDownloadRequests = new HashMap<HTTPRequest, JPIPDownloadRequest>();
			private ArrayList<JPIPDownloadRequest> downloadRequests = new ArrayList<JPIPDownloadRequest>();
			
			@Override
			public void run() {
				LocalDateTime tmp = LocalDateTime.MIN;
				LocalDateTime currentStart = start;
				int counter = 0;
				CacheableImageData[] cacheableImageDatas;
				while (tmp.isBefore(end)) {
					tmp = currentStart.plusSeconds(cadence
							* (MAX_FRAME_SIZE - 1));
					String request = "startTime="
							+ currentStart.format(formatter) + "&endTime="
							+ tmp.format(formatter) + "&sourceId=" + sourceID
							+ "&jpip=true&verbose=true&cadence=" + cadence;
					HTTPRequest httpRequest = new HTTPRequest(URL + request,
							PRIORITY.URGENT);
					requests.add(httpRequest);
					UltimateDownloadManager.addRequest(httpRequest);
					httpRequests.add(httpRequest);

					request = "startTime=" + currentStart.format(formatter)
							+ "&endTime=" + tmp.format(formatter)
							+ "&sourceId=" + sourceID + "&cadence=" + cadence;

					CacheableImageData cacheableImageData = new CacheableImageData(
							id, new Kdu_cache());
					JPIPDownloadRequest jpipDownloadRequest = new JPIPDownloadRequest(
							URL + request, PRIORITY.LOW, cacheableImageData,
							requests);
					jpipDownloadRequests.put(httpRequest, jpipDownloadRequest);
					downloadRequests.add(jpipDownloadRequest);
					requests.add(jpipDownloadRequest);
					UltimateDownloadManager.addRequest(jpipDownloadRequest);
					currentStart = tmp.plusSeconds(1);
				}

				boolean finished = false;

				while (!finished) {
					if (Thread.interrupted())
						return;
					for (HTTPRequest httpRequest : httpRequests) {
						finished = false;
						finished &= httpRequest.isFinished();
						JPIPDownloadRequest jpipDownloadRequest = jpipDownloadRequests
								.get(httpRequest);
						if (httpRequest.isFinished()
								&& requests.contains(httpRequest)) {
							JSONObject jsonObject;
							try {
								jsonObject = new JSONObject(httpRequest
										.getDataAsString());

								if (jsonObject.has("error")) {
									System.out.println("error during : "
											+ httpRequest);
									requests.remove(httpRequest);
									break;
								}

								JSONArray frames = ((JSONArray) jsonObject
										.get("frames"));
								LocalDateTime[] localDateTimes = new LocalDateTime[frames
										.length()];
								for (int i = 0; i < frames.length(); i++) {
									Timestamp timestamp = new Timestamp(frames
											.getLong(i) * 1000L);
									localDateTimes[i] = timestamp
											.toLocalDateTime();
								}

								String jpipURI = jsonObject.getString("uri");

								CacheableImageData cacheableImageData = jpipDownloadRequest
										.getCachaableImageData();
								cacheableImageData
										.setLocalDateTimes(localDateTimes);
								jpipDownloadRequests.remove(httpRequest);
								Cache.addCacheElement(cacheableImageData);
								addFramedates(localDateTimes);

								JPIPRequest jpipRequestLow = new JPIPRequest(
										jpipURI, PRIORITY.HIGH, 0, frames
												.length(), new Rectangle(256,
												256), cacheableImageData);

								requests.add(jpipRequestLow);

								UltimateDownloadManager
										.addRequest(jpipRequestLow);


							} catch (JSONException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (IOException e) {
								badRequests.add(httpRequest);
							}
							requests.remove(httpRequest);
						}
					}
				}
				httpRequests.clear();
				
				finished = false;
				while (!finished) {
					for (JPIPDownloadRequest request : downloadRequests){
						if (Thread.interrupted())
							return;
						if (request.isFinished()){
							try {
								request.checkException();
							} catch (IOException e) {
								badRequests.add(request);
							}
							requests.remove(request);
						}
						
					}
				}
				downloadRequests.clear();
				imageLayer.addBadRequest(badRequests);
			}
		}, "JPIP_URI_LOADER");

		jpipURLLoader.start();
	}

	private void addFramedates(LocalDateTime[] localDateTimes) {
		for (LocalDateTime localDateTime : localDateTimes)
			treeSet.add(localDateTime);
		LocalDateTime[] allLocalDateTimes = treeSet
				.toArray(new LocalDateTime[treeSet.size()]);
		TimeLine.SINGLETON.updateLocalDateTimes(treeSet);
	}

	public MetaData getMetaData(LocalDateTime currentDateTime)
			throws InterruptedException, ExecutionException, JHV_KduException,
			CacheException {

		CacheableImageData cacheObject = null;
		if (treeSet.size() == 0)
			throw new JHVException.CacheException("no layer is loaded");
		
		LocalDateTime localDateTime = this.getNextLocalDateTime(currentDateTime);
		if (localDateTime == null)
			localDateTime = treeSet.last();

		cacheObject = Cache.getCacheElement(id, localDateTime);
		if (Cache.getCacheElement(this.id, localDateTime) == null)
			throw new JHVException.CacheException(
					"no cache at this time available");
		fileName = cacheObject.getImageFile();
		return this.getMetaData(localDateTime, cacheObject);
	}
	
	private LocalDateTime getNextLocalDateTime(LocalDateTime currentDateTime){
		LocalDateTime after = treeSet.ceiling(currentDateTime);
		LocalDateTime before = treeSet.floor(currentDateTime);
		long beforeValue = before != null ? ChronoUnit.SECONDS.between(before, currentDateTime) : Long.MAX_VALUE;
		long afterValue = after != null ? ChronoUnit.SECONDS.between(currentDateTime, after) : Long.MAX_VALUE;
		return beforeValue > afterValue ? after : before;
	}

	public MetaData getMetaData(LocalDateTime localDateTime,
			CacheableImageData cacheableImageData) {
		if (cacheableImageData.getImageFile() != null)
			render.openImage(cacheableImageData.getImageFile());
		else
			render.openImage(cacheableImageData.getImageData());
		MetaData metaData = null;
		try {
			metaData = render.getMetadata(cacheableImageData
					.getIdx(localDateTime) + 1);
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		render.closeImage();
		return metaData;
	}

	@Deprecated
	public TreeSet<LocalDateTime> getLocalDateTimes() {
		return this.treeSet;
	}

	private ByteBuffer getImageFromLocalFile(int idx, float zoomFactor,
			Rectangle imageSize) throws JHV_KduException {
		render.openImage(fileName);

		ByteBuffer intBuffer = render.getImage(idx, 8, zoomFactor, imageSize);
		render.closeImage();
		return intBuffer;
	}

	public ByteBuffer getImageData(LocalDateTime currentDateTime,
			ImageRegion imageRegion, MetaData metaData, boolean highResolution)
			throws InterruptedException, ExecutionException, JHV_KduException {
		Queue<CachableTexture> textures = TextureCache.getCacheableTextures();
		LocalDateTime localDateTime = this.getNextLocalDateTime(currentDateTime);
		if (localDateTime == null)
			localDateTime = treeSet.last();

		for (CachableTexture texture : textures) {
			if (texture.compareRegion(id, imageRegion, localDateTime)
					&& !texture.hasChanged()) {
				this.imageRegion = texture.getImageRegion();
				TextureCache.setElementAsFist(texture);
				return null;
			}
		}
		CacheableImageData cacheObject = null;

		cacheObject = Cache.getCacheElement(id, localDateTime);
		fileName = cacheObject.getImageFile();

		this.imageRegion = imageRegion;
		this.imageRegion.setLocalDateTime(localDateTime);
		this.imageRegion = TextureCache.addElement(imageRegion, id);
		this.imageRegion.setMetaData(metaData);
		this.imageRegion.setID(id);
		if (fileName != null) {
			return getImageFromLocalFile(cacheObject.getIdx(localDateTime),
					this.imageRegion.getZoomFactor(),
					this.imageRegion.getImageSize());
		}

		render.openImage(cacheObject.getImageData());

		ByteBuffer intBuffer = render.getImage(
				cacheObject.getIdx(localDateTime), 8,
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
		TimeLine.SINGLETON.updateLocalDateTimes(treeSet);
		LocalDateTime[] localDateTimes = new LocalDateTime[treeSet.size()];
		// TimeLine.SINGLETON.updateLocalDateTimes(this.treeSet
		// .toArray(localDateTimes));
	}

	public void cancelDownload() {
		if (jpipURLLoader != null && jpipURLLoader.isAlive())
			jpipURLLoader.interrupt();
		for (AbstractRequest request : requests) {
			UltimateDownloadManager.remove(request);
		}
	}

	public String getURL(){
		if (localFile)
			return null;
		DateTimeFormatter formatter = DateTimeFormatter
				.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		LocalDateTime start = treeSet.first();
		LocalDateTime end = treeSet.last();
		int cadence = 0;
		LocalDateTime last = null;
		for (LocalDateTime localDateTime : treeSet) {
			if (last != null) {
				cadence += ChronoUnit.SECONDS.between(last, localDateTime);
			}
			last = localDateTime;
			System.out.println(last);
		}
		cadence /= (treeSet.size() - 1);
		String request = "startTime=" + start.format(formatter) + "&endTime="
				+ end.format(formatter) + "&sourceId=" + sourceID + "&cadence="
				+ cadence;

		return URL + request;
	}

	public boolean isLocalFile() {
		return localFile;
	}

	public MetaData getMetaData(int i, String path) {
		render.openImage(path);
		MetaData metaData = null;
		try {
			metaData = render.getMetadata(0);
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}
		render.closeImage();
		return metaData;
	}

}
