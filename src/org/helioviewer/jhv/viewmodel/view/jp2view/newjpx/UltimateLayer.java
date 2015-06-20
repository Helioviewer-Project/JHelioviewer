package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.IOException;
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

import javax.swing.JOptionPane;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
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
	private static final int MAX_THREAD_PER_LAYER = 2;
	private static final int MAX_THREAD_PER_LOAD_JPIP_URLS = 4;
	
	private KakaduRender render;

	private String fileName = null;
	private LocalDateTime[] localDateTimes;
	private TreeSet<LocalDateTime> treeSet;
	private ImageLayer newLayer;

	private ImageRegion imageRegion;
	private Thread jpipURLLoader;
	
	private int id;

	private ArrayList<AbstractRequest> requests = new ArrayList<AbstractRequest>();

	public UltimateLayer(int id, int sourceID,
			KakaduRender render, ImageLayer newLayer) {
		treeSet = new TreeSet<LocalDateTime>();
		this.id = id;
		this.newLayer = newLayer;
		this.sourceID = sourceID;
		this.render = render;
	}

	public UltimateLayer(int id, String filename, KakaduRender render,
			ImageLayer newLayer) {
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

		ExecutorService executorService = Executors
				.newFixedThreadPool(MAX_THREAD_PER_LOAD_JPIP_URLS);


		LocalDateTime tmp = LocalDateTime.MIN;
		LocalDateTime currentStart = start;

		jpipURLLoader = new Thread(new Runnable() {
			private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			private ArrayList<HTTPRequest> httpRequests = new ArrayList<HTTPRequest>();

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
							PRIORITY.HIGH);
					requests.add(httpRequest);
					UltimateDownloadManager.addRequest(httpRequest);
					httpRequests.add(httpRequest);
					currentStart = tmp;
				}
				if (Thread.interrupted()) return;
				
				for (HTTPRequest httpRequest : httpRequests) {
					while (!httpRequest.isFinished()) {
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}


					JSONObject jsonObject;
					try {
						jsonObject = new JSONObject(httpRequest
								.getDataAsString());

						if (jsonObject.has("error")) {
							return;
						}
						
						JSONArray frames = ((JSONArray) jsonObject
								.get("frames"));
						LocalDateTime[] localDateTimes = new LocalDateTime[frames.length()];
						for (int i = 0; i < frames.length(); i++) {
							Timestamp timestamp = new Timestamp(frames
									.getLong(i) * 1000L);
							localDateTimes[i] = timestamp.toLocalDateTime();
						}

						String jpipURI = jsonObject.getString("uri");
						System.out.println("jpipURL : " + jpipURI);
						Kdu_cache kduCache = new Kdu_cache();
						
						CacheableImageData cacheableImageData = new CacheableImageData(
								id, localDateTimes, kduCache);
						Cache.addCacheElement(cacheableImageData);
						addFramedates(localDateTimes);

						
						JPIPRequest jpipRequestLow = new JPIPRequest(jpipURI, PRIORITY.HIGH, 0, frames.length(), new Rectangle(256, 256), kduCache, cacheableImageData);
						JPIPRequest jpipRequestMiddle = new JPIPRequest(jpipURI, PRIORITY.MEDIUM, 0, frames.length(), new Rectangle(2048, 2048), kduCache, cacheableImageData);
						JPIPRequest jpipRequestHigh = new JPIPRequest(jpipURI, PRIORITY.LOW, 0, frames.length(), new Rectangle(4096, 4096), kduCache, cacheableImageData);
						requests.add(jpipRequestLow);
						requests.add(jpipRequestMiddle);
						requests.add(jpipRequestHigh);
						UltimateDownloadManager.addRequest(jpipRequestLow);
						UltimateDownloadManager.addRequest(jpipRequestMiddle);
						UltimateDownloadManager.addRequest(jpipRequestHigh);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException e) {
						JOptionPane.showConfirmDialog(MainFrame.SINGLETON, "No connection available from " + httpRequest, "Error during connection", JOptionPane.OK_OPTION, JOptionPane.ERROR_MESSAGE);
					}
					
				}

			}
		}, "JPIP_URI_LOADER");

		jpipURLLoader.start();
	}

	public int getFrameCount() {
		if (fileName != null)
			return localDateTimes.length;
		return 1;
	}

	public void addFramedates(LocalDateTime[] localDateTimes) {
		for (LocalDateTime localDateTime : localDateTimes) treeSet.add(localDateTime);
		 LocalDateTime[] allLocalDateTimes = treeSet.toArray(new LocalDateTime[treeSet.size()]);
		TimeLine.SINGLETON.setFrames(allLocalDateTimes);
	}

	public MetaData getMetaData(LocalDateTime currentDateTime)
			throws InterruptedException, ExecutionException, JHV_KduException {
		if (fileName != null)
			return this.getMetaData(TimeLine.SINGLETON.getCurrentFrame());

		if (Cache.getCacheElement(this.id, currentDateTime) == null)
			return null;
		
		CacheableImageData cacheObject = Cache.getCacheElement(id,
				currentDateTime);

		render.openImage(cacheObject.getImageData());
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
		Queue<CachableTexture> textures = TextureCache
				.getCacheableTextures();
		for (CachableTexture texture : textures) {
			if (texture.compareRegion(id, imageRegion, currentDateTime) && !texture.hasChanged()) {
				this.imageRegion = texture.getImageRegion();
				TextureCache.setElementAsFist(texture);
				return null;
			}
		}
		if (fileName != null) {
			imageRegion.setLocalDateTime(currentDateTime);
			this.imageRegion = TextureCache.addElement(imageRegion,
					id);
			this.imageRegion.setMetaData(metaData);
			return getImageFromLocalFile(currentDateTime,
					this.imageRegion.getZoomFactor(),
					this.imageRegion.getImageSize());
		}

		CacheableImageData cacheObject = Cache.getCacheElement(id,
				currentDateTime);

		imageRegion.setLocalDateTime(currentDateTime);
		this.imageRegion = TextureCache.addElement(imageRegion, id);
		this.imageRegion.setMetaData(metaData);

		System.out
				.println("---------------------getImage---------------------");
		render.openImage(cacheObject.getImageData());

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
		TimeLine.SINGLETON.updateLocalDateTimes(this.treeSet
				.toArray(localDateTimes));
	}

	public void cancelDownload() {
		if (jpipURLLoader != null && jpipURLLoader.isAlive())
			jpipURLLoader.interrupt();
		for (AbstractRequest request : requests){
			UltimateDownloadManager.remove(request);
		}
	}

}
