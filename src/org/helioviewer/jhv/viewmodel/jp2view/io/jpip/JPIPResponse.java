package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import java.io.IOException;

import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPResponse;

/**
 * A response to a JPIPRequest. Encapsulates both the HTTPResponse headers and
 * the JPIPDataSegments.
 */
public class JPIPResponse extends HTTPResponse
{
	/** The status... can be EOR_WINDOW_DONE or EOR_IMAGE_DONE */
	long statusI = -1;

	public JPIPResponse(HTTPResponse res) throws IOException
	{
		super(res.status, res.reason);

		for (String key : res.getHeaders())
			setHeader(key, res.getHeader(key));
	}

	public boolean isImageComplete()
	{
		return statusI == JPIPConstants.EOR_IMAGE_DONE;
	}
	
	public boolean isResponseComplete()
	{
		return statusI == JPIPConstants.EOR_WINDOW_DONE || statusI == JPIPConstants.EOR_IMAGE_DONE;
	}
}
