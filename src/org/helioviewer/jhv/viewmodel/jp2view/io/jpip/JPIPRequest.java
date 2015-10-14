package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import javax.annotation.Nullable;

import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPRequest;

/**
 * A glorified HTTP request object.
 */
public class JPIPRequest extends HTTPRequest
{
	private @Nullable String query = null;

	/**
	 * Default constructor.
	 * 
	 * @param _method
	 */
	public JPIPRequest(Method _method)
	{
		super(_method);
	}

	/**
	 * Gets the query string.
	 * 
	 * @return Query String
	 */
	public @Nullable String getQuery()
	{
		return query;
	}

	/**
	 * Sets the query string.
	 * 
	 * @param _query
	 */
	public void setQuery(Object _query)
	{
		query = _query.toString();
	}
}
