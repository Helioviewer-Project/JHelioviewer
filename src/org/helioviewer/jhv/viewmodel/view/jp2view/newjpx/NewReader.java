package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPRequest.Method;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDataSegment;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDatabinClass;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequest;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequestField;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_Kdu_cache;

public class NewReader implements JHVReader, Callable<JHVCachable> {

	/** Whether IOExceptions should be shown on System.err or not */
	private static final boolean VERBOSE = false;

	/** The thread that this object runs on. */
	private volatile Thread myThread;

	/** A boolean flag used for stopping the thread. */
	private volatile boolean stop;

	/** A reference to the JP2Image this object is owned by. */
	private JP2Image jp2Image;

	/** The JPIPSocket used to connect to the server. */
	private JPIPSocket socket;

	/** The a reference to the cache object used by the run method. */
	private JHV_Kdu_cache cacheRef;

	private ImageLayer imageLayer;

	
	/**
	 * The time when the last response was received. It is used for performing
	 * the flow control. A negative value means that there is not a previous
	 * valid response to take into account.
	 */
	private volatile long lastResponseTime = -1;

	/** The current length in bytes to use for requests */
	private int JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;

	private Deque<JPIPQuery> requests;
	private URI uri;
	private ResolutionSet resolutionSet;


	/**
	 * The constructor. Creates and connects the socket if image is remote.
	 * 
	 * @param _imageViewRef
	 * @throws URISyntaxException
	 */
	public NewReader(String url, int instrumentID, ResolutionSet resolutionSet)
			throws URISyntaxException {
		this.uri = new URI(url);
		this.resolutionSet = resolutionSet;
		this.requests = new LinkedList<JPIPQuery>();
		this.imageLayer = new ImageLayer(instrumentID);

		socket = new JPIPSocket();
		createRequests();
	}

	private void createRequests() {
		ResolutionLevel resolutionLevel = resolutionSet
					.getResolutionLevel(0);
			Rectangle rect = resolutionLevel.getResolutionBounds();

			SubImage subImage = new SubImage(rect);
			JPIPQuery query = this
					.createQuery(new JP2ImageParameter(subImage,
							resolutionLevel, 8, 0), 0,
							UltimateLayer.MAX_FRAME_SIZE - 1);
			System.out.println("query   : " + query);
			requests.push(query);
		
	}

	private void openSocket() {
		try {
			JPIPResponse res = (JPIPResponse) socket.connect(uri);
			addJPIPResponseData(res);
			String jpipTargetID = res.getHeader("JPIP-tid");
			System.out.println("jpipTargetID : " + jpipTargetID);
		} catch (IOException | JHV_KduException e) {
			e.printStackTrace();
		}

	}

	public void receiveData() throws IOException {
		while (!requests.isEmpty()) {
			if (socket.isClosed())
				openSocket();
			JPIPDataSegment data = null;
			JPIPQuery query = requests.poll();
			JPIPRequest request = new JPIPRequest(Method.GET);
			
			boolean complete = false;
			do {
				if (socket.isClosed())
					openSocket();
				
			query.setField(JPIPRequestField.LEN.toString(), String.valueOf(JpipRequestLen));

				//System.out.println("query : " + query);
				request.setQuery(query);
			socket.send(request);
			JPIPResponse response = socket.receive();
			// Update optimal package size
			flowControl();
			try {
				complete = this.addJPIPResponseData(response);
			} catch (JHV_KduException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			System.out.println("size : " + imageLayer.getSize());
			
			} while (!(complete));
			imageLayer.setComplete(complete);
			System.out.println("complete : " + uri);
		}

	}

	/**
	 * This method perfoms the flow control, that is, adjusts dynamically the
	 * value of the variable <code>JPIP_REQUEST_LEN</code>. The used algorithm
	 * is the same as the used one by the viewer kdu_show of Kakadu (if
	 * something works... why not to use it?)
	 */
	private void flowControl() {
		int adjust = 0;
		int receivedBytes = socket.getReceivedData();
		long replyTextTime = socket.getReplyTextTime();
		long replyDataTime = socket.getReplyDataTime();

		long tdat = replyDataTime - replyTextTime;

		if (((receivedBytes - JpipRequestLen) < (JpipRequestLen >> 1))
				&& (receivedBytes > (JpipRequestLen >> 1))) {
			if (tdat > 10000)
				adjust = -1;
			else if (lastResponseTime > 0) {
				long tgap = replyTextTime - lastResponseTime;

				if ((tgap + tdat) < 1000)
					adjust = +1;
				else {
					double gapRatio = ((double) tgap)
							/ ((double) (tgap + tdat));
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
	
    public boolean addJPIPResponseData(JPIPResponse jRes) throws JHV_KduException {
        JPIPDataSegment data;
        while ((data = jRes.removeJpipDataSegment()) != null && !data.isEOR)
        	try {
                imageLayer.getCache().Add_to_databin(data.classID.getKakaduClassID(), data.codestreamID, data.binID, data.data, data.offset, data.length, data.isFinal, true, false);
                imageLayer.addSize(data.length);
            } catch (KduException ex) {
                throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
            }
        return jRes.isResponseComplete();
    }
    
    private void updateServerCacheModel() throws JHV_KduException, IOException{
    	String cModel = this.buildCacheModelUpdateString();
        if (cModel == null)
            return;

        JPIPQuery cacheUpdateQuery = new JPIPQuery();
        cacheUpdateQuery.setField("model", cModel);

        JPIPRequest req = new JPIPRequest(JPIPRequest.Method.POST);
        req.setQuery(cacheUpdateQuery.toString());

        socket.send(req);
        JPIPResponse res = socket.receive();

        this.addJPIPResponseData(res);
    }
    
    
    public String buildCacheModelUpdateString() throws JHV_KduException {
        int length;
        long codestreamID, databinID;
        boolean isComplete[] = new boolean[1];
        StringBuilder cacheModel = new StringBuilder(1000);
        Kdu_cache cache = imageLayer.getCache();
        try {
            codestreamID = cache.Get_next_codestream(-1);

            while (codestreamID >= 0) {
                cacheModel.append("[" + codestreamID + "],");
                for (JPIPDatabinClass databinClass : JPIPDatabinClass.values()) {
                    databinID = cache.Get_next_lru_databin(databinClass.getKakaduClassID(), codestreamID, -1, false);
                    while (databinID >= 0) {
                        if (cache.Mark_databin(databinClass.getKakaduClassID(), codestreamID, databinID, false)) {
                            length = cache.Get_databin_length(databinClass.getKakaduClassID(), codestreamID, databinID, isComplete);
                            // Append the databinClass String and the databinID
                            cacheModel.append(databinClass.getJpipString() + (databinClass == JPIPDatabinClass.MAIN_HEADER_DATABIN ? "" : String.valueOf(databinID)));
                            // If its not complete append the length of the
                            // databin
                            if (!isComplete[0])
                                cacheModel.append(":" + String.valueOf(length));
                            cacheModel.append(",");
                        }
                        databinID = cache.Get_next_lru_databin(databinClass.getKakaduClassID(), codestreamID, databinID, false);
                    }
                }
                codestreamID = cache.Get_next_codestream(codestreamID);
            }
            if (cacheModel.length() > 0)
                cacheModel.deleteCharAt(cacheModel.length() - 1);

        } catch (KduException ex) {
            throw new JHV_KduException("Internal Kakadu error: " + ex.getMessage());
        }
        return cacheModel.toString();
    }

	public JPIPQuery createQuery(JP2ImageParameter currParams, int iniFrame,
			int endFrame) {
		JPIPQuery query = new JPIPQuery();

		query.setField(JPIPRequestField.CONTEXT.toString(), "jpxl<" + iniFrame
				+ "-" + endFrame + ">");
		query.setField(JPIPRequestField.LAYERS.toString(),
				String.valueOf(currParams.qualityLayers));

		Rectangle resDims = currParams.resolution.getResolutionBounds();

		query.setField(
				JPIPRequestField.FSIZ.toString(),
				String.valueOf(resDims.width) + ","
						+ String.valueOf(resDims.height) + "," + "closest");
		query.setField(
				JPIPRequestField.ROFF.toString(),
				String.valueOf(currParams.subImage.x) + ","
						+ String.valueOf(currParams.subImage.y));
		query.setField(
				JPIPRequestField.RSIZ.toString(),
				String.valueOf(currParams.subImage.width) + ","
						+ String.valueOf(currParams.subImage.height));

		return query;
	}

	@Override
	public FutureTask<JHVCachable> getData(LocalDateTime[] framesDateTimes) {
		this.imageLayer.setFramesDateTime(framesDateTimes);
		FutureTask<JHVCachable> futureTask = new FutureTask<JHVCachable>(this);
		// TODO Auto-generated method stub
		return futureTask;
	}

	@Override
	public JHVCachable call(){
		try {
			openSocket();
			updateServerCacheModel();
			receiveData();
			if (!socket.isClosed())
				this.socket.close();
			return this.imageLayer;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
