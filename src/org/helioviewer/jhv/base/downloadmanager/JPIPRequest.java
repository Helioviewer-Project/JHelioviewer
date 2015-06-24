package org.helioviewer.jhv.base.downloadmanager;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import kdu_jni.KduException;
import kdu_jni.Kdu_cache;

import org.helioviewer.jhv.layers.CacheableImageData;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.http.HTTPRequest.Method;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPConstants;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPDataSegment;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPQuery;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequestField;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPResponse;
import org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPSocket;
import org.helioviewer.jhv.viewmodel.view.jp2view.kakadu.JHV_KduException;

public class JPIPRequest extends AbstractRequest {

	private JPIPSocket jpipSocket;

	private int cacheSize = 0;

	private final JPIPQuery query;

	private int JpipRequestLen = JPIPConstants.MIN_REQUEST_LEN;
	private volatile long lastResponseTime = -1;
	private final CacheableImageData cacheableImageData;

	public JPIPRequest(String url, PRIORITY priority, int startFrame,
			int endFrame, Rectangle size,
			CacheableImageData cacheableImageData) {
		super(url, priority);
		this.cacheableImageData = cacheableImageData;
		jpipSocket = new JPIPSocket();

		query = new JPIPQuery();
		query.setField(JPIPRequestField.CONTEXT.toString(), "jpxl<"
				+ startFrame + "-" + endFrame + ">");
		query.setField(JPIPRequestField.LAYERS.toString(), String.valueOf(8));

		query.setField(JPIPRequestField.FSIZ.toString(),
				String.valueOf(size.width) + "," + String.valueOf(size.height)
						+ "," + "closest");
		query.setField(JPIPRequestField.ROFF.toString(), String.valueOf(0)
				+ "," + String.valueOf(0));
		query.setField(JPIPRequestField.RSIZ.toString(),
				String.valueOf(size.width) + "," + String.valueOf(size.height));

	}

	@Override
	void execute() throws IOException {
		if (cacheableImageData.getImageFile() != null) {
			finished = true;
		} else {
			receiveData();
		}
	}

	private void receiveData() throws IOException {
		try {
			openSocket();
			org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequest request = new org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip.JPIPRequest(
					Method.GET);
			boolean complete = false;
			do {
				System.out.println("receiveData");
				if (jpipSocket.isClosed())
					openSocket();
				if (cacheableImageData.getImageFile() != null) break;
				query.setField(JPIPRequestField.LEN.toString(),
						String.valueOf(JpipRequestLen));

				request.setQuery(query);
				jpipSocket.send(request);
				JPIPResponse response = jpipSocket.receive();
				// Update optimal package size
				flowControl();
				try {
					complete = this.addJPIPResponseData(response);
				} catch (JHV_KduException e) {
					e.printStackTrace();
				}
			} while (!(complete));
			cacheableImageData.markAsChanged();
			finished = true;
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JHV_KduException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				jpipSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void openSocket() throws IOException, URISyntaxException,
			JHV_KduException {
		JPIPResponse jpipResponse = (JPIPResponse) jpipSocket.connect(new URI(
				url));
		addJPIPResponseData(jpipResponse);
	}

	private boolean addJPIPResponseData(JPIPResponse jRes)
			throws JHV_KduException {
		JPIPDataSegment data;
		while ((data = jRes.removeJpipDataSegment()) != null && !data.isEOR)
			try {
				if (cacheableImageData.getImageFile() == null){
				cacheableImageData.getImageData().Add_to_databin(data.classID.getKakaduClassID(),
						data.codestreamID, data.binID, data.data, data.offset,
						data.length, data.isFinal, true, false);
				cacheSize += data.length;
				}
			} catch (KduException ex) {
				throw new JHV_KduException("Internal Kakadu error: "
						+ ex.getMessage());
			}
		return jRes.isResponseComplete();
	}

	private void flowControl() {
		int adjust = 0;
		int receivedBytes = jpipSocket.getReceivedData();
		long replyTextTime = jpipSocket.getReplyTextTime();
		long replyDataTime = jpipSocket.getReplyDataTime();

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

}
