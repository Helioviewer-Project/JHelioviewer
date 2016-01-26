package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPResponse;

/**
 * A response to a JPIPRequest. Encapsulates both the HTTPResponse headers and
 * the JPIPDataSegments.
 */
public class JPIPResponse extends HTTPResponse
{
	/** The status... can be EOR_WINDOW_DONE or EOR_IMAGE_DONE */
	private long statusI;

	/** A list of the data segments. */
	private ArrayList<JPIPDataSegment> jpipDataList;

	/**
	 * Used to form responses.
	 * 
	 * @param res
	 * @throws IOException
	 */
	@SuppressWarnings("null")
	public JPIPResponse(HTTPResponse res) throws IOException
	{
		super(res.status, res.reason);

		for (String key : res.getHeaders())
			setHeader(key, res.getHeader(key));
		
		statusI = -1;
		jpipDataList = new ArrayList<>();
	}

	public void addJpipDataSegment(JPIPDataSegment data)
	{
		if (data.isEOR)
			statusI = data.binID;
		
		jpipDataList.add(data);
	}

	public @Nullable JPIPDataSegment removeJpipDataSegment()
	{
		return (jpipDataList.isEmpty() ? null : jpipDataList.remove(0));
	}

	public long getResponseSize()
	{
		long size = 0;
		for (JPIPDataSegment aJpipDataList : jpipDataList)
			size += aJpipDataList.length;
		return size;
	}

	public boolean isResponseComplete()
	{
		return statusI == JPIPConstants.EOR_WINDOW_DONE || statusI == JPIPConstants.EOR_IMAGE_DONE;
	}
}
