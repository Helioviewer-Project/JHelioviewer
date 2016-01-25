package org.helioviewer.jhv.viewmodel.jp2view.io.http;

import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * The class <code>HTTPMessage</code> defines the basic body of a HTTP message.
 */
public abstract class HTTPMessage
{
	/** A hash table with the headers of the message */
	private Hashtable<String, String> headers = new Hashtable<>();

	/**
	 * Returns the value of a message header.
	 * 
	 * @param key
	 *            The header name.
	 * @return The value of the specified header or <code>null</code> if it was
	 *         not found.
	 */
	public final @Nullable String getHeader(String key)
	{
		return headers.get(key);
	}

	/**
	 * Sets a new value for a specific HTTP message header. If the header does
	 * not exists, it will be added to the header list of the message.
	 * 
	 * @param key
	 *            Header name.
	 * @param val
	 *            Header value.
	 */
	public final void setHeader(String key, String val)
	{
		headers.put(key, val);
	}

	/**
	 * Same as setHeader except for a map of headers.
	 * 
	 * @param _map
	 */
	public final void setHeaders(Map<String, String> _map)
	{
		headers.putAll(_map);
	}

	/**
	 * Returns a <code>Set<String></code> with all the headers keys. This set
	 * backs the headers, so don't change anything.
	 */
	public final Set<String> getHeaders()
	{
		return headers.keySet();
	}

	/**
	 * Tests to see if a header exists.
	 * 
	 * @param _key
	 * @return True, if the header exists, false otherwise
	 */
	public final boolean headerExists(String _key)
	{
		return headers.containsKey(_key);
	}
}
