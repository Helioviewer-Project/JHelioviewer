package org.helioviewer.jhv.viewmodel.jp2view.io.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.helioviewer.jhv.viewmodel.jp2view.io.LineReader;

/**
 * The class <code>HTTPSocket</code> is a simple implementation for read/write
 * HTTP messages. In this version are only supported to send requests and to
 * receive responses.
 */
public class HTTPSocket extends Socket
{
	/** The last used port */
	private int lastUsedPort = 0;

	/** The last used host */
	private String lastUsedHost = null;

	/** The default port for the HTTP socket */
	static private final int PORT = 80;

	/** The maximum HTTP version supported */
	static private final double version = 1.1;

	/** The version in standard formated text */
	static public final String versionText = "HTTP/" + Double.toString(version);

	/** The array of bytes that contains the CRLF codes */
	static private final byte CRLFBytes[] = { 13, 10 };

	/** The string representation of the CRLF codes */
	static public final String CRLF = new String(CRLFBytes, StandardCharsets.UTF_8);

	/** Default constructor */
	public HTTPSocket()
	{
		super();
	}

	/**
	 * Connects to the specified host via the supplied URI.
	 * 
	 * @param _uri
	 * @throws IOException
	 */
	public Object connect(URI _uri) throws IOException
	{
		lastUsedPort = _uri.getPort() <= 0 ? PORT : _uri.getPort();
		lastUsedHost = _uri.getHost();
		super.setSoTimeout(10000);
		super.setKeepAlive(true);
		super.connect(new InetSocketAddress(lastUsedHost, lastUsedPort), 10000);
		return null;
	}

	/**
	 * Reconnects to the last used host, and using the last used port.
	 * 
	 * @throws java.io.IOException
	 */
	public void reconnect() throws IOException
	{
		super.connect(new InetSocketAddress(lastUsedHost, lastUsedPort), 10000);
	}

	/**
	 * Receives a HTTP message from the socket. Currently it is only supported
	 * to receive HTTP responses.
	 * 
	 * @return A new <code>HTTPMessage</code> object with the message read or
	 *         <code>null</code> if the end of stream was reached.
	 * @throws java.io.IOException
	 */
	public HTTPMessage receive() throws IOException
	{
		int code;
		double ver;

		InputStream input = getInputStream();
		String line = LineReader.readLine(input);
		if (line == null)
			return null;

		String[] parts = line.split(" ", 3);

		if (parts.length != 3)
			throw new ProtocolException("Invalid HTTP message.");

		if (parts[0].startsWith("HTTP/"))
		{

			// Parses HTTP version
			try
			{
				ver = Double.parseDouble(parts[0].substring(5));
			}
			catch (NumberFormatException ex)
			{
				throw new ProtocolException("Invalid HTTP version format");
			}

			if ((ver < 1) || (ver > version))
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
		else
		{
			throw new ProtocolException("Requests receiving not yet supported!");
		}
	}

	/** Returns the lastUsedPort */
	public int getPort()
	{
		return lastUsedPort;
	}

	/** Returns the lastUsedHost */
	public String getHost()
	{
		return lastUsedHost;
	}

};
