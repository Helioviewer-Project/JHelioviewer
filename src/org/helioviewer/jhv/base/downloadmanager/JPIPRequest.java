package org.helioviewer.jhv.base.downloadmanager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.layers.MovieKduCacheBacked;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPDatabinClass;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;

import kdu_jni.KduException;
import kdu_jni.Kdu_global;

public class JPIPRequest extends AbstractDownloadRequest
{
	private final JPIPQuery query;
	public final MovieKduCacheBacked m;
	public final int qualityLayers;
	public final int width;
	public final int height;
	
	public boolean imageComplete=false;
	
	public JPIPRequest(DownloadPriority priority, int _qualityLayers, int _width, int _height, MovieKduCacheBacked _m)
	{
		super(_m.jpipURI.toString(), priority);
		
		m=_m;
		qualityLayers=_qualityLayers;
		width=_width;
		height=_height;
		
		query = new JPIPQuery();
		query.setField("type", "jpp-stream");
		query.setField("context", "jpxl<0-" + (_m.getFrameCount()-1) + ">");
		query.setField("layers", String.valueOf(_qualityLayers));
		query.setField("fsiz", _width + "," + _height + ",closest");
		query.setField("rsiz", _width + "," + _height);
		query.setField("roff", "0,0");
		
		//hack: esa-jpip currently only supports stateful requests ?!?
		query.setField("cnew", "http");
		
		//hack: esa-jpip currently only supports requests WITH len
		query.setField("len", String.valueOf(Integer.MAX_VALUE));
		
		try
		{
			LinkedHashMap<Long, ArrayList<String>> cacheContents = m.getCachedDatabins();
			
			if(!cacheContents.isEmpty())
			{
				StringBuilder sbModel = new StringBuilder();
				long prevStartId = -1;
				long prevMaxId = -1;
				
				while(!cacheContents.isEmpty())
				{
					long maxId=cacheContents.keySet().stream().mapToLong(l -> l.longValue()).max().getAsLong();
					String element = cacheContents.get(maxId).get(0);
					
					long startId=maxId;
					while(cacheContents.get(startId-1)!=null && cacheContents.get(startId-1).contains(element) && startId>0)
						startId--;
					
					if(startId!=prevStartId || maxId!=prevMaxId)
					{
						sbModel.append(",[");
						sbModel.append(startId);
						if(startId!=maxId)
							sbModel.append("-"+maxId);
						sbModel.append("]");
						
						prevStartId=startId;
						prevMaxId=maxId;
					}
					
					sbModel.append(","+element);
					
					for(long id=startId;id<=maxId;id++)
					{
						cacheContents.get(id).remove(element);
						if(cacheContents.get(id).isEmpty())
							cacheContents.remove(id);
					}
				}
				
				query.setField("model", sbModel.substring(1));
			}
		}
		catch (KduException _e)
		{
			Telemetry.trackException(_e);
		}
	}

	private volatile JPIPSocket jpipSocket;
	
	@Override
	void execute() throws IOException
	{
		if(!isRequired())
			return;
		
		try
		{
			jpipSocket = new JPIPSocket(new URI(url), TIMEOUT);
			@Nullable JPIPResponse response = jpipSocket.send(query.toString(), data ->
					m.addToDatabin(data.classID.getKakaduClassID(),
						data.codestreamID, data.binID, data.data, data.offset,
						data.length, data.isFinal));
			
			if(response==null)
				throw new IOException();
			
			/*if(!response.isResponseComplete() && response.getHeader("JPIP-cnew")!=null)
			{
				query.removeField("cnew");
				
				String[] tokens = response.getHeader("JPIP-cnew").split(",");
				String cnew=Arrays.stream(tokens).filter(s -> s.startsWith("cid=")).findAny().get().split("=",2)[1];
				query.setField("cid", Integer.parseInt(cnew)+"");
				
				String path="/" + Arrays.stream(tokens).filter(s -> s.startsWith("path=")).findAny().get().split("=",2)[1];
				jpipSocket.uri = new URI(jpipSocket.uri.getScheme(), jpipSocket.uri.getHost(), path, jpipSocket.uri.getFragment());
				
				response = jpipSocket.send(query.toString());
				if(response==null)
					throw new IOException();
			}*/
			
			//TODO: verify tid
			
			imageComplete = response.isImageComplete();
			m.notifyAboutUpgradedQuality(imageComplete ? Integer.MAX_VALUE : width*height, imageComplete ? Integer.MAX_VALUE : qualityLayers);
		}
		catch (URISyntaxException | UnsuitableMetaDataException | IOException e)
		{
			Telemetry.trackException(e);
		}
		finally
		{
			try
			{
				jpipSocket.close();
			}
			catch (IOException e)
			{
				Telemetry.trackException(e);
			}
		}
	}

	@Override
	public void interrupt()
	{
		try
		{
			cancelled=true;
			jpipSocket.close();
		}
		catch(Throwable t)
		{
		}
	}

	private boolean isRequired()
	{
		if(width*height<m.areaLimit.get())
			return false;
		
		if(width*height==m.areaLimit.get() && qualityLayers<=m.qualityLayersLimit.get())
			return false;
		
		return true;
	}
}
