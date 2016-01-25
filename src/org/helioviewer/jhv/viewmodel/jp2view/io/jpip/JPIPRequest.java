package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPMessage;

public class JPIPRequest extends HTTPMessage
{
	public final String query;

	public JPIPRequest(String _query)
	{
		query = _query;
	}
}
