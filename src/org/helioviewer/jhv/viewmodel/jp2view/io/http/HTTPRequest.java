package org.helioviewer.jhv.viewmodel.jp2view.io.http;

/**
 * 
 * The class <code>HTTPRequest</code> identifies a HTTP request. Currently it is
 * only supported the <code>GET</code> request.
 */
public class HTTPRequest extends HTTPMessage
{
	/** An enum identifying the 2 types of HTTPRequests supported. */
	public enum Method
	{
		GET, POST
	}

	/** The request type */
	private Method method;

	/**
	 * Constructs a new HTTP request indicating the request type.
	 */
	public HTTPRequest(Method _method)
	{
		method = _method;
	}

	/** Returns the method of the request. */
	public Method getMethod()
	{
		return method;
	}

	/** This is a request message so this method always returns true. */
	public boolean isRequest()
	{
		return true;
	}
}
