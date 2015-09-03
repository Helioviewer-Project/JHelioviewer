package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.downloadmanager.AbstractDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.layers.CacheableImageData;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.opengl.TextureCache;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UltimateLayer
{
	public String observatory;
	public String instrument;
	public String measurement1;
	public String measurement2;
	public int sourceID;
	
	public static final int MAX_FRAME_SIZE = 10;
	private static final String URL = "http://api.helioviewer.org/v2/getJPX/?";

	private ConcurrentSkipListSet<LocalDateTime> localDateTimes = new ConcurrentSkipListSet<LocalDateTime>();

	private ImageRegion imageRegion;
	private Thread jpipLoader;
	private boolean localFile = false;
	private ImageLayer imageLayer;
	private int id;
	
	private CacheableImageData cacheableImageData;

	private ArrayList<AbstractDownloadRequest> requests = new ArrayList<AbstractDownloadRequest>();
	private ArrayList<AbstractDownloadRequest> failedRequests = new ArrayList<AbstractDownloadRequest>();

	ThreadLocal<KakaduRender> kakaduRenders = new ThreadLocal<KakaduRender>()
			{
				@Override
				protected KakaduRender initialValue()
				{
					return new KakaduRender();
				};
			};

	public UltimateLayer(int id, int sourceID, ImageLayer newLayer)
	{
		this.id = id;
		this.imageLayer = newLayer;
		this.sourceID = sourceID;
	}

	public UltimateLayer(int id, String filename, KakaduRender render, ImageLayer newLayer)
	{
		this.id = id;
		this.sourceID = 0;
		this.imageLayer = newLayer;
		this.localFile = true;
		cacheableImageData = new CacheableImageData(id, filename);
		Cache.addCacheElement(cacheableImageData);
		try
		{
			render.openImage(cacheableImageData.getSource());
			int framecount = cacheableImageData.getFrameCount();
			LocalDateTime[] localDateTimes = new LocalDateTime[framecount];
			MetaData[] metaDatas = new MetaData[framecount];
			for (int i = 1; i <= framecount; i++)
			{
				metaDatas[i-1] = render.getMetadata(i, cacheableImageData.getFamilySrc());
				this.localDateTimes.add(metaDatas[i-1].getLocalDateTime());
				localDateTimes[i - 1] = metaDatas[i-1].getLocalDateTime();
			}
			cacheableImageData.setMetadatas(metaDatas);
			cacheableImageData.setTimeRange(localDateTimes);
			render.closeImage();
		}
		catch (KduException e)
		{
			
			e.printStackTrace();
		}
		catch (JHV_KduException e)
		{
			
			e.printStackTrace();
		}
		this.timeArrayChanged();
	}
	
	public void setTimeRange(final LocalDateTime start, final LocalDateTime end, final int cadence)
	{
		jpipLoader = new Thread(new Runnable()
		{
			private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			private ArrayList<HTTPRequest> httpRequests = new ArrayList<HTTPRequest>();
			private HashMap<HTTPRequest, JPIPDownloadRequest> jpipDownloadRequests = new HashMap<HTTPRequest, JPIPDownloadRequest>();
			private ArrayList<JPIPDownloadRequest> downloadRequests = new ArrayList<JPIPDownloadRequest>();

			@Override
			public void run()
			{
				LocalDateTime tmp = LocalDateTime.MIN;
				LocalDateTime currentStart = start;
				while (tmp.isBefore(end))
				{
					tmp = currentStart.plusSeconds(cadence * (MAX_FRAME_SIZE - 1));
					String request = "startTime="
							+ currentStart.format(formatter) + "&endTime="
							+ tmp.format(formatter) + "&sourceId=" + sourceID
							+ "&jpip=true&verbose=true&cadence=" + cadence;
					HTTPRequest httpRequest = new HTTPRequest(URL + request,
							DownloadPriority.HIGH);
					requests.add(httpRequest);
					UltimateDownloadManager.addRequest(httpRequest);
					httpRequests.add(httpRequest);

					request = "startTime=" + currentStart.format(formatter)
							+ "&endTime=" + tmp.format(formatter)
							+ "&sourceId=" + sourceID + "&cadence=" + cadence;

					CacheableImageData cacheableImageData = new CacheableImageData(
							id);
					JPIPDownloadRequest jpipDownloadRequest = new JPIPDownloadRequest(
							URL + request, DownloadPriority.LOW, cacheableImageData,
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
									failedRequests.add(httpRequest);
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
										jpipURI, DownloadPriority.URGENT, 0, frames
												.length(), new Rectangle(256,
												256), cacheableImageData);

								requests.add(jpipRequestLow);

								UltimateDownloadManager
										.addRequest(jpipRequestLow);

							} catch (JSONException e1) {
								
								e1.printStackTrace();
							} catch (IOException e) {
								failedRequests.add(httpRequest);
							}
							requests.remove(httpRequest);
						}
					}
				}
				httpRequests.clear();

				finished = false;
				while (!finished) {
					finished = true;
					//FIXME: concurrent modification exception
					for (AbstractDownloadRequest request : requests) {
						if (request != null) {
							finished &= request.isFinished();
						}
					}
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						
						e.printStackTrace();
					}
				}

				finished = false;
				while (!finished) {
					AbstractDownloadRequest[] requests = new AbstractDownloadRequest[UltimateLayer.this.requests.size()];
					UltimateLayer.this.requests.toArray(requests);
					for (AbstractDownloadRequest request : requests)
					{
						if (Thread.interrupted())
							return;
						if (request.isFinished())
						{
							try
							{
								request.checkException();
							}
							catch (IOException e)
							{
								failedRequests.add(request);
							}
							UltimateLayer.this.requests.remove(request);
						}
					}
					finished = UltimateLayer.this.requests.isEmpty();
				}
				downloadRequests.clear();
				imageLayer.addFailedRequests(failedRequests);
			}
		}, "JPIP_URI_LOADER");

		jpipLoader.setDaemon(true);
		jpipLoader.start();
	}

	private void addFramedates(LocalDateTime[] localDateTimes)
	{
		for (LocalDateTime localDateTime : localDateTimes)
			this.localDateTimes.add(localDateTime);
		
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
	public ConcurrentSkipListSet<LocalDateTime> getLocalDateTimes()
	{
		return this.localDateTimes;
	}

	public ImageRegion getImageRegion()
	{
		return this.imageRegion;
	}

	@Deprecated
	private void timeArrayChanged()
	{
		TimeLine.SINGLETON.updateLocalDateTimes(this.localDateTimes);
	}

	public void cancelAllDownloadsForThisLayer()
	{
		if (jpipLoader != null && jpipLoader.isAlive())
			jpipLoader.interrupt();
		
		for (AbstractDownloadRequest request : requests)
			UltimateDownloadManager.remove(request);
		
		requests.clear();
	}

	public String getURL()
	{
		if (localFile)
			return null;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		LocalDateTime start = this.localDateTimes.first();
		LocalDateTime end = this.localDateTimes.last();
		int cadence = 0;
		LocalDateTime last = null;
		for (LocalDateTime localDateTime : this.localDateTimes)
		{
			if (last != null)
			{
				cadence += ChronoUnit.SECONDS.between(last, localDateTime);
			}
			last = localDateTime;
		}
		cadence /= (this.localDateTimes.size() - 1);
		String request = "startTime=" + start.format(formatter) + "&endTime=" + end.format(formatter) + "&sourceId=" + sourceID + "&cadence=" + cadence;

		return URL + request;
	}

	public boolean isLocalFile()
	{
		return localFile;
	}

	public MetaData getMetaData(int i, String path)
	{
		KakaduRender kakaduRender = new KakaduRender();
		MetaData metaData = null;
		kakaduRender.openImage(cacheableImageData.getSource());
		try
		{
			metaData = kakaduRender.getMetadata(i, cacheableImageData.getFamilySrc());
		}
		catch (JHV_KduException e)
		{
			e.printStackTrace();
		}
		return metaData;
	}

	public void retryFailedRequests(final AbstractDownloadRequest[] requests)
	{
		Thread thread = new Thread(new Runnable()
		{
			private ArrayList<HTTPRequest> httpRequests = new ArrayList<HTTPRequest>();
			private HashMap<HTTPRequest, JPIPDownloadRequest> jpipDownloadRequests = new HashMap<HTTPRequest, JPIPDownloadRequest>();
			private ArrayList<JPIPDownloadRequest> downloadRequests = new ArrayList<JPIPDownloadRequest>();

			@Override
			public void run()
			{
				boolean finished = false;

				for (AbstractDownloadRequest request : requests)
				{
					request.setRetries(3);
					
					//FIXME: type code stinks
					if (request instanceof JPIPDownloadRequest)
					{
						downloadRequests.add((JPIPDownloadRequest) request);
						UltimateLayer.this.requests.add(request);
					}
					else if (request instanceof JPIPRequest)
					{
						UltimateLayer.this.requests.add(request);
					}
					else
					{
						httpRequests.add((HTTPRequest) request);
						UltimateLayer.this.requests.add(request);
					}
					
					UltimateDownloadManager.addRequest(request);
				}

				for (HTTPRequest httpRequest : httpRequests)
					for (JPIPDownloadRequest downloadRequest : downloadRequests)
						if (downloadRequest.getEqualJPIPRequest() == httpRequest)
							jpipDownloadRequests.put(httpRequest, downloadRequest);

				while (!finished)
				{
					if (Thread.interrupted())
						return;
					for (HTTPRequest httpRequest : httpRequests)
					{
						finished = true;
						finished &= httpRequest.isFinished();
						JPIPDownloadRequest jpipDownloadRequest = jpipDownloadRequests.get(httpRequest);
						if (httpRequest.isFinished() && UltimateLayer.this.requests.contains(httpRequest))
						{
							JSONObject jsonObject;
							try
							{
								jsonObject = new JSONObject(httpRequest.getDataAsString());

								if (jsonObject.has("error"))
								{
									System.out.println("error during : " + httpRequest);
									UltimateLayer.this.requests.remove(httpRequest);
									break;
								}

								JSONArray frames = ((JSONArray) jsonObject.get("frames"));
								LocalDateTime[] localDateTimes = new LocalDateTime[frames.length()];
								for (int i = 0; i < frames.length(); i++)
								{
									Timestamp timestamp = new Timestamp(frames.getLong(i) * 1000L);
									localDateTimes[i] = timestamp.toLocalDateTime();
								}

								String jpipURI = jsonObject.getString("uri");
								
								CacheableImageData cacheableImageData = jpipDownloadRequest.getCachaableImageData();
								cacheableImageData.setLocalDateTimes(localDateTimes);
								jpipDownloadRequests.remove(httpRequest);
								Cache.addCacheElement(cacheableImageData);
								addFramedates(localDateTimes);

								JPIPRequest lowResolutionPreview = new JPIPRequest(jpipURI, DownloadPriority.HIGH, 0, frames.length(), new Rectangle(128, 128), cacheableImageData);

								UltimateLayer.this.requests.add(lowResolutionPreview);
								UltimateDownloadManager.addRequest(lowResolutionPreview);
							}
							catch (JSONException e1)
							{
								e1.printStackTrace();
							}
							catch (IOException e)
							{
								failedRequests.add(httpRequest);
							}
							
							UltimateLayer.this.requests.remove(httpRequest);
						}
					}
				}
				httpRequests.clear();

				finished = false;
				while (!finished)
				{
					finished = true;
					for (AbstractDownloadRequest request : requests)
						finished &= request.isFinished();

					try
					{
						Thread.sleep(20);
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}

				finished = false;
				while (!finished)
				{
					AbstractDownloadRequest[] requests = new AbstractDownloadRequest[UltimateLayer.this.requests.size()];
					UltimateLayer.this.requests.toArray(requests);
					for (AbstractDownloadRequest request : requests)
					{
						if (Thread.interrupted())
							return;
						if (request.isFinished())
						{
							try
							{
								request.checkException();
							}
							catch (IOException e)
							{
								failedRequests.add(request);
							}
							
							UltimateLayer.this.requests.remove(request);
						}
					}
					finished = UltimateLayer.this.requests.isEmpty();
				}
			}
		}, "RETRY-DOWNLAOD-REQUESTS");
		thread.setDaemon(true);
		thread.start();
	}
	
	public ByteBuffer getImageData(LocalDateTime currentDateTime, ImageRegion imageRegion, MetaData metaData)
	{
		LocalDateTime localDateTime = this.getNextLocalDateTime(currentDateTime);
		if (imageRegion.getImageSize().getWidth() < 0 || imageRegion.getImageSize().getHeight() < 0)
			return null;
		
		if (localDateTime == null)
			localDateTime = this.localDateTimes.last();

		ImageRegion cachedRegion = TextureCache.search(id, imageRegion, localDateTime);
		if(cachedRegion != null)
		{
			this.imageRegion = cachedRegion;
			return null;
		}
		
		CacheableImageData cacheObject = null;

		cacheObject = Cache.getCacheElement(id, localDateTime);

		this.imageRegion = imageRegion;
		this.imageRegion.setLocalDateTime(localDateTime);
		this.imageRegion = TextureCache.addElement(imageRegion, id);
		this.imageRegion.setMetaData(metaData);
		this.imageRegion.setID(id);

		ByteBuffer imageData = null;
		KakaduRender kakaduRender = kakaduRenders.get();
		kakaduRender.openImage(cacheObject.getSource());
		imageData = kakaduRender.getImage(
				cacheObject.getIdx(localDateTime), 8,
				this.imageRegion.getZoomFactor(),
				this.imageRegion.getImageSize());
		kakaduRender.closeImage();

		return imageData;
	}
	
	public MetaData getMetaData(LocalDateTime currentDateTime)
	{
		if (this.localDateTimes.isEmpty())
			return null;
		
		LocalDateTime localDateTime = this.getNextLocalDateTime(currentDateTime);
		if (localDateTime == null)
			localDateTime = this.localDateTimes.last();

		CacheableImageData cacheObject = Cache.getCacheElement(id, localDateTime);
		System.out.println(cacheObject);
		if (cacheObject == null)
			return null;
		
		return cacheObject.getMetaData(cacheObject.getIdx(localDateTime));
	}
}
