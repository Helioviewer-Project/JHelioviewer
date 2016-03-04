package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.IntervalStore;
import org.helioviewer.jhv.base.Settings;
import org.helioviewer.jhv.base.Settings.IntKey;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.DownloadManager;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.JPIPRequest;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.opengl.Texture;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.TimeLine.DecodeQualityLevel;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.jogamp.opengl.GLContext;

import kdu_jni.KduException;

public class KakaduLayer extends ImageLayer
{
	@Nullable private volatile Thread loaderThread;
	private boolean localFile = false;
	private final int sourceId;
	protected @Nullable String localPath;
	
	public KakaduLayer(int _sourceId, LocalDateTime _start, LocalDateTime _end, int _cadence, String _name)
	{
		sourceId = _sourceId;
		name = _name;
		
		setTimeRange(_start, _end, _cadence);
	}
	
	public @Nullable Match findBestFrame(LocalDateTime _currentDateTime)
	{
		//FIXME: why is this broken?
		//if(noFrames.contains(_currentDateTime.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS)))
		//	return null;
		
		Match m = MovieCache.findBestFrame(sourceId, _currentDateTime);
		if(m==null)
			return null;
		
		return m;
	}
	
	@Override
	public boolean isDataAvailableOnServer(LocalDateTime _ldt)
	{
		return !noFrames.contains(_ldt.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS));
	}
	
	public KakaduLayer(String _filePath) throws IOException
	{
		localPath = _filePath;
		
		sourceId = genSourceId(_filePath);
		localFile = true;
		
		try
		{
			Movie movie = new Movie(sourceId,_filePath);
			if(movie.getAnyMetaData()==null)
				throw new UnsuitableMetaDataException();
		
			name = movie.getAnyMetaData().displayName;
			
			SortedSet<LocalDateTime> times=new TreeSet<LocalDateTime>();
			for(int i=1;i<movie.getFrameCount();i++)
			{
				MetaData md=movie.getMetaData(i);
				if(md!=null)
					times.add(md.localDateTime);
			}
	
			LocalDateTime[] sortedTimes=times.toArray(new LocalDateTime[0]);
			start = sortedTimes[0];
			end = sortedTimes[sortedTimes.length-1];
			
			cadence = (int)ChronoUnit.SECONDS.between(start, end);
			for(int i=2;i<sortedTimes.length;i++)
				cadence=Math.min(cadence, (int)ChronoUnit.SECONDS.between(sortedTimes[i-2], sortedTimes[i])/2);
			
			if(cadence<1)
				cadence=1;
			
			MovieCache.add(movie);
		}
		catch(KduException _k)
		{
			throw new IOException("Kakadu Exception ",_k);
		}
	}
	
	public void storeConfiguration(JSONObject _json) throws JSONException
	{
		_json.put("type", "kakadu");
		
		if(localPath!=null)
			_json.put("localPath", localPath);
		else
			_json.put("name", name);

		_json.put("id", sourceId);
		_json.put("cadence", cadence);
		_json.put("startDateTime", start);
		_json.put("endDateTime", end);
		
		storeJSONState(_json);
	}
	
	public static @Nullable KakaduLayer createFromJSON(JSONObject jsonLayer) throws JSONException, FileNotFoundException, IOException
	{
		KakaduLayer l;
		
		if (jsonLayer.has("localPath"))
		{
			l = new KakaduLayer(jsonLayer.getString("localPath"));
		}
		else
		{
			LocalDateTime start = LocalDateTime.parse(jsonLayer.getString("startDateTime"));
			LocalDateTime end = LocalDateTime.parse(jsonLayer.getString("endDateTime"));
			l = new KakaduLayer(jsonLayer.getInt("id"), start, end, jsonLayer.getInt("cadence"), jsonLayer.getString("name"));
		}
		
		l.applyJSONState(jsonLayer);
		return l;
	}
	
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
	
	static class MetadataDownload
	{
		@Nullable HTTPRequest metadata;
		
		List<Long> from = new ArrayList<Long>();
		List<Long> to = new ArrayList<Long>();
		
		MetadataDownload()
		{
		}
	}
	
	private volatile boolean incomplete;
	
	private final LinkedList<MetadataDownload> pendingMetadata = new LinkedList<>();
	private final ArrayList<Long> startTimes = new ArrayList<Long>();
	private final ArrayList<Long> endTimes = new ArrayList<Long>();
	
	private void startPendingDownloads()
	{
		while(!startTimes.isEmpty())
		{
			MetadataDownload md=new MetadataDownload();
			
			StringBuilder currentStarts = new StringBuilder();
			StringBuilder currentEnds = new StringBuilder();
			
			int batchSize=Settings.getInt(IntKey.JPIP_BATCH_SIZE);
			for(int i=0;i<batchSize && !startTimes.isEmpty(); i++) 
			{
				Long a = startTimes.remove(0);
				Long b = endTimes.remove(0);
				
				md.from.add(a);
				md.to.add(b);
				
				if(currentStarts.length()>0)
				{
					currentStarts.append(',');
					currentEnds.append(',');
				}
				currentStarts.append(a.toString());
				currentEnds.append(b.toString());
			}
			
			md.metadata = new HTTPRequest(Globals.JPX_DATASOURCE_MIDPOINT
					+ "?sourceId=" + sourceId
					+ "&jpip=true"
					+ "&verbose=true"
					+ "&startTimes=" + currentStarts.toString()
					+ "&endTimes=" + currentEnds.toString(),
					DownloadPriority.MEDIUM);
			
			/*md.hq = new JPIPDownloadRequest(Globals.JPX_DATASOURCE_MIDPOINT
					+ "?sourceId=" + sourceId
					+ "&startTimes=" + currentStarts.toString()
					+ "&endTimes=" + currentEnds.toString(),
					MovieCache.generateFilename(sourceId),
					DownloadPriority.LOW);*/
			
			pendingMetadata.add(md);
			DownloadManager.addRequest(md.metadata);
		}
	}
	
	private void addRangeRequest(long _start, long _end, final long _cadence)
	{
		if(_cadence<1)
			throw new IllegalArgumentException("Invalid cadence: "+_cadence);
		
		while(_start<_end)
		{
			long a = _start;
			long b = _start + _cadence;
			LocalDateTime middle = LocalDateTime.ofInstant(Instant.ofEpochSecond(_start + _cadence/2).plusMillis((_cadence&1)==1?500:0), ZoneOffset.UTC);
			
			if(!noFrames.fullyContains(a,b))
			{
				Match bestMatch = findBestFrame(middle);
				if(bestMatch==null || bestMatch.timeDifferenceSeconds>=_cadence/2 || !bestMatch.movie.isFullQuality())
				{
					if(bestMatch!=null && !bestMatch.movie.isFullQuality() && bestMatch.timeDifferenceSeconds<_cadence/2)
					{
						//we already have a match: re-fetch the exact same frame (but this time in better quality)
						long ts = bestMatch.movie.getMetaData(bestMatch.index).localDateTime.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS);
						startTimes.add(ts);
						endTimes.add(ts+1);
					}
					else
					{
						startTimes.add(a);
						endTimes.add(b);
					}
				}
			}
			
			_start += _cadence;
		}
	}
	
	static final int SETTINGS_JPIP_BATCH_SIZE=Settings.getInt(IntKey.JPIP_BATCH_SIZE);
	static final int SETTINGS_PREVIEW_TIME_SUBSAMPLE=Settings.getInt(IntKey.PREVIEW_TEMPORAL_SUBSAMPLE);
	static final int SETTINGS_PREVIEW_SPATIAL_START=Settings.getInt(IntKey.PREVIEW_SPATIAL_START);
	
	static final IntervalStore<Long> noFrames=new IntervalStore<>();
	
	private void setTimeRange(final LocalDateTime _start, final LocalDateTime _end, final int _cadence)
	{
		if(loaderThread!=null)
			throw new IllegalStateException("Shouldn't restart while existing thread is still running.");
		
		cadence = _cadence;
		start = _start;
		end = start.plusSeconds(((ChronoUnit.SECONDS.between(_start, _end)+cadence-1)/cadence)*cadence);
		
		loaderThread = new Thread(() ->
		{
			LinkedList<JPIPRequest> pendingJPIP = new LinkedList<>();
			
			try
			{
				//download metadata
				ArrayList<Movie> pendingMovies = new ArrayList<Movie>();
				
				//round down/up to the nearest _cadence block (helps avoid jitter in general and reduces required frames)
				long seconds = ChronoUnit.SECONDS.between(_start, _end);
				long cadence = (long)Math.round(Math.pow(SETTINGS_PREVIEW_TIME_SUBSAMPLE, Math.ceil(Math.log(seconds)/Math.log(SETTINGS_PREVIEW_TIME_SUBSAMPLE))-1));
				
				for(;;)
				{
					long start = _start.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS);
					long end = _end.atOffset(ZoneOffset.UTC).getLong(ChronoField.INSTANT_SECONDS);
					
					if(cadence>_cadence)
					{
						start = (start/cadence)*cadence;
						end = ((end+cadence-1)/cadence)*cadence;
					}
					else
					{
						start = start-cadence/2;
						end = end-(cadence+1)/2;
					}
					
					final long fStart = start;
					final long fEnd = end;
					final long fCadence = cadence;
					SwingUtilities.invokeAndWait(() -> addRangeRequest(fStart, fEnd, fCadence));
					
					System.out.println("Using "+((end-start)/cadence)+" frames total, downloading "+startTimes.size()+" at cadence "+cadence);
					
					startPendingDownloads();
					while(!pendingMetadata.isEmpty())
					{
						MetadataDownload download = pendingMetadata.removeFirst();
						if(!download.metadata.isFinished())
						{
							Thread.sleep(250);
							pendingMetadata.addLast(download);
							continue;
						}
						
						try
						{
							download.metadata.checkException();
							
							JSONObject jsonObject = new JSONObject(download.metadata.getDataAsString());
							if (jsonObject.has("error"))
								throw new JSONException("JSON error: "+download.metadata.getDataAsString());
							
							int validFrames = 0;
							JSONArray frames = jsonObject.getJSONArray("frames");
							for(int i=0;i<frames.length();i++)
								if("null".equalsIgnoreCase(frames.getString(i)))
									noFrames.addInterval(download.from.get(i), download.to.get(i));
								else
									validFrames++;

							//double check api response, to detect misunderstandings & bugs
							if(!Globals.IS_RELEASE_VERSION)
								for(int f=0; f < frames.length();f++)
									if(!"null".equalsIgnoreCase(frames.getString(f)))
									{
										long ts = frames.getLong(f);
										if(noFrames.contains(ts))
											//FIXME: still happening...
											System.err.println("API returned frame for "+ts+" when it previously found no such frame.");
										
										boolean found=false;
										for(int i=0;i<download.from.size();i++)
										{
											long from = download.from.get(i);
											long to = download.to.get(i);
											if(ts >= from && ts<to)
											{
												found=true;
												break;
											}
										}
										
										if(!found)
										{
											StringBuilder sb=new StringBuilder();
											for(int i=0; i < download.from.size();i++)
												sb.append("\n"+download.from.get(i)+"-"+download.to.get(i));
											
											System.err.println("API returned data from not requested time range: "+ts+". Requested:"+sb+"\nURL: "+download.metadata.toString());
										}
									}
							
							if(validFrames>0)
							{
								final Movie m = new Movie(sourceId, validFrames, new URI(jsonObject.getString("uri")));
								pendingMovies.add(m);
								SwingUtilities.invokeLater(() -> MovieCache.add(m));
							}
						}
						catch(Throwable _e)
						{
							if(cadence<=_cadence)
								incomplete=true;
							Telemetry.trackException(_e);
						}
					}
					
					downloadMoreMovieData(pendingJPIP, pendingMovies, Integer.MAX_VALUE, SETTINGS_PREVIEW_SPATIAL_START, SETTINGS_PREVIEW_SPATIAL_START);
					
					if(cadence==_cadence)
						break;
					
					cadence/=SETTINGS_PREVIEW_TIME_SUBSAMPLE;
					if(cadence<_cadence)
						cadence=_cadence;
				}
				
				//download actual movies, increasing resolution
				for(int res = SETTINGS_PREVIEW_SPATIAL_START; ; res<<=1)
				{
					downloadMoreMovieData(pendingJPIP, pendingMovies, Integer.MAX_VALUE, res, res);
					if(pendingMovies.isEmpty())
					{
						//TODO: slowly increase number of quality layers at the end
						break;
					}
				}
			}
			catch (InterruptedException _e)
			{
				incomplete=true;
			}
			catch(InvocationTargetException _ite)
			{
				Telemetry.trackException(_ite);
			}
			finally
			{
				System.out.println("Loader thread terminated.");
				loaderThread=null;
				
				SwingUtilities.invokeLater(() ->
				{
					for(Texture t:ImageLayer.textures)
						t.invalidate();
					
					MainFrame.SINGLETON.repaintLazy();
				});
				
				for(MetadataDownload md:pendingMetadata)
					DownloadManager.remove(md.metadata);
				
				for(JPIPRequest j:pendingJPIP)
					DownloadManager.remove(j);
			}
		}, "Layer loader "+sourceId);
		
		incomplete = false;
		loaderThread.setDaemon(true);
		loaderThread.start();
	}
	
	private void downloadMoreMovieData(LinkedList<JPIPRequest> pendingJPIP, ArrayList<Movie> pendingMovies, int _qualityLayers, int _width, int _height) throws InterruptedException
	{
		for(Movie m:pendingMovies)
		{
			JPIPRequest jpip = new JPIPRequest(DownloadPriority.MEDIUM, _qualityLayers, _width, _height, m);
			DownloadManager.addRequest(jpip);
			pendingJPIP.add(jpip);
		}
		
		boolean pendingRefresh = false;
		while(!pendingJPIP.isEmpty())
		{
			JPIPRequest cur = pendingJPIP.removeFirst();
			
			if(!cur.isFinished())
			{
				if(pendingRefresh)
				{
					SwingUtilities.invokeLater(() ->
					{
						for(Texture t:ImageLayer.textures)
							t.invalidate();
						MainFrame.SINGLETON.repaintLazy();
					});
					pendingRefresh=false;
				}
				
				Thread.sleep(250);
				pendingJPIP.addLast(cur);
				continue;
			}
			
			try
			{
				cur.checkException();
				
				if(cur.imageComplete)
					cur.m.notifyOfUpgradedQuality(Integer.MAX_VALUE, Integer.MAX_VALUE);
				
				pendingRefresh=true;
			}
			catch(Throwable _t)
			{
				incomplete=true;
				Telemetry.trackException(_t);
			}
		}

		SwingUtilities.invokeLater(() ->
		{
			for(Texture t:ImageLayer.textures)
				t.invalidate();
			MainFrame.SINGLETON.repaintLazy();
		});
		
		pendingMovies.removeIf(m -> m.isFullQuality());
	}
	
	@Override
	public @Nullable LocalDateTime getCurrentTime()
	{
		LocalDateTime ldt=TimeLine.SINGLETON.getCurrentDateTime();
		if(ldt.isBefore(start.minusSeconds(cadence)) || ldt.isAfter(end.plusSeconds(cadence)))
			return null;
		
		Match match = findBestFrame(TimeLine.SINGLETON.getCurrentDateTime());
		if(match==null)
			return null;
		
		LocalDateTime dt=match.getMetaData().localDateTime;
		if(dt.isBefore(start.minusSeconds(cadence)) || dt.isAfter(end.plusSeconds(cadence)))
			return null;
		
		return dt;
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
		Match match = findBestFrame(currentDateTime);
		if (match == null)
			return null;
		
		MetaData md=match.getMetaData();
		initializeMetadata(md);
		
		LocalDateTime dt=md.localDateTime;
		if(dt.isBefore(start.minusSeconds(cadence)) || dt.isAfter(end.plusSeconds(cadence)))
			return null;
		
		return md;
	}
	
	public @Nullable Document getMetaDataDocument(LocalDateTime currentDateTime)
	{
		Match match = findBestFrame(currentDateTime);
		if (match == null)
			return null;
		
		return match.movie.readMetadataDocument(match.index);
	}
	
	public ListenableFuture<PreparedImage> prepareImageData(final MainPanel _panel, final DecodeQualityLevel _quality, final Dimension _size, final GLContext _gl)
	{
		final LocalDateTime mainTime = TimeLine.SINGLETON.getCurrentDateTime();
		if(mainTime.isBefore(start.minusSeconds(cadence)) || mainTime.isAfter(end.plusSeconds(cadence)))
			return Futures.immediateFuture(null);
		
		final MetaData metaData = getMetaData(mainTime);
		if (metaData == null)
			return Futures.immediateFuture(null);
		
		if(metaData.localDateTime.isBefore(start.minusSeconds(cadence)) || metaData.localDateTime.isAfter(end.plusSeconds(cadence)))
			return Futures.immediateFuture(null);
		
		final ImageRegion requiredMinimumRegion = calculateRegion(_panel, _quality, metaData, _size);
		if (requiredMinimumRegion == null)
			return Futures.immediateFuture(null);
		
		for(Texture t:textures)
			if(t.contains(this, _quality, requiredMinimumRegion, metaData.localDateTime))
			{
				t.usedByCurrentRenderPass=true;
				return Futures.immediateFuture(new PreparedImage(this,t));
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
		
		final SettableFuture<PreparedImage> future=SettableFuture.create();
		
		Globals.runWithGLContext(() ->
		{
			ImageRegion requiredSafeRegion = new ImageRegion(
					requiredMinimumRegion.areaOfSourceImage,
					_quality,
					_panel.getTranslationCurrent().z,
					metaData,
					_size,
					TimeLine.SINGLETON.isPlaying() ? 1.05 : 1.2);
			
			Match bestMatch=findBestFrame(metaData.localDateTime);
			
			if(bestMatch!=null)
			{
				LocalDateTime dt=bestMatch.getMetaData().localDateTime;
				if(dt.isBefore(start.minusSeconds(cadence)) || dt.isAfter(end.plusSeconds(cadence)))
				{
					future.set(null);
					return;
				}
				
				if(bestMatch.decodeImage(_quality, requiredSafeRegion.decodeZoomFactor, requiredSafeRegion.texels, tex))
				{
					tex.needsUpload=true;
					/*
					tex.uploadByteBuffer(GLContext.getCurrentGL().getGL2(), KakaduLayer.this, bestMatch.getMetaData().localDateTime, requiredSafeRegion);
					if(Globals.isOSX())
						GLContext.getCurrentGL().glFlush();*/
					
					future.set(new PreparedImage(KakaduLayer.this,tex,requiredSafeRegion));
					return;
				}
			}

			tex.usedByCurrentRenderPass=false;
			future.set(null);
		});
		
		return future;
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
