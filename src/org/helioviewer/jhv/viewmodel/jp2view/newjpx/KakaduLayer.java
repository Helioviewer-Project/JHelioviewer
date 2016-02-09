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
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.BooleanKey;
import org.helioviewer.jhv.base.Settings.IntKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.DownloadManager;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.LUT;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.layers.Movie.Quality;
import org.helioviewer.jhv.opengl.Texture;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.DecodeQualityLevel;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.jogamp.opengl.GLContext;

public class KakaduLayer extends ImageLayer
{
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
		name = _name;
		
		cadence = _cadence;
		
		setTimeRange(start, end, cadence);
	}
	
	public @Nullable Match getMovie(LocalDateTime _currentDateTime)
	{
		return MovieCache.findBestFrame(sourceId, _currentDateTime);
	}
	
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
		if(cadence==0)
			cadence=1;
		
		MovieCache.add(movie);
	}
	
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
				setLUT(LUT.values()[jsonLayer.getInt("lut")]);
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
	
	private volatile boolean incomplete;
	
	//TODO: test what happens when the same sourceId is added twice, concurrently
	
	private void setTimeRange(final LocalDateTime _start, final LocalDateTime _end, final int _cadence)
	{
		if(loaderThread!=null)
			throw new IllegalStateException("Shouldn't restart while existing thread is still running.");
		
		loaderThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				int settingsPreviewTimeSubsample=Settings.getInt(IntKey.PREVIEW_TEMPORAL_SUBSAMPLE);
				int settingsPreviewSize=Settings.getInt(IntKey.PREVIEW_RESOLUTION);
				int settingsPreviewQuality=Settings.getInt(IntKey.PREVIEW_QUALITY);
				boolean settingsPreviewEnabled=Settings.getBoolean(BooleanKey.PREVIEW_ENABLED);

				//TODO: instead, request images by subdividing resolution
				LinkedList<MovieDownload> downloads = new LinkedList<>();
				try
				{
					ArrayList<Long> startTimes = new ArrayList<Long>();
					ArrayList<Long> endTimes = new ArrayList<Long>();
					
					LocalDateTime currentStart = _start;
					while (currentStart.isBefore(_end))
					{
						LocalDateTime currentEnd = currentStart.plusSeconds(_cadence);
						if(currentEnd.isAfter(_end))
							currentEnd = _end;
						
						//don't re-download already cached images
						long maxDistance = currentStart.until(currentEnd, ChronoUnit.SECONDS)/2;
						Match bestMatch=MovieCache.findBestFrame(sourceId, currentStart.plusSeconds(maxDistance));
						if(bestMatch == null || bestMatch.timeDifferenceSeconds>maxDistance || bestMatch.movie.quality!=Quality.FULL)
						{
							if(bestMatch != null && bestMatch.movie.quality!=Quality.FULL)
								try
								{
									SwingUtilities.invokeAndWait(new Runnable()
									{
										@Override
										public void run()
										{
											MovieCache.remove(bestMatch.movie);
										}
									});
								}
								catch(Throwable t)
								{
									Telemetry.trackException(t);
								}
							
							startTimes.add(currentStart.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS));
							endTimes.add(currentEnd.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS));
						}
						
						currentStart = currentEnd;
					}
					
					System.out.println("Downloading "+startTimes.size()+" frames");
					
					while (!startTimes.isEmpty())
					{
						StringBuilder currentStarts = new StringBuilder();
						StringBuilder currentEnds = new StringBuilder();
						StringBuilder ssCurrentStarts = new StringBuilder();
						StringBuilder ssCurrentEnds = new StringBuilder();
						
						int batchSize=Settings.getInt(IntKey.JPIP_BATCH_SIZE);
						for(int i=0;i<batchSize && !startTimes.isEmpty(); i++) 
						{
							String start=startTimes.remove(0).toString();
							String end=endTimes.remove(0).toString();
							
							if(currentStarts.length()>0)
							{
								currentStarts.append(',');
								currentEnds.append(',');
							}
							currentStarts.append(start);
							currentEnds.append(end);
							
							if(i%settingsPreviewTimeSubsample==0)
							{
								if(ssCurrentStarts.length()>0)
								{
									ssCurrentStarts.append(',');
									ssCurrentEnds.append(',');
								}
								ssCurrentStarts.append(start);
								ssCurrentEnds.append(end);
							}
						}
						
						MovieDownload md=new MovieDownload();
						
						if(settingsPreviewEnabled && ssCurrentStarts.length()>0)
							md.metadata = new HTTPRequest(Globals.JPX_DATASOURCE_MIDPOINT
									+ "?sourceId=" + sourceId
									+ "&jpip=true"
									+ "&verbose=true"
									+ "&startTimes=" + ssCurrentStarts.toString()
									+ "&endTimes=" + ssCurrentEnds.toString(),
									DownloadPriority.HIGH);
						
						md.hq = new JPIPDownloadRequest(Globals.JPX_DATASOURCE_MIDPOINT
								+ "?sourceId=" + sourceId
								+ "&startTimes=" + currentStarts.toString()
								+ "&endTimes=" + currentEnds.toString(),
								MovieCache.generateFilename(sourceId),
								DownloadPriority.LOW);
						
						downloads.add(md);
						
						currentStart = currentStart.plusSeconds(_cadence * batchSize);
					}
					
					for(MovieDownload md:downloads)
						DownloadManager.addRequest(settingsPreviewEnabled ? md.metadata : md.hq);
					
					while(!downloads.isEmpty())
					{
						//System.out.println("Pending download:");
						try
						{
							if (Thread.interrupted())
							{
								incomplete=true;
								break;
							}
							
							final MovieDownload download = downloads.removeFirst();
							//System.out.println(download.metadata+" "+download.lq+" "+download.hq+" "+download.lqMovie);
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
										continue;
									}
									
									boolean containsValidFrames = false;
									JSONArray frames = ((JSONArray) jsonObject.get("frames"));
									for(int i=0;i<frames.length();i++)
										if(!"null".equalsIgnoreCase(frames.getString(i)))
										{
											containsValidFrames = true;
											break;
										}
									
									if(containsValidFrames)
									{
										if(settingsPreviewEnabled)
										{
											//TODO: quality limiting doesn't work
											download.lq = new JPIPRequest(jsonObject.getString("uri"), DownloadPriority.MEDIUM, 0, frames.length()-1, settingsPreviewQuality, new Rectangle(settingsPreviewSize, settingsPreviewSize));
											DownloadManager.addRequest(download.lq);
										}

										DownloadManager.addRequest(download.hq);
										
										download.metadata=null;
										downloads.addLast(download);
									}
									else if(settingsPreviewTimeSubsample==1)
									{
										DownloadManager.remove(download.metadata);
										DownloadManager.remove(download.lq);
										DownloadManager.remove(download.hq);
										continue;
									}
								}
								catch(JSONException _e)
								{
									DownloadManager.remove(download.metadata);
									DownloadManager.remove(download.lq);
									DownloadManager.remove(download.hq);
	
									Telemetry.trackException(_e);
									
									incomplete=true;
									continue;
								}
							}
							else if(download.hq!=null && download.hq.isFinished())
							{
								DownloadManager.remove(download.metadata);
								DownloadManager.remove(download.lq);
								
								try
								{
									download.hq.checkException();
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
								catch(Throwable _t)
								{
									incomplete=true;
									Telemetry.trackException(_t);
									continue;
								}
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
							else if(download.hq!=null || download.lq!=null || download.metadata!=null)
							{
								downloads.addLast(download);
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
							}
							else
								throw new RuntimeException("Downloads shouldn't ever be in this state.");
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
				}
				finally
				{
					System.out.println("Loader thread terminated.");
					loaderThread=null;
					
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							for(Texture t:ImageLayer.textures)
								t.invalidate();
							MainFrame.SINGLETON.LAYER_PANEL.updateData();
							MainFrame.SINGLETON.MAIN_PANEL.repaint();
							MainFrame.SINGLETON.OVERVIEW_PANEL.repaint();
							MainFrame.SINGLETON.MOVIE_PANEL.repaint();
						}
					});
					
					for(MovieDownload md:downloads)
					{
						DownloadManager.remove(md.hq);
						DownloadManager.remove(md.lq);
						DownloadManager.remove(md.metadata);
					}
				}
			}
		}, "Layer loader "+sourceId);

		incomplete = false;
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
		{
			loaderThread.interrupt();
			incomplete=true;
		}
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
	
	public Future<PreparedImage> prepareImageData(final MainPanel _panel, final DecodeQualityLevel _quality, final Dimension _size, final GLContext _gl)
	{
		final LocalDateTime mainTime = TimeLine.SINGLETON.getCurrentDateTime();
		if(mainTime.isBefore(start.minusSeconds(cadence)) || mainTime.isAfter(end.plusSeconds(cadence)))
			return new FutureValue<>(null);
			
		final MetaData metaData = getMetaData(mainTime);
		if (metaData == null)
			return new FutureValue<>(null);
		
		if(metaData.localDateTime.isBefore(start.minusSeconds(cadence)) || metaData.localDateTime.isAfter(end.plusSeconds(cadence)))
			return new FutureValue<>(null);
		
		initializeMetadata(metaData);
		
		final ImageRegion requiredMinimumRegion = calculateRegion(_panel, _quality, metaData, _size);
		if (requiredMinimumRegion == null)
			return new FutureValue<>(null);
		
		for(Texture t:textures)
			if(t.contains(this, _quality, requiredMinimumRegion, metaData.localDateTime))
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
			@Override
			public @Nullable PreparedImage call() throws Exception
			{
				Thread.currentThread().setName("Decoder-"+Thread.currentThread().getId());
				
				ImageRegion requiredSafeRegion = new ImageRegion(
						requiredMinimumRegion.areaOfSourceImage,
						_quality,
						_panel.getTranslationCurrent().z,
						metaData,
						_size,
						TimeLine.SINGLETON.isPlaying() ? 1.05 : 1.2);
				
				if(MovieCache.decodeImage(sourceId, metaData.localDateTime, _quality, requiredSafeRegion.decodeZoomFactor, requiredSafeRegion.texels, tex))
				{
					tex.needsUpload=true;
					return new PreparedImage(tex,requiredSafeRegion);
				}
				else
				{
					tex.usedByCurrentRenderPass=false;
					return null;
				}
			}
		});
	}
	
	@Override
	public boolean retryNeeded()
	{
		return incomplete && loaderThread==null;
	}
	
	@Override
	public boolean isLoading()
	{
		return loaderThread!=null;
	}
	
	@Override
	public void retry()
	{
		if(!retryNeeded())
			return;
		
		setTimeRange(start, end, cadence);
	}
}
