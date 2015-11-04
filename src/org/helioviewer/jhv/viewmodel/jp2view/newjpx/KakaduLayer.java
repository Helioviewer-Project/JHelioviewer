package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.FutureValue;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.opengl.Texture;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

public class KakaduLayer extends ImageLayer
{
	public static final int MAX_FRAME_DOWNLOAD_BATCH = 15;
	private static final String URL = "http://api.helioviewer.org/v2/getJPX/?";
	
	public TreeSet<LocalDateTime> localDateTimes = new TreeSet<LocalDateTime>();

	@Nullable private volatile Thread loaderThread;
	private boolean localFile = false;
	private final int sourceId;
	protected int cadence = -1;
	protected @Nullable String localPath;
	
	public int getCadence()
	{
		return cadence;
	}
	
	private static final ExecutorService exDecoder = Executors.newWorkStealingPool();
	
	public KakaduLayer(int _sourceId, LocalDateTime _start, LocalDateTime _end, int _cadence, String _name)
	{
		sourceId = _sourceId;
		start = _start;
		end = _end;
		cadence = _cadence;
		name = _name;
		
		setTimeRange(start, end, cadence);
	}
	
	public @Nullable Match getMovie(LocalDateTime _currentDateTime)
	{
		return MovieCache.findBestFrame(sourceId, _currentDateTime);
	}
	
	
	@SuppressWarnings("null")
	public KakaduLayer(String _filePath)
	{
		localPath = _filePath;
		
		sourceId = genSourceId(_filePath);
		localFile = true;
		
		Movie movie = new Movie(sourceId,_filePath);
		if(movie.getAnyMetaData()==null)
			throw new UnsuitableMetaDataException();
		
		LocalDateTime[] times=new LocalDateTime[movie.getFrameCount()];
		for(int i=0;i<times.length;i++)
			times[i]=movie.getMetaData(i).localDateTime;
		addFrameDateTimes(times);
		
		start = localDateTimes.first();
		end = localDateTimes.last();
		name = movie.getAnyMetaData().displayName;
		
		cadence = (int) (ChronoUnit.SECONDS.between(start, end) / times.length);
		
		MovieCache.add(movie);
	}
	
	@SuppressWarnings("null")
	public void storeConfiguration(JSONObject jsonLayer)
	{
		try
		{
			jsonLayer.put("localPath", localPath==null ? "":localPath);
			jsonLayer.put("id", sourceId);
			jsonLayer.put("cadence", cadence);
			jsonLayer.put("startDateTime", start);
			jsonLayer.put("endDateTime", end);
			jsonLayer.put("name", name);
			jsonLayer.put("opa.city", opacity);
			jsonLayer.put("sharpen", sharpness);
			jsonLayer.put("gamma", gamma);
			jsonLayer.put("contrast", contrast);
			if(getLUT()!=null)
				jsonLayer.put("lut", getLUT().ordinal());
			else
				jsonLayer.put("lut", -1);
			jsonLayer.put("redChannel", redChannel);
			jsonLayer.put("greenChannel", greenChannel);
			jsonLayer.put("blueChannel", blueChannel);

			jsonLayer.put("visibility", isVisible());
			jsonLayer.put("invertedLut", invertedLut);
			jsonLayer.put("coronaVisiblity", coronaVisible);
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
	}
	
	public void readStateFile(JSONObject jsonLayer)
	{
		try
		{
			opacity = jsonLayer.getDouble("opa.city");
			sharpness = jsonLayer.getDouble("sharpen");
			gamma = jsonLayer.getDouble("gamma");
			contrast = jsonLayer.getDouble("contrast");
			
			if(jsonLayer.getInt("lut")==-1)
				setLUT(null);
			else
				setLUT(Lut.values()[jsonLayer.getInt("lut")]);
			redChannel=jsonLayer.getBoolean("redChannel");
			greenChannel=jsonLayer.getBoolean("greenChannel");
			blueChannel=jsonLayer.getBoolean("blueChannel");
			
			setVisible(jsonLayer.getBoolean("visibility"));
			invertedLut = jsonLayer.getBoolean("invertedLut");
			coronaVisible=jsonLayer.getBoolean("coronaVisiblity");
			MainFrame.SINGLETON.FILTER_PANEL.update();
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
	}

	//TODO: convert into constructor & combine with readStateFile
	public static @Nullable KakaduLayer createFromStateFile(JSONObject jsonLayer)
	{
		try
		{
			if (!jsonLayer.getString("localPath").equals(""))
			{
				return new KakaduLayer(jsonLayer.getString("localPath"));
			}
			else if (jsonLayer.getInt("cadence") >= 0)
			{
				LocalDateTime start = LocalDateTime.parse(jsonLayer.getString("startDateTime"));
				LocalDateTime end = LocalDateTime.parse(jsonLayer.getString("endDateTime"));
				return new KakaduLayer(jsonLayer.getInt("id"), start, end, jsonLayer.getInt("cadence"), jsonLayer.getString("name"));
			}
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
		return null;
	}

	private static HashMap<String,Integer> usedIDs=new HashMap<>();
	private static int unusedID=-1;
	
	//TODO: are negative sourceId's allowed?
	private int genSourceId(String _filename)
	{
		if(!usedIDs.containsKey(_filename))
			usedIDs.put(_filename, unusedID--);
		
		return usedIDs.get(_filename);
	}
	
	public @Nullable String getLocalFilePath()
	{
		return localPath;
	}
	
	static class MovieDownload
	{
		@Nullable HTTPRequest metadata;
		@Nullable JPIPRequest lq;
		@Nullable JPIPDownloadRequest hq;
		@Nullable Movie lqMovie;
		
		MovieDownload()
		{
		}
	}
	
	@SuppressWarnings("null")
	public void setTimeRange(final LocalDateTime start, final LocalDateTime end, final int cadence)
	{
		if(loaderThread!=null)
			throw new RuntimeException("Changing time range after creation currently not implemented.");
		
		loaderThread = new Thread(new Runnable()
		{
			private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			private LinkedList<MovieDownload> downloads = new LinkedList<MovieDownload>();
			private boolean incomplete=false; //FIXME: should be respected by layer manager + retry logic
			
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
							MovieCache.generateFilename(sourceId,currentStart,currentEnd,cadence),
							DownloadPriority.LOW);
					
					UltimateDownloadManager.addRequest(md.metadata);
					UltimateDownloadManager.addRequest(md.hq);
					
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
							
							try
							{
								final Movie m=new Movie(sourceId,download.hq.filename);
								SwingUtilities.invokeAndWait(new Runnable()
								{
									@Override
									public void run()
									{
										if(download.lqMovie!=null)
											MovieCache.remove(download.lqMovie);
										MovieCache.add(m);
										
										if(lut==null)
										{
											MetaData md=m.getAnyMetaData();
											if(md!=null)
												setLUT(md.getDefaultLUT());
										}
									}
								});
							}
							catch(UnsuitableMetaDataException _umde)
							{
								Telemetry.trackException(_umde);
							}
							
							download.hq = null;
							download.lq = null;
						}
						else if(download.lq!=null && download.lq.isFinished())
						{
							try
							{
								download.lqMovie=new Movie(sourceId,download.lq.kduCache);
								SwingUtilities.invokeAndWait(new Runnable()
								{
									@Override
									public void run()
									{
										MovieCache.add(download.lqMovie);
										
										if(lut==null)
										{
											MetaData md=download.lqMovie.getAnyMetaData();
											if(md!=null)
												setLUT(md.getDefaultLUT());
										}
									}
								});
							}
							catch(UnsuitableMetaDataException _umde)
							{
								Telemetry.trackException(_umde);
							}
							
							download.lq=null;
							downloads.addLast(download);
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
								
								SwingUtilities.invokeAndWait(new Runnable()
								{
									@Override
									public void run()
									{
										addFrameDateTimes(localDateTimes);
									}
								});
								
								download.lq = new JPIPRequest(jsonObject.getString("uri"), DownloadPriority.URGENT, 0, frames.length(), new Rectangle(256, 256));
								
								UltimateDownloadManager.addRequest(download.lq);
								download.metadata=null;
							}
							catch(JSONException _e)
							{
								UltimateDownloadManager.remove(download.metadata);
								UltimateDownloadManager.remove(download.lq);
								UltimateDownloadManager.remove(download.hq);

								Telemetry.trackException(_e);
								
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
									//TODO: should only happen if needed (layer visible, time range, ...)
									MainFrame.SINGLETON.MAIN_PANEL.repaint(1000);
									MainFrame.SINGLETON.OVERVIEW_PANEL.repaint(1000);
									MainFrame.SINGLETON.MOVIE_PANEL.repaint(1000);
								}
							});
							Thread.sleep(500);
							downloads.addLast(download);
						}
					}
					catch (InterruptedException _e)
					{
						incomplete=true;
						break;
					}
					catch(Throwable _e)
					{
						incomplete=true;
						Telemetry.trackException(_e);
					}
				}
				
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						MainFrame.SINGLETON.MAIN_PANEL.repaint(1000);
						MainFrame.SINGLETON.OVERVIEW_PANEL.repaint(1000);
						MainFrame.SINGLETON.MOVIE_PANEL.repaint(1000);
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
		}, "Layer loader "+sourceId);

		loaderThread.setDaemon(true);
		loaderThread.start();
	}
	
	@Override
	public @Nullable LocalDateTime getCurrentTime()
	{
		return findClosestLocalDateTime(TimeLine.SINGLETON.getCurrentDateTime());
	}

	private void addFrameDateTimes(LocalDateTime[] _localDateTimes)
	{
		if(_localDateTimes.length==0)
			return;
		
		for (LocalDateTime localDateTime : _localDateTimes)
			localDateTimes.add(localDateTime);
		
		//TODO: should probably only be set if current layer is active
		TimeLine.SINGLETON.setLocalDateTimes(localDateTimes);
		MainFrame.SINGLETON.MOVIE_PANEL.repaintSlider();
	}


	public @Nullable LocalDateTime findClosestLocalDateTime(LocalDateTime _currentDateTime)
	{
		LocalDateTime after = localDateTimes.ceiling(_currentDateTime);
		LocalDateTime before = localDateTimes.floor(_currentDateTime);
		long beforeValue = before != null ? ChronoUnit.NANOS.between(before, _currentDateTime) : Long.MAX_VALUE;
		long afterValue = after != null ? ChronoUnit.NANOS.between(_currentDateTime, after) : Long.MAX_VALUE;
		return beforeValue > afterValue ? after : before;
	}


	@Deprecated
	public NavigableSet<LocalDateTime> getLocalDateTimes()
	{
		return localDateTimes;
	}

	@Override
	public void dispose()
	{
		if (loaderThread != null)
			loaderThread.interrupt();
	}

	public @Nullable String getDownloadURL()
	{
		if (localFile)
			return null;
		
		if(ChronoUnit.SECONDS.between(start, end) / cadence >= 1000)
			return null;
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		return URL + "startTime=" + start.format(formatter) + "&endTime=" + end.format(formatter) + "&sourceId=" + sourceId + "&cadence=" + cadence;
	}

	public boolean isLocalFile()
	{
		return localFile;
	}

	@Nullable
	public MetaData getMetaData(LocalDateTime currentDateTime)
	{
		if (localDateTimes.isEmpty())
			return null;
		
		return MovieCache.getMetaData(sourceId, currentDateTime);
	}
	
	public @Nullable Document getMetaDataDocument(LocalDateTime currentDateTime)
	{
		if (localDateTimes.isEmpty())
			return null;
		
		return MovieCache.getMetaDataDocument(sourceId, currentDateTime);
	}
	
	public Future<PreparedImage> prepareImageData(final MainPanel _panel, final Dimension _size)
	{
		final MetaData metaData = getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
		if (metaData == null)
			return new FutureValue<PreparedImage>(null);
		
		//TODO: push instead of pull
		if(lut==null)
			lut=metaData.getDefaultLUT();
		
		final ImageRegion requiredMinimumRegion = calculateRegion(_panel, metaData, _size);
		if (requiredMinimumRegion == null)
			return new FutureValue<PreparedImage>(null);
		
		for(Texture t:textures)
			if(t.contains(this, requiredMinimumRegion, metaData.localDateTime))
			{
				t.usedByCurrentRenderPass=true;
				return new FutureValue<PreparedImage>(new PreparedImage(t));
			}
		
		//search an empty spot, starting at the end (=oldest)
		int textureNr;
		for(textureNr=textures.size()-1;textureNr>=0;textureNr--)
			if(!textures.get(textureNr).usedByCurrentRenderPass)
				break;
		
		if(textureNr<0)
			//we didn't find a free spot?!? how should this be possible?
			//ImageLayer.ensureAppropriateTextureCacheSize ensures that we have
			//at least two textures per AbstractImageLayer
			throw new RuntimeException("Shouldn't be possible. Bug!");
		
		//move elements to ensure lru order (most recent = element #0)
		final Texture tex=textures.remove(textureNr);
		textures.add(0, tex);
		tex.invalidate();
		tex.usedByCurrentRenderPass=true;
		
		return exDecoder.submit(new Callable<PreparedImage>()
		{
			@SuppressWarnings("null")
			@Override
			public PreparedImage call() throws Exception
			{
				Thread.currentThread().setName("Decoder-"+Thread.currentThread().getId());
				
				ImageRegion requiredSafeRegion = new ImageRegion(
						requiredMinimumRegion.areaOfSourceImage,
						_panel.getTranslationCurrent().z,
						metaData,
						_size,
						TimeLine.SINGLETON.isPlaying() ? 1.05 : 1.2);
				
				return new PreparedImage(
						tex,
						requiredSafeRegion,
						MovieCache.decodeImage(sourceId, metaData.localDateTime, 16384 /* 0-8 */, requiredSafeRegion.decodeZoomFactor, requiredSafeRegion.texels)
					);
			}
		});
	}
}
