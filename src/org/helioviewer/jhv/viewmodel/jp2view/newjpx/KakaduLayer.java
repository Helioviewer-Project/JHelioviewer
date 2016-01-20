package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.FutureValue;
import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.DownloadManager;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
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
	public static final int MAX_FRAME_DOWNLOAD_BATCH = 24;
	
	@Nullable private volatile Thread loaderThread;
	private boolean localFile = false;
	private final int sourceId;
	protected @Nullable String localPath;
	
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
		
		start = movie.getMetaData(0).localDateTime;
		end = movie.getMetaData(movie.getFrameCount()-1).localDateTime;
		name = movie.getAnyMetaData().displayName;
		
		cadence = (int) (ChronoUnit.SECONDS.between(start, end) / movie.getFrameCount());
		
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
			jsonLayer.put("coronaVisibility", coronaVisible);
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
			coronaVisible=jsonLayer.getBoolean("coronaVisibility");
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
	
	//private static HashMap<Integer,IntervalStore> unavailableData = new HashMap<>();

	private static HashMap<String,Integer> usedIDs=new HashMap<>();
	private static int unusedID=-1;
	
	/**
	 * This method creates new, otherwise unused SourceId's for sources
	 * that stem from files. Helioviewer.org only ever uses positive id's.
	 * 
	 * @param _filename
	 * @return Associated (negative) ID
	 */
	private static int genSourceId(String _filename)
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
			private LinkedList<MovieDownload> downloads = new LinkedList<>();
			private boolean incomplete=false; //FIXME: should be respected by layer manager + retry logic
			
			@Override
			public void run()
			{
				ArrayList<Long> startTimes = new ArrayList<Long>();
				ArrayList<Long> endTimes = new ArrayList<Long>();
				
				LocalDateTime currentStart = start;
				while (currentStart.isBefore(end))
				{
					LocalDateTime currentEnd = currentStart.plusSeconds(cadence);
					if(currentEnd.isAfter(end))
						currentEnd = end;
					
					//don't re-download already cached images
					long maxDistance = currentStart.until(currentEnd, ChronoUnit.SECONDS)/2;
					Match bestMatch=MovieCache.findBestFrame(sourceId, currentStart.plusSeconds(maxDistance));
					if(bestMatch == null || bestMatch.timeDifferenceSeconds>maxDistance)
					{
						startTimes.add(currentStart.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS));
						endTimes.add(currentEnd.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS));
					}
					
					currentStart = currentEnd;
				}
				
				while (!startTimes.isEmpty())
				{
					StringBuilder currentStarts = new StringBuilder();
					StringBuilder currentEnds = new StringBuilder();
					
					for(int i=0;i<MAX_FRAME_DOWNLOAD_BATCH && !startTimes.isEmpty(); i++) 
					{
						if(i>0)
						{
							currentStarts.append(',');
							currentEnds.append(',');
						}
						currentStarts.append(startTimes.remove(0).toString());
						currentEnds.append(endTimes.remove(0).toString());
					}
					
					
					MovieDownload md=new MovieDownload();
					md.metadata = new HTTPRequest(Globals.JPX_DATASOURCE_MIDPOINT
							+ "?sourceId=" + sourceId
							+ "&jpip=true"
							+ "&verbose=true"
							+ "&startTimes=" + currentStarts.toString()
							+ "&endTimes=" + currentEnds.toString(),
							DownloadPriority.HIGH);
					
					md.hq = new JPIPDownloadRequest(Globals.JPX_DATASOURCE_MIDPOINT
							+ "?sourceId=" + sourceId
							+ "&startTimes=" + currentStarts.toString()
							+ "&endTimes=" + currentEnds.toString(),
							MovieCache.generateFilename(sourceId),
							DownloadPriority.LOW);
					
					DownloadManager.addRequest(md.metadata);
					DownloadManager.addRequest(md.hq);
					
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
						if(download.metadata!=null && download.metadata.isFinished())
						{
							try
							{
								JSONObject jsonObject = new JSONObject(download.metadata.getDataAsString());
								if (jsonObject.has("error"))
								{
									System.err.println("JSON error: "+download.metadata.getDataAsString());
									
									DownloadManager.remove(download.metadata);
									DownloadManager.remove(download.lq);
									DownloadManager.remove(download.hq);
									
									incomplete=true;
									download.metadata=null;
									continue;
								}
								
								downloads.addLast(download);
								
								JSONArray frames = ((JSONArray) jsonObject.get("frames"));
								download.lq = new JPIPRequest(jsonObject.getString("uri"), DownloadPriority.MEDIUM, 0, frames.length(), new Rectangle(256, 256));
								
								DownloadManager.addRequest(download.lq);
								download.metadata=null;
							}
							catch(JSONException _e)
							{
								DownloadManager.remove(download.metadata);
								DownloadManager.remove(download.lq);
								DownloadManager.remove(download.hq);

								Telemetry.trackException(_e);
								
								incomplete=true;
								download.lq=null;
								download.hq=null;
								download.metadata=null;
							}
						}
						else if(download.metadata==null && download.hq!=null && download.hq.isFinished())
						{
							DownloadManager.remove(download.metadata);
							DownloadManager.remove(download.lq);
							
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
									}
								});
							}
							catch(UnsuitableMetaDataException _umde)
							{
								incomplete=true;
								Telemetry.trackException(_umde);
							}
							
							download.metadata = null;
							download.hq = null;
							download.lq = null;
						}
						else if(download.metadata==null && download.lq!=null && download.lq.isFinished())
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
						DownloadManager.remove(md.hq);
					if(md.lq!=null)
						DownloadManager.remove(md.lq);
					if(md.metadata!=null)
						DownloadManager.remove(md.metadata);
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

	public @Nullable LocalDateTime findClosestLocalDateTime(LocalDateTime _currentDateTime)
	{
		Match match = MovieCache.findBestFrame(sourceId, _currentDateTime);
		if(match==null)
			return null;
		
		return match.movie.getMetaData(match.index).localDateTime;
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
		return Globals.JPX_DATASOURCE_TRADITIONAL + "?startTime=" + start.format(formatter) + "&endTime=" + end.format(formatter) + "&sourceId=" + sourceId + "&cadence=" + cadence;
	}

	public boolean isLocalFile()
	{
		return localFile;
	}

	@Nullable
	public MetaData getMetaData(LocalDateTime currentDateTime)
	{
		return MovieCache.getMetaData(sourceId, currentDateTime);
	}
	
	public @Nullable Document getMetaDataDocument(LocalDateTime currentDateTime)
	{
		return MovieCache.getMetaDataDocument(sourceId, currentDateTime);
	}
	
	public Future<PreparedImage> prepareImageData(final MainPanel _panel, final Dimension _size)
	{
		final MetaData metaData = getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
		if (metaData == null)
			return new FutureValue<>(null);
		
		if(lut==null)
			setLUT(metaData.getDefaultLUT());
		
		final ImageRegion requiredMinimumRegion = calculateRegion(_panel, metaData, _size);
		if (requiredMinimumRegion == null)
			return new FutureValue<>(null);
		
		for(Texture t:textures)
			if(t.contains(this, requiredMinimumRegion, metaData.localDateTime))
			{
				t.usedByCurrentRenderPass=true;
				return new FutureValue<>(new PreparedImage(t));
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
