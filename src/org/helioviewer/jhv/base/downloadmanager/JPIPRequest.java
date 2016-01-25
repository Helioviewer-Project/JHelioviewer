package org.helioviewer.jhv.base.downloadmanager;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPDataSegment;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPRequestField;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

public class JPIPRequest extends AbstractDownloadRequest
{
	private final JPIPQuery query;

	private int JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;
	private volatile long lastResponseTime = -1;
	public final Kdu_cache kduCache = new Kdu_cache();
	
	public JPIPRequest(String url, DownloadPriority priority, int startFrame, int endFrame, Rectangle size)
	{
		super(url, priority);
		
		query = new JPIPQuery();
		query.setField(JPIPRequestField.CONTEXT.toString(), "jpxl<" + startFrame + "-" + endFrame + ">");
		query.setField(JPIPRequestField.LAYERS.toString(), String.valueOf(8));

		query.setField(JPIPRequestField.FSIZ.toString(), String.valueOf(size.width) + "," + String.valueOf(size.height) + "," + "closest");
		query.setField(JPIPRequestField.ROFF.toString(), String.valueOf(0) + "," + String.valueOf(0));
		query.setField(JPIPRequestField.RSIZ.toString(), String.valueOf(size.width) + "," + String.valueOf(size.height));
	}

	private volatile JPIPSocket jpipSocket;
	
	@Override
	void execute() throws IOException
	{
		jpipSocket = new JPIPSocket(TIMEOUT);

		try
		{
			openSocket(jpipSocket, kduCache);
			for(;;)
			{
				if (jpipSocket.isClosed())
					openSocket(jpipSocket, kduCache);
				
				query.setField(JPIPRequestField.LEN.toString(), String.valueOf(JpipRequestLen));

				org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPRequest request = new org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPRequest(query.toString());
				jpipSocket.send(request);
				@Nullable JPIPResponse response = jpipSocket.receive();
				
				if(response==null)
					throw new IOException();
				
				// Update optimal package size
				flowControl(jpipSocket);
				try
				{
					boolean complete = addJPIPResponseData(response,kduCache);
					if(complete)
						break;
				}
				catch (KduException e)
				{
					Telemetry.trackException(e);
				}
			}
			
			finished = true;
		}
		catch (URISyntaxException | UnsuitableMetaDataException | KduException e)
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

	private void openSocket(JPIPSocket _socket, Kdu_cache _kduCache) throws IOException, URISyntaxException, KduException
	{
		JPIPResponse jpipResponse = (JPIPResponse) _socket.connect(new URI(url));
		addJPIPResponseData(jpipResponse,_kduCache);
	}

	private boolean addJPIPResponseData(JPIPResponse jRes, Kdu_cache _kduCache) throws KduException
	{
		JPIPDataSegment data;
		while ((data = jRes.removeJpipDataSegment()) != null && !data.isEOR)
			_kduCache.Add_to_databin(data.classID.getKakaduClassID(),
				data.codestreamID, data.binID, data.data, data.offset,
				data.length, data.isFinal, true, false);

		return jRes.isResponseComplete();
	}

	private void flowControl(JPIPSocket jpipSocket)
	{
		int adjust = 0;
		int receivedBytes = jpipSocket.getReceivedData();
		long replyTextTime = jpipSocket.getReplyTextTime();
		long replyDataTime = jpipSocket.getReplyDataTime();

		long tdat = replyDataTime - replyTextTime;

		if (((receivedBytes - JpipRequestLen) < (JpipRequestLen >> 1)) && (receivedBytes > (JpipRequestLen >> 1)))
		{
			if (tdat > 10000)
				adjust = -1;
			else if (lastResponseTime > 0)
			{
				long tgap = replyTextTime - lastResponseTime;

				if ((tgap + tdat) < 1000)
					adjust = +1;
				else
				{
					double gapRatio = ((double) tgap) / ((double) (tgap + tdat));
					double targetRatio = ((double) (tdat + tgap)) / 10000.0;

					if (gapRatio > targetRatio)
						adjust = +1;
					else
						adjust = -1;
				}
			}
		}

		JpipRequestLen += (JpipRequestLen >> 2) * adjust;

		if (JpipRequestLen > JPIPConstants.MAX_REQUEST_LEN)
			JpipRequestLen = JPIPConstants.MAX_REQUEST_LEN;

		if (JpipRequestLen < JPIPConstants.MIN_REQUEST_LEN)
			JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

		lastResponseTime = replyDataTime;
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
}
