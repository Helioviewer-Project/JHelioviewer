package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
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

import javax.swing.SwingUtilities;

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.base.FutureValue;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

public class KakaduLayer extends AbstractImageLayer
{
	public static final int MAX_FRAME_DOWNLOAD_BATCH = 15;
	private static final String URL = "http://api.helioviewer.org/v2/getJPX/?";
	
	public TreeSet<LocalDateTime> localDateTimes = new TreeSet<LocalDateTime>();

	private volatile Thread loaderThread;
	private boolean localFile = false;
	private final int sourceId;
	protected int cadence = -1;
	protected String localPath;
	
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
	
	public Match getMovie(LocalDateTime _currentDateTime)
	{
		return MovieCache.findBestFrame(sourceId, _currentDateTime);
	}

	public KakaduLayer(String _filePath)
	{
		localPath = _filePath;
		
		sourceId = genSourceId(_filePath);
		localFile = true;
		
		Movie movie = new Movie(this,sourceId);
		movie.setFile(_filePath);
		
		MovieCache.add(movie);
		name = movie.getMetaData(0).getFullName();
		
		start = getLocalDateTimes().first();
		end = getLocalDateTimes().last();
		cadence = (int) (ChronoUnit.SECONDS.between(start, end) / getLocalDateTimes().size());
	}
	
	public void writeStateFile(JSONObject jsonLayer)
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
			jsonLayer.put("lut", getLUT().ordinal());
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
			setLUT(Lut.values()[jsonLayer.getInt("lut")]);
			redChannel=jsonLayer.getBoolean("redChannel");
			greenChannel=jsonLayer.getBoolean("greenChannel");
			blueChannel=jsonLayer.getBoolean("blueChannel");
			
			setVisible(jsonLayer.getBoolean("visibility"));
			invertedLut = jsonLayer.getBoolean("invertedLut");
			coronaVisible=jsonLayer.getBoolean("coronaVisiblity");
			MainFrame.FILTER_PANEL.update();
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
	}

	//TODO: convert into constructor & combine with readStateFile
	public static KakaduLayer createFromStateFile(JSONObject jsonLayer)
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
	
	public String getLocalFilePath()
	{
		return localPath;
	}
	
	static class MovieDownload
	{
		HTTPRequest metadata;
		JPIPRequest lq;
		JPIPDownloadRequest hq;
		Movie movie;
	}
	
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
					md.movie = new Movie(KakaduLayer.this,sourceId);
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
							
							String hqFilename = download.hq.getFilename();
							download.hq = null;
							download.lq = null;
							
							try
							{
								download.movie.setFile(hqFilename);
								
								SwingUtilities.invokeLater(new Runnable()
								{
									@Override
									public void run()
									{
										MovieCache.add(download.movie);
									}
								});
							}
							catch(UnsuitableMetaDataException _umde)
							{
								Telemetry.trackException(_umde);
							}
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
								
								SwingUtilities.invokeLater(new Runnable()
								{
									@Override
									public void run()
									{
										addFrameDateTimes(localDateTimes);
									}
								});
								
								download.lq = new JPIPRequest(jsonObject.getString("uri"), DownloadPriority.URGENT, 0, frames.length(), new Rectangle(256, 256), download.movie);
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
									//FIXME: should only happen if current layer is active
									//FIXME: should only happen if needed
									MainFrame.MAIN_PANEL.repaint();
									MainFrame.OVERVIEW_PANEL.repaint();
									MainFrame.MOVIE_PANEL.repaint();
								}
							});
							Thread.sleep(500);
							downloads.addLast(download);
						}
					}
					catch(IOException _e)
					{
						incomplete=true;
						Telemetry.trackException(_e);
					}
					catch (InterruptedException _e)
					{
						incomplete=true;
						break;
					}
				}
				
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						MainFrame.MAIN_PANEL.repaint();
						MainFrame.OVERVIEW_PANEL.repaint();
						MainFrame.MOVIE_PANEL.repaint();
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
	public LocalDateTime getCurrentTime()
	{
		return findClosestLocalDateTime(TimeLine.SINGLETON.getCurrentDateTime());
	}

	private void addFrameDateTimes(LocalDateTime[] _localDateTimes)
	{
		for (LocalDateTime localDateTime : _localDateTimes)
			localDateTimes.add(localDateTime);
		
		//FIXME: should probably only be set if current layer is active
		TimeLine.SINGLETON.setLocalDateTimes(localDateTimes);
		MainFrame.MOVIE_PANEL.repaintSlider();
	}


	public LocalDateTime findClosestLocalDateTime(LocalDateTime _currentDateTime)
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

	public String getDownloadURL()
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

	public MetaData getMetaData(LocalDateTime currentDateTime)
	{
		if (localDateTimes.isEmpty())
			return null;
		
		return MovieCache.getMetaData(sourceId, currentDateTime);
	}
	
	public Document getMetaDataDocument(LocalDateTime currentDateTime)
	{
		if (localDateTimes.isEmpty())
			return null;
		
		return MovieCache.getMetaDataDocument(sourceId, currentDateTime);
	}
	
	public Future<PreparedImage> prepareImageData(final MainPanel mainPanel, final Dimension size)
	{
		final MetaData metaData = getMetaData(TimeLine.SINGLETON.getCurrentDateTime());
		if (metaData == null)
			return new FutureValue<PreparedImage>(null);
		
		//TODO: push instead of pull
		if(lut==null)
			lut=metaData.getDefaultLUT();
		
		final ImageRegion requiredMinimumRegion = calculateRegion(mainPanel, metaData, size);
		if (requiredMinimumRegion == null)
			return new FutureValue<PreparedImage>(null);
		
		if(texture.contains(this, requiredMinimumRegion, metaData.getLocalDateTime()))
			return new FutureValue<PreparedImage>(null);
		
		return exDecoder.submit(new Callable<PreparedImage>()
		{
			@Override
			public PreparedImage call() throws Exception
			{
				ImageRegion requiredSafeRegion = new ImageRegion(requiredMinimumRegion.requiredOfSourceImage, mainPanel.getTranslation().z, metaData,size,1.2);
				return new PreparedImage(
						requiredSafeRegion,
						MovieCache.decodeImage(sourceId, metaData.getLocalDateTime(), 8, requiredSafeRegion.decodeZoomFactor, requiredSafeRegion.texels),
						requiredSafeRegion.texels.width,
						requiredSafeRegion.texels.height
					);
			}
		});
	}
}
