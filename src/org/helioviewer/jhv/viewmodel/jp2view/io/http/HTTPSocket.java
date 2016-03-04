package org.helioviewer.jhv.viewmodel.jp2view.io.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;

import javax.annotation.Nullable;

import org.helioviewer.jhv.viewmodel.jp2view.io.LineReader;

/**
 * The class <code>HTTPSocket</code> is a simple implementation for read/write
 * HTTP messages. In this version are only supported to send requests and to
 * receive responses.
 */
public class HTTPSocket extends Socket
{
	public final int port;
	public URI uri;
	public final int timeout;
	public final String host;

	/** The default port for the HTTP socket */
	static private final int DEFAULT_PORT = 80;

	/** The maximum HTTP version supported */
	static private final double VERSION = 1.1;
	static public final String VERSION_TEXT = "HTTP/" + Double.toString(VERSION);

	/** The array of bytes that contains the CRLF codes */
	static public final String CRLF = "\r\n";

	public HTTPSocket(URI _uri, int _timeout) throws IOException
	{
		super();
		timeout=_timeout;
		
		uri = _uri;
		port = _uri.getPort() <= 0 ? DEFAULT_PORT : _uri.getPort();
		host = _uri.getHost();
		setSoTimeout(timeout);
		setKeepAlive(false);
		super.connect(new InetSocketAddress(host, port), timeout);
	}

	/**
	 * Reconnects to the last used host, and using the last used port.
	 * 
	 * @throws java.io.IOException
	 */
	protected void reconnect() throws IOException
	{
		super.connect(new InetSocketAddress(host, port), timeout);
	}

	/**
	 * Receives a HTTP message from the socket. Currently it is only supported
	 * to receive HTTP responses.
	 * 
	 * @return A new <code>HTTPMessage</code> object with the message read or
	 *         <code>null</code> if the end of stream was reached.
	 * @throws java.io.IOException
	 */
	public @Nullable HTTPMessage receive() throws IOException
	{
		int code;
		double ver;

		InputStream input = getInputStream();
		String line = LineReader.readLine(input);
		if (line == null)
			return null;

		String[] parts = line.split(" ", 3);

		if (parts.length != 3)
			throw new ProtocolException("Invalid HTTP message: "+line);

		if (!parts[0].startsWith("HTTP/"))
			throw new ProtocolException("Requests receiving not yet supported!");
		
		// Parses HTTP version
		try
		{
			ver = Double.parseDouble(parts[0].substring(5));
		}
		catch (NumberFormatException ex)
		{
			throw new ProtocolException("Invalid HTTP version format");
		}

		if ((ver < 1) || (ver > VERSION))
			throw new ProtocolException("HTTP version not supported");

		// Parses status code
		try
		{
			code = Integer.parseInt(parts[1]);
		}
		catch (NumberFormatException ex)
		{
			throw new ProtocolException("Invalid HTTP status code format");
		}

		// Instantiates new HTTPResponse
		HTTPResponse res = new HTTPResponse(code, parts[2]);

		// Parses HTTP headers
		for (;;)
		{
			line = LineReader.readLine(input);

			if (line == null)
				throw new EOFException("End of stream reached before end of HTTP message");
			else if (line.length() <= 0)
				break;

			parts = line.split(": ", 2);

			if (parts.length != 2)
				throw new ProtocolException("Invalid HTTP header format");

			res.setHeader(parts[0], parts[1]);
		}

		return res;
	}
}
