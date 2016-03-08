package org.helioviewer.jhv.viewmodel.jp2view.newjpx;

import java.awt.Dimension;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.format.DateTimeFormatter;
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
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Movie;
import org.helioviewer.jhv.layers.Movie.Match;
import org.helioviewer.jhv.layers.MovieFileBacked;
import org.helioviewer.jhv.layers.MovieKduCacheBacked;
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
	
	public KakaduLayer(int _sourceId, long _startMS, long _endMS, long _cadenceMS, String _name)
	{
		sourceId = _sourceId;
		name = _name;
		
		setTimeRange(_startMS, _endMS, _cadenceMS);
	}
	
	//FIXME: find the best quality frame within _cadence
	public @Nullable Match findBestFrame(long _currentTimeMS)
	{
		Match m = MovieCache.findBestFrame(sourceId, _currentTimeMS);
		if(m==null)
			return null;
		
		if(m.timeDifferenceMS>cadenceMS && noFrames.contains(_currentTimeMS))
			return null;
		
		return m;
	}
	
	@Override
	public boolean isDataAvailableOnServer(long _timeMS)
	{
		return !noFrames.contains(_timeMS);
	}
	
	public KakaduLayer(String _filePath) throws IOException
	{
		localPath = _filePath;
		
		sourceId = genSourceId(_filePath);
		localFile = true;
		
		try
		{
			Movie movie = new MovieFileBacked(sourceId,_filePath);
			if(movie.getAnyMetaData()==null)
				throw new UnsuitableMetaDataException();
		
			name = movie.getAnyMetaData().displayName;
			
			SortedSet<Long> times=new TreeSet<Long>();
			for(int i=1;i<movie.getFrameCount();i++)
			{
				MetaData md=movie.getMetaData(i);
				if(md!=null)
					times.add(md.timeMS);
			}
	
			Long[] sortedTimes=times.toArray(new Long[0]);
			startMS = sortedTimes[0];
			endMS = sortedTimes[sortedTimes.length-1];
			
			cadenceMS = endMS-startMS;
			for(int i=2;i<sortedTimes.length;i++)
				cadenceMS=Math.min(cadenceMS, (sortedTimes[i]-sortedTimes[i-2])/2);
			
			if(cadenceMS<1)
				cadenceMS=1;
			
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
		_json.put("cadenceMS", cadenceMS);
		_json.put("startTimeMS", startMS);
		_json.put("endTimeMS", endMS);
		
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
			long startMS = jsonLayer.getLong("startTimeMS");
			long endMS = jsonLayer.getLong("endTimeMS");
			l = new KakaduLayer(jsonLayer.getInt("id"), startMS, endMS, jsonLayer.getLong("cadenceMS"), jsonLayer.getString("name"));
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
	
	private void addRangeRequest(long _startMS, long _endMS, final long _cadenceMS)
	{
		if(_cadenceMS<1)
			throw new IllegalArgumentException("Invalid cadence: "+_cadenceMS);
		
		while(_startMS<_endMS)
		{
			long a = _startMS;
			long b = _startMS + _cadenceMS;
			long middle = _startMS+_cadenceMS/2;
			
			if(!noFrames.fullyContains(a,b))
			{
				Match bestMatch = findBestFrame(middle);
				if(bestMatch==null || bestMatch.timeDifferenceMS>=_cadenceMS/2 || !bestMatch.movie.isFullQuality())
				{
					//this doesn't work for some unknown reason
					/*if(bestMatch!=null && !bestMatch.movie.isFullQuality() && bestMatch.timeDifferenceMS<_cadenceMS/2)
					{
						//we already have a match: re-fetch the exact same frame (but this time in better quality)
						long timeMS = bestMatch.movie.getTimeMS(bestMatch.index);
						startTimes.add(timeMS/1000);
						endTimes.add(timeMS/1000+1);
					}
					else*/
					{
						startTimes.add(a/1000);
						endTimes.add((b+999)/1000);
					}
				}
			}
			
			_startMS += _cadenceMS;
		}
	}
	
	static final int SETTINGS_JPIP_BATCH_SIZE=Settings.getInt(IntKey.JPIP_BATCH_SIZE);
	static final int SETTINGS_PREVIEW_TIME_SUBSAMPLE=Settings.getInt(IntKey.PREVIEW_TEMPORAL_SUBSAMPLE);
	static final int SETTINGS_PREVIEW_SPATIAL_START=Settings.getInt(IntKey.PREVIEW_SPATIAL_START);
	
	static final IntervalStore<Long> noFrames=new IntervalStore<>();
	
	private void setTimeRange(final long _startMS, final long _endMS, final long _cadenceMS)
	{
		if(loaderThread!=null)
			throw new IllegalStateException("Shouldn't restart while existing thread is still running.");
		
		//round down/up to the nearest _cadence block (helps avoid jitter in general and reduces required frames)
		cadenceMS = _cadenceMS;
		startMS = (_startMS/cadenceMS)*cadenceMS;
		endMS = startMS + ((((_endMS-startMS)+cadenceMS-1)/cadenceMS)*cadenceMS);
		
		loaderThread = new Thread(() ->
		{
			LinkedList<JPIPRequest> pendingJPIP = new LinkedList<>();
			
			try
			{
				//download metadata
				ArrayList<MovieKduCacheBacked> pendingMovies = new ArrayList<MovieKduCacheBacked>();
				
				long curCadenceMS = (long)Math.round(Math.pow(SETTINGS_PREVIEW_TIME_SUBSAMPLE, Math.ceil(Math.log(endMS-startMS)/Math.log(SETTINGS_PREVIEW_TIME_SUBSAMPLE))-1));
				for(;;)
				{
					//round down/up to the nearest _cadence block (helps avoid jitter in general and reduces required frames)
					final long fStartMS;
					final long fEndMS;
					if(curCadenceMS>_cadenceMS)
					{
						fStartMS = (_startMS/curCadenceMS)*curCadenceMS;
						fEndMS = ((_endMS+curCadenceMS-1)/curCadenceMS)*curCadenceMS;
					}
					else
					{
						fStartMS = _startMS-curCadenceMS/2;
						fEndMS = _endMS-(curCadenceMS+1)/2;
					}
					
					final long fCadence = curCadenceMS;
					SwingUtilities.invokeAndWait(() -> addRangeRequest(fStartMS, fEndMS, fCadence));
					
					System.out.println("Using "+((fEndMS-fStartMS)/fCadence)+" frames total, downloading "+startTimes.size()+" at cadence "+fCadence);
					
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
								{
									noFrames.addInterval(download.from.get(i)*1000, download.to.get(i)*1000);
								}
								else
									validFrames++;

							//double check api response, to detect misunderstandings & bugs
							if(!Globals.IS_RELEASE_VERSION)
								for(int f=0; f < frames.length();f++)
									if(!"null".equalsIgnoreCase(frames.getString(f)))
									{
										long ts = frames.getLong(f);
										if(noFrames.contains(ts*1000))
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
								final MovieKduCacheBacked m = new MovieKduCacheBacked(sourceId, validFrames, new URI(jsonObject.getString("uri")));
								pendingMovies.add(m);
								SwingUtilities.invokeLater(() -> MovieCache.add(m));
							}
						}
						catch(Throwable _e)
						{
							if(curCadenceMS<=_cadenceMS)
								incomplete=true;
							Telemetry.trackException(_e);
						}
					}
					
					downloadMoreMovieData(pendingJPIP, pendingMovies, Integer.MAX_VALUE, SETTINGS_PREVIEW_SPATIAL_START, SETTINGS_PREVIEW_SPATIAL_START);
					
					if(curCadenceMS==_cadenceMS)
						break;
					
					curCadenceMS/=SETTINGS_PREVIEW_TIME_SUBSAMPLE;
					if(curCadenceMS<_cadenceMS)
						curCadenceMS=_cadenceMS;
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
	
	private void downloadMoreMovieData(LinkedList<JPIPRequest> pendingJPIP, ArrayList<MovieKduCacheBacked> pendingMovies, int _qualityLayers, int _width, int _height) throws InterruptedException
	{
		for(MovieKduCacheBacked m:pendingMovies)
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
					cur.m.notifyAboutUpgradedQuality(Integer.MAX_VALUE, Integer.MAX_VALUE);
				
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
	public long getCurrentTimeMS()
	{
		long now=TimeLine.SINGLETON.getCurrentTimeMS();
		if(now<startMS-cadenceMS || now>endMS+cadenceMS)
			return 0;
		
		Match match = findBestFrame(TimeLine.SINGLETON.getCurrentTimeMS());
		if(match==null)
			return 0;
		
		long timeMS = match.getTimeMS();
		if(timeMS<startMS-cadenceMS || timeMS>endMS+cadenceMS)
			return 0;
		
		return timeMS;
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
		
		if((endMS-startMS) / cadenceMS >= 1000)
			return null;
		
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
		
		return Globals.JPX_DATASOURCE_TRADITIONAL
				+ "?startTime=" + MathUtils.toLDT(startMS).format(formatter)
				+ "&endTime=" + MathUtils.toLDT(endMS).format(formatter)
				+ "&sourceId=" + sourceId
				+ "&cadence=" + cadenceMS/1000;
	}

	public boolean isLocalFile()
	{
		return localFile;
	}
	
	public long getTimeMS(long _timeMS)
	{
		Match match = findBestFrame(_timeMS);
		if (match == null)
			return 0;
		
		long timeMS=match.getTimeMS();
		if(timeMS<startMS-cadenceMS || timeMS>endMS+cadenceMS)
			return 0;
		
		return timeMS;
	}

	@Nullable
	public MetaData getMetaData(long _timeMS)
	{
		Match match = findBestFrame(_timeMS);
		if (match == null)
			return null;
		
		MetaData md=match.getMetaData();
		if(md == null)
			return null;
		
		initializeMetadata(md);
		
		if(md.timeMS<startMS-cadenceMS || md.timeMS>endMS+cadenceMS)
			return null;
		
		return md;
	}
	
	public @Nullable Document getMetaDataDocument(long _timeMS)
	{
		Match match = findBestFrame(_timeMS);
		if (match == null)
			return null;
		
		return match.movie.readMetadataDocument(match.index);
	}
	
	public ListenableFuture<PreparedImage> prepareImageData(final MainPanel _panel, final DecodeQualityLevel _quality, final Dimension _size, final GLContext _gl)
	{
		final long mainTime = TimeLine.SINGLETON.getCurrentTimeMS();
		if(mainTime<startMS-cadenceMS || mainTime>endMS+cadenceMS)
			return Futures.immediateFuture(null);
		
		final MetaData metaData = getMetaData(mainTime);
		if (metaData == null)
			return Futures.immediateFuture(null);
		
		if(metaData.timeMS<startMS-cadenceMS || metaData.timeMS>endMS+cadenceMS)
			return Futures.immediateFuture(null);
		
		final ImageRegion requiredMinimumRegion = calculateRegion(_panel, _quality, metaData, _size);
		if (requiredMinimumRegion == null)
			return Futures.immediateFuture(null);
		
		for(Texture t:textures)
			if(t.contains(this, _quality, requiredMinimumRegion, metaData.timeMS))
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
			
			Match bestMatch=findBestFrame(metaData.timeMS);
			if(bestMatch!=null)
			{
				long timeMS = bestMatch.getTimeMS();
				if(timeMS<startMS-cadenceMS || timeMS>endMS+cadenceMS)
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
		
		setTimeRange(startMS, endMS, cadenceMS);
	}
}
