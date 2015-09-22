package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.TreeSet;

import javax.swing.SwingUtilities;

import kdu_jni.KduException;

import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.downloadmanager.AbstractDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.opengl.TextureCache;
import org.helioviewer.jhv.viewmodel.TimeLine;
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
	public int sourceId;
	
	public static final int MAX_FRAME_DOWNLOAD_BATCH = 10;
	private static final String URL = "http://api.helioviewer.org/v2/getJPX/?";
	
	public TreeSet<LocalDateTime> localDateTimes = new TreeSet<LocalDateTime>();

	//FIXME: should be passed as parameters, not as state
	public ImageRegion imageRegion;
	
	private volatile Thread loaderThread;
	private boolean localFile = false;
	private int layerId;
	
	private Movie movie;

	ThreadLocal<KakaduRender> kakaduRenders = new ThreadLocal<KakaduRender>()
			{
				@Override
				protected KakaduRender initialValue()
				{
					return new KakaduRender();
				};
			};

	public UltimateLayer(int _layerId, int _sourceId)
	{
		this.layerId = _layerId;
		this.sourceId = _sourceId;
	}

	public UltimateLayer(int _layerId, String _filename)
	{
		this.layerId = _layerId;
		this.sourceId = 0;
		this.localFile = true;
		movie = new Movie(_layerId);
		movie.setFile(_filename);
		MovieCache.add(movie);
		
		TimeLine.SINGLETON.setLocalDateTimes(this.localDateTimes);
	}
	
	
	class MovieDownload
	{
		HTTPRequest metadata;
		JPIPRequest lq;
		JPIPDownloadRequest hq;
		Movie movie;
	}
	
	public void setTimeRange(final LocalDateTime start, final LocalDateTime end, final int cadence)
	{
		loaderThread = new Thread(new Runnable()
		{
			private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			private LinkedList<MovieDownload> downloads = new LinkedList<MovieDownload>();
			private boolean incomplete=false;
			
			@Override
			public void run()
			{
				LocalDateTime currentStart = start;
				while (currentStart.isBefore(end))
				{
					LocalDateTime currentEnd = currentStart.plusSeconds(cadence * (MAX_FRAME_DOWNLOAD_BATCH - 1));
					if(currentEnd.isAfter(end))
						currentEnd = end;
					
					MovieDownload md=new MovieDownload();
					md.movie = new Movie(sourceId);
					md.metadata = new HTTPRequest(URL
							+ "startTime=" + currentStart.format(formatter)
							+ "&endTime=" + currentEnd.format(formatter)
							+ "&sourceId=" + sourceId
							+ "&cadence=" + cadence
							+ "&jpip=true&verbose=true",
							DownloadPriority.HIGH);
					
					md.hq = new JPIPDownloadRequest(URL
							+ "startTime=" + currentStart.format(formatter)
							+ "&endTime=" + currentEnd.format(formatter)
							+ "&sourceId=" + sourceId
							+ "&cadence=" + cadence,
							DownloadPriority.LOW);
					
					UltimateDownloadManager.addRequest(md.metadata);
					//UltimateDownloadManager.addRequest(md.hq);
					
					downloads.add(md);
					
					currentStart = currentStart.plusSeconds(cadence * MAX_FRAME_DOWNLOAD_BATCH);
				}
				
				while(!downloads.isEmpty())
				{
					try
					{
						if (Thread.interrupted())
						{
							incomplete=true;
							break;
						}
						
						final MovieDownload download = downloads.removeFirst();
						if(download.hq!=null && download.hq.isFinished())
						{
							UltimateDownloadManager.remove(download.metadata);
							UltimateDownloadManager.remove(download.lq);
							
							download.movie.setFile(download.hq.getFilename());
							
							download.hq = null;
							download.lq = null;
							
							SwingUtilities.invokeLater(new Runnable()
							{
								@Override
								public void run()
								{
									MovieCache.add(download.movie);
								}
							});
						}
						else if(download.lq!=null && download.lq.isFinished())
						{
							download.lq=null;
							downloads.addLast(download);
							SwingUtilities.invokeLater(new Runnable()
							{
								@Override
								public void run()
								{
									MovieCache.add(download.movie);
								}
							});
						}
						else if(download.metadata!=null && download.metadata.isFinished())
						{
							try
							{
								JSONObject jsonObject = new JSONObject(download.metadata.getDataAsString());
								if (jsonObject.has("error"))
								{
									System.err.println("JSON error: "+download.metadata.getDataAsString());
									
									UltimateDownloadManager.remove(download.metadata);
									UltimateDownloadManager.remove(download.lq);
									UltimateDownloadManager.remove(download.hq);
									
									incomplete=true;
									download.metadata=null;
									continue;
								}
								
								downloads.addLast(download);
								
								JSONArray frames = ((JSONArray) jsonObject.get("frames"));
								final LocalDateTime[] localDateTimes = new LocalDateTime[frames.length()];
								for (int i = 0; i < frames.length(); i++)
									localDateTimes[i] = new Timestamp(frames.getLong(i) * 1000L).toLocalDateTime();
								
								download.movie.setLocalDateTimes(localDateTimes);
								
								SwingUtilities.invokeLater(new Runnable()
								{
									@Override
									public void run()
									{
										addFrameDateTimes(localDateTimes);
									}
								});
								
								download.lq = new JPIPRequest(jsonObject.getString("uri"), DownloadPriority.URGENT, 0, frames.length(), new Rectangle(128, 128), download.movie);
								UltimateDownloadManager.addRequest(download.lq);
								download.metadata=null;
							}
							catch(JSONException _e)
							{
								UltimateDownloadManager.remove(download.metadata);
								UltimateDownloadManager.remove(download.lq);
								UltimateDownloadManager.remove(download.hq);

								_e.printStackTrace();
								
								incomplete=true;
								download.lq=null;
								download.hq=null;
								download.metadata=null;
								continue;
							}
						}
						else
						{
							SwingUtilities.invokeLater(new Runnable()
							{
								@Override
								public void run()
								{
									//FIXME: should probably only be set if current layer is active
									MainFrame.MOVIE_PANEL.repaintSlider();
								}
							});
							Thread.sleep(200);
							downloads.addLast(download);
						}
					}
					catch(IOException _e)
					{
						incomplete=true;
						_e.printStackTrace();
					}
					catch (InterruptedException _e)
					{
						incomplete=true;
						_e.printStackTrace();
						break;
					}
				}
				
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						//FIXME: should probably only be set if current layer is active
						MainFrame.MOVIE_PANEL.repaintSlider();
					}
				});
				
				for(MovieDownload md:downloads)
				{
					if(md.hq!=null)
						UltimateDownloadManager.remove(md.hq);
					if(md.lq!=null)
						UltimateDownloadManager.remove(md.lq);
					if(md.metadata!=null)
						UltimateDownloadManager.remove(md.metadata);
				}
			}
		}, "Layer loader "+observatory+"-"+instrument+"-"+measurement1+"-"+measurement2);

		loaderThread.setDaemon(true);
		loaderThread.start();
	}

	private void addFrameDateTimes(LocalDateTime[] _localDateTimes)
	{
		for (LocalDateTime localDateTime : _localDateTimes)
			localDateTimes.add(localDateTime);
		
		//FIXME: should probably only be set if current layer is active
		TimeLine.SINGLETON.setLocalDateTimes(localDateTimes);
		MainFrame.MOVIE_PANEL.repaintSlider();
	}


	public LocalDateTime getClosestLocalDateTime(LocalDateTime currentDateTime)
	{
		LocalDateTime after = this.localDateTimes.ceiling(currentDateTime);
		LocalDateTime before = this.localDateTimes.floor(currentDateTime);
		long beforeValue = before != null ? ChronoUnit.SECONDS.between(before, currentDateTime) : Long.MAX_VALUE;
		long afterValue = after != null ? ChronoUnit.SECONDS.between(currentDateTime, after) : Long.MAX_VALUE;
		return beforeValue > afterValue ? after : before;
	}


	@Deprecated
	public NavigableSet<LocalDateTime> getLocalDateTimes()
	{
		return this.localDateTimes;
	}

	public ImageRegion getImageRegion()
	{
		return this.imageRegion;
	}

	public void cancelAllDownloadsForThisLayer()
	{
		if (loaderThread != null)
			loaderThread.interrupt();
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
				cadence += ChronoUnit.SECONDS.between(last, localDateTime);
			
			last = localDateTime;
		}
		cadence /= (this.localDateTimes.size() - 1);

		return URL + "startTime=" + start.format(formatter) + "&endTime=" + end.format(formatter) + "&sourceId=" + sourceId + "&cadence=" + cadence;
	}

	public boolean isLocalFile()
	{
		return localFile;
	}

	public MetaData getMetaData(int i, String path)
	{
		KakaduRender kakaduRender = new KakaduRender();
		kakaduRender.openImage(movie.getSource());
		try
		{
			return kakaduRender.getMetadata(i, movie.getFamilySrc());
		}
		catch (KduException e)
		{
			e.printStackTrace();
		}
		finally
		{
			kakaduRender.closeImage();
		}
		return null;
	}

	public void retryFailedRequests(final AbstractDownloadRequest[] requests)
	{
		//FIXME
		throw new RuntimeException("TODO");
	}
	
	public ByteBuffer getImageData(LocalDateTime localDateTime, ImageRegion imageRegion, MetaData metaData)
	{
		Movie cacheObject = MovieCache.get(sourceId, localDateTime);

		this.imageRegion = imageRegion;
		this.imageRegion.setLocalDateTime(localDateTime);
		this.imageRegion = TextureCache.add(imageRegion, layerId);
		this.imageRegion.setMetaData(metaData);
		this.imageRegion.setID(layerId);

		ByteBuffer imageData = null;
		KakaduRender kakaduRender = kakaduRenders.get();
		kakaduRender.openImage(cacheObject.getSource());
		imageData = kakaduRender.getImage(cacheObject.getIdx(localDateTime), 8, this.imageRegion.getZoomFactor(), this.imageRegion.getImageSize());
		kakaduRender.closeImage();
		
		return imageData;
	}
	
	public MetaData getMetaData(LocalDateTime currentDateTime)
	{
		if (this.localDateTimes.isEmpty())
			return null;
		
		LocalDateTime localDateTime = this.getClosestLocalDateTime(currentDateTime);
		if (localDateTime == null)
			localDateTime = this.localDateTimes.last();

		Movie cacheObject = MovieCache.get(sourceId, localDateTime);
		if (cacheObject == null)
			return null;
		
		return cacheObject.getMetaData(cacheObject.getIdx(localDateTime));
	}
}
