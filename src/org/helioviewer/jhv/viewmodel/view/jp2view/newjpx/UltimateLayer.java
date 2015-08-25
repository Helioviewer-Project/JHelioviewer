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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;

import kdu_jni.KduException;

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

	private KakaduRender render;

	private ConcurrentSkipListSet<LocalDateTime> localDateTimes = new ConcurrentSkipListSet<LocalDateTime>();

	private ImageRegion imageRegion;
	private Thread jpipURLLoader;
	private boolean localFile = false;
	private ImageLayer imageLayer;
	private int id;
	
	private CacheableImageData cacheableImageData;

	private ArrayList<AbstractRequest> requests = new ArrayList<AbstractRequest>();

	private ArrayList<AbstractRequest> badRequests = new ArrayList<AbstractRequest>();

	private static final int NUMBER_OF_KAKADU_RENDERERS = 20;
	private static final BlockingQueue<KakaduRender> kakaduRenders = new ArrayBlockingQueue<KakaduRender>(NUMBER_OF_KAKADU_RENDERERS);;

	static{
		for (int i = 0; i < NUMBER_OF_KAKADU_RENDERERS; i++){
			kakaduRenders.offer(new KakaduRender());
		}
	}
	
	public UltimateLayer(int id, int sourceID, KakaduRender render,
			ImageLayer newLayer) {
		this.id = id;
		this.imageLayer = newLayer;
		this.sourceID = sourceID;
		this.render = render;
	}

	public UltimateLayer(int id, String filename, KakaduRender render,
			ImageLayer newLayer) {
		this.id = id;
		this.sourceID = 0;
		this.imageLayer = newLayer;
		this.render = render;
		this.localFile = true;
		cacheableImageData = new CacheableImageData(id, filename);
		Cache.addCacheElement(cacheableImageData);
		try {
			this.render.openImage(cacheableImageData.getSource());
			int framecount = cacheableImageData.getFrameCount();
			LocalDateTime[] localDateTimes = new LocalDateTime[framecount];
			MetaData[] metaDatas = new MetaData[framecount];
			for (int i = 1; i <= framecount; i++) {
				metaDatas[i-1] = render.getMetadata(i, cacheableImageData.getFamilySrc());
				this.localDateTimes.add(metaDatas[i-1].getLocalDateTime());
				localDateTimes[i - 1] = metaDatas[i-1].getLocalDateTime();
			}
			cacheableImageData.setMetadatas(metaDatas);
			cacheableImageData.setTimeRange(localDateTimes);
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
							id);
					JPIPDownloadRequest jpipDownloadRequest = new JPIPDownloadRequest(
							URL + request, PRIORITY.LOW, cacheableImageData,
							requests, httpRequest);
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
						finished = true;
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
									requests.remove(httpRequest);
									badRequests.add(httpRequest);
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
					finished = true;
					for (AbstractRequest request : requests) {
						if (request != null) {
							finished &= request.isFinished();
						}
					}
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				finished = false;
				while (!finished) {
					AbstractRequest[] requests = new AbstractRequest[UltimateLayer.this.requests
							.size()];
					UltimateLayer.this.requests.toArray(requests);
					for (AbstractRequest request : requests) {
						if (Thread.interrupted())
							return;
						if (request.isFinished()) {
							try {
								request.checkException();
							} catch (IOException e) {
								badRequests.add(request);
							}
							UltimateLayer.this.requests.remove(request);
						}
					}
					finished = UltimateLayer.this.requests.isEmpty();
				}
				downloadRequests.clear();
				imageLayer.addBadRequests(badRequests);
			}
		}, "JPIP_URI_LOADER");

		jpipURLLoader.start();
	}

	private void addFramedates(LocalDateTime[] localDateTimes) {
		for (LocalDateTime localDateTime : localDateTimes) {
			this.localDateTimes.add(localDateTime);
		}
		TimeLine.SINGLETON.updateLocalDateTimes(this.localDateTimes);
	}


	private LocalDateTime getNextLocalDateTime(LocalDateTime currentDateTime) {
		LocalDateTime after = this.localDateTimes.ceiling(currentDateTime);
		LocalDateTime before = this.localDateTimes.floor(currentDateTime);
		long beforeValue = before != null ? ChronoUnit.SECONDS.between(before,
				currentDateTime) : Long.MAX_VALUE;
		long afterValue = after != null ? ChronoUnit.SECONDS.between(
				currentDateTime, after) : Long.MAX_VALUE;
		return beforeValue > afterValue ? after : before;
	}


	@Deprecated
	public ConcurrentSkipListSet<LocalDateTime> getLocalDateTimes() {
		return this.localDateTimes;
	}

	public ImageRegion getImageRegion() {
		return this.imageRegion;
	}

	@Deprecated
	private void timeArrayChanged() {
		TimeLine.SINGLETON.updateLocalDateTimes(this.localDateTimes);
	}

	public void cancelDownload() {
		if (jpipURLLoader != null && jpipURLLoader.isAlive())
			jpipURLLoader.interrupt();
		for (AbstractRequest request : requests) {
			UltimateDownloadManager.remove(request);
		}
		requests.clear();
	}

	public String getURL() {
		if (localFile)
			return null;
		DateTimeFormatter formatter = DateTimeFormatter
				.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		LocalDateTime start = this.localDateTimes.first();
		LocalDateTime end = this.localDateTimes.last();
		int cadence = 0;
		LocalDateTime last = null;
		for (LocalDateTime localDateTime : this.localDateTimes) {
			if (last != null) {
				cadence += ChronoUnit.SECONDS.between(last, localDateTime);
			}
			last = localDateTime;
		}
		cadence /= (this.localDateTimes.size() - 1);
		String request = "startTime=" + start.format(formatter) + "&endTime="
				+ end.format(formatter) + "&sourceId=" + sourceID + "&cadence="
				+ cadence;

		return URL + request;
	}

	public boolean isLocalFile() {
		return localFile;
	}

	public MetaData getMetaData(int i, String path) {
		//render.openImage(path);
		KakaduRender kakaduRender = new KakaduRender();
		MetaData metaData = null;
		kakaduRender.openImage(cacheableImageData.getSource());
		try {
			metaData = kakaduRender.getMetadata(i, cacheableImageData.getFamilySrc());
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metaData;
	}

	public void retryBadRequest(final AbstractRequest[] requests) {
		Thread thread = new Thread(new Runnable() {
			private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			private ArrayList<HTTPRequest> httpRequests = new ArrayList<HTTPRequest>();
			private HashMap<HTTPRequest, JPIPDownloadRequest> jpipDownloadRequests = new HashMap<HTTPRequest, JPIPDownloadRequest>();
			private ArrayList<JPIPDownloadRequest> downloadRequests = new ArrayList<JPIPDownloadRequest>();

			@Override
			public void run() {
				boolean finished = false;

				for (AbstractRequest request : requests) {
					request.setRetries(3);
					;
					if (request instanceof JPIPDownloadRequest) {
						downloadRequests.add((JPIPDownloadRequest) request);
						UltimateLayer.this.requests.add(request);
					} else if (request instanceof JPIPRequest) {
						UltimateLayer.this.requests.add(request);
					} else {
						httpRequests.add((HTTPRequest) request);
						UltimateLayer.this.requests.add(request);
					}
					UltimateDownloadManager.addRequest(request);
				}

				for (HTTPRequest httpRequest : httpRequests) {
					for (JPIPDownloadRequest downloadRequest : downloadRequests) {
						if (downloadRequest.getEqualJPIPRequest() == httpRequest) {
							jpipDownloadRequests.put(httpRequest,
									downloadRequest);
						}
					}
				}

				while (!finished) {
					if (Thread.interrupted())
						return;
					for (HTTPRequest httpRequest : httpRequests) {
						finished = true;
						finished &= httpRequest.isFinished();
						JPIPDownloadRequest jpipDownloadRequest = jpipDownloadRequests
								.get(httpRequest);
						if (httpRequest.isFinished()
								&& UltimateLayer.this.requests
										.contains(httpRequest)) {
							JSONObject jsonObject;
							try {
								jsonObject = new JSONObject(httpRequest
										.getDataAsString());

								if (jsonObject.has("error")) {
									System.out.println("error during : "
											+ httpRequest);
									UltimateLayer.this.requests
											.remove(httpRequest);
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

								UltimateLayer.this.requests.add(jpipRequestLow);

								UltimateDownloadManager
										.addRequest(jpipRequestLow);

							} catch (JSONException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (IOException e) {
								badRequests.add(httpRequest);
							}
							UltimateLayer.this.requests.remove(httpRequest);
						}
					}
				}
				httpRequests.clear();

				finished = false;
				while (!finished) {
					finished = true;
					for (AbstractRequest request : requests) {
						finished &= request.isFinished();
					}
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				finished = false;
				while (!finished) {
					AbstractRequest[] requests = new AbstractRequest[UltimateLayer.this.requests
							.size()];
					UltimateLayer.this.requests.toArray(requests);
					for (AbstractRequest request : requests) {
						if (Thread.interrupted())
							return;
						if (request.isFinished()) {
							try {
								request.checkException();
							} catch (IOException e) {
								badRequests.add(request);
							}
							UltimateLayer.this.requests.remove(request);
						}
					}
					finished = UltimateLayer.this.requests.isEmpty();
				}
			}
		}, "RETRY-DOWNLAOD-REQUESTS");
		thread.start();
	}
	
	public ByteBuffer getImageData(LocalDateTime currentDateTime,
			ImageRegion imageRegion, MetaData metaData, boolean highResolution){
		ByteBuffer imageData = null;
		Queue<CachableTexture> textures = TextureCache.getCacheableTextures();
		
		LocalDateTime localDateTime = this
				.getNextLocalDateTime(currentDateTime);
		if (imageRegion.getImageSize().getWidth() < 0
				|| imageRegion.getImageSize().getHeight() < 0)
			return null;
		if (localDateTime == null)
			localDateTime = this.localDateTimes.last();

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

		this.imageRegion = imageRegion;
		this.imageRegion.setLocalDateTime(localDateTime);
		this.imageRegion = TextureCache.addElement(imageRegion, id);
		this.imageRegion.setMetaData(metaData);
		this.imageRegion.setID(id);

		KakaduRender kakaduRender = null;
		try {
			kakaduRender = kakaduRenders.take();
			kakaduRender.openImage(cacheObject.getSource());
			imageData = kakaduRender.getImage(
					cacheObject.getIdx(localDateTime), 8,
					this.imageRegion.getZoomFactor(),
					this.imageRegion.getImageSize());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (kakaduRender != null){
		kakaduRender.closeImage();
		kakaduRenders.offer(kakaduRender);
		}
		return imageData;
	}
	
	public MetaData getMetaData(LocalDateTime currentDateTime){
		CacheableImageData cacheObject = null;
		if (this.localDateTimes.size() == 0) return null;
		
		LocalDateTime localDateTime = this
				.getNextLocalDateTime(currentDateTime);
		if (localDateTime == null)
			localDateTime = this.localDateTimes.last();

		cacheObject = Cache.getCacheElement(id, localDateTime);
		if (Cache.getCacheElement(this.id, localDateTime) == null){
			return null;			
		}
		
		return cacheObject.getMetaData(cacheObject.getIdx(localDateTime));
	}		
}
