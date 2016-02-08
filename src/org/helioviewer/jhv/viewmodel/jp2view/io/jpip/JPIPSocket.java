package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.apache.http.client.entity.DeflateInputStream;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.viewmodel.jp2view.io.ChunkedInputStream;
import org.helioviewer.jhv.viewmodel.jp2view.io.FixedSizedInputStream;
import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPResponse;
import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPSocket;

/**
 * Assumes a persistent HTTP connection.
 */
public class JPIPSocket extends HTTPSocket
{
	/**
	 * The jpip channel ID for the connection (persistent)
	 */
	private @Nullable String jpipChannelID;

	/**
	 * The path supplied on the uri line of the HTTP message. Generally for the
	 * first request it is the image path in relative terms, but the response
	 * could change it. The Kakadu server seems to change it to /jpip.
	 */
	private @Nullable String jpipPath;

	/** Amount of data (bytes) of the last response */
	private int receivedData = 0;

	/** Time when received the last reply text */
	private long replyTextTm = 0;

	/** Time when received the last reply data */
	private long replyDataTm = 0;

	public JPIPSocket(int _timeout)
	{
		super(_timeout);
	}

	/**
	 * Connects to the specified URI. The second parameter only serves to
	 * distinguish it from the super classes connect method (I want to return
	 * something and the super class has a return type of void).
	 * 
	 * @param _uri
	 * @return The first response of the server when connecting.
	 * @throws IOException
	 */
	public Object connect(URI _uri) throws IOException
	{
		super.connect(_uri);

		jpipPath = _uri.getPath();


		JPIPQuery query = new JPIPQuery();
		query.setField(JPIPRequestField.CNEW.toString(), "http");
		query.setField(JPIPRequestField.TYPE.toString(), "jpp-stream");
		query.setField(JPIPRequestField.TID.toString(), "0");
		
		JPIPRequest req = new JPIPRequest(query.toString());

		JPIPResponse res = null;

		while (isConnected() && (res == null))
		{
			send(req);
			res = receive();
		}

		if (res == null)
			throw new IOException("After conncting to the server, it did not send a response.");

		HashMap<String, String> map;
		String[] cnewParams = { "cid", "transport", "host", "path", "port", "auxport" };
		if (res.getHeader("JPIP-cnew") != null)
		{
			map = new HashMap<>();
			String[] parts = res.getHeader("JPIP-cnew").split(",");
			for (String part : parts)
				for (String cnewParam : cnewParams)
					if (part.startsWith(cnewParam + "="))
						map.put(cnewParam, part.substring(cnewParam.length() + 1));
		}
		else
			throw new IOException("The header 'JPIP-cnew' was not sent by the server!");

		jpipChannelID = map.get("cid");

		jpipPath = "/" + map.get("path");

		if (jpipChannelID == null)
			throw new IOException("The channel id was not sent by the server");

		if (map.get("transport") == null || !map.get("transport").equals("http"))
			throw new IOException("The client currently only supports http transport.");

		return res;

	}

	/** Closes the JPIPSocket */
	public void close() throws IOException
	{
		if (this.isClosed())
			return;

		try
		{
			if (jpipChannelID != null)
			{
				JPIPQuery query = new JPIPQuery();
				query.setField(JPIPRequestField.CCLOSE.toString(), jpipChannelID);
				query.setField(JPIPRequestField.LEN.toString(), "0");
				
				JPIPRequest req = new JPIPRequest(query.toString());
				send(req);
			}

		}
		catch (IOException e)
		{
			Telemetry.trackException(e);
		}
		finally
		{
			super.close();
		}
	}

	/**
	 * Sends a JPIPRequest
	 * 
	 * @param _req
	 * @throws IOException
	 */
	public void send(JPIPRequest _req) throws IOException
	{
		String queryStr = _req.query;

		// Adds some default headers if they were not already added.
		if (!_req.headerExists("Cache-Control"))
			_req.setHeader("Cache-Control", "no-cache");
		if (!_req.headerExists("Accept-Encoding"))
			_req.setHeader("Accept-Encoding", "gzip, deflate");
		if (!_req.headerExists("Host"))
			_req.setHeader("Host", getHost() + ":" + getPort());
		// Adds a necessary JPIP request field
		if ((!queryStr.contains("cid=")) && (!queryStr.contains("cclose")) && jpipChannelID != null)
			queryStr += "&cid=" + jpipChannelID;

		//if (_req.method == Method.GET)
		{
			if (!_req.headerExists("Connection"))
				_req.setHeader("Connection", "Keep-Alive");
		}
		
		//WAS NEVER SUPPORTED AND NEVER USED (therefore never tested)
		/*else if (_req.method == Method.POST)
		{
			if (!_req.headerExists("Content-Type"))
				_req.setHeader("Content-Type", "application/x-www-form-urlencoded");
			if (!_req.headerExists("Content-Length"))
				_req.setHeader("Content-Length", String.valueOf(queryStr.getBytes(StandardCharsets.UTF_8).length));
		}*/

		StringBuilder str = new StringBuilder();

		// Adds the URI line.
		str.append("GET ");
		str.append(jpipPath+"?"+queryStr);
		str.append(" ");
		str.append(versionText).append(CRLF);

		// Adds the headers
		for (String key : _req.getHeaders())
			str.append(key).append(": ").append(_req.getHeader(key)).append(CRLF);
		str.append(CRLF);

		
		//WAS NEVER USED, NEVER TESTED
		/*
		// Adds the message body if necessary.
		if (_req.method == HTTPRequest.Method.POST)
			str.append(queryStr);
		*/
		
		if (!isConnected())
			reconnect();

		// Writes the result to the output stream.
		getOutputStream().write(str.toString().getBytes(StandardCharsets.UTF_8));
	}

	private String getResponseHeadersAsString(HTTPResponse res)
	{
		StringBuilder result = new StringBuilder("Headers:");
		if (res != null && res.getHeaders() != null)
			for (String header : res.getHeaders())
				result.append("\n").append(header).append(": ").append(res.getHeader(header));

		return result.toString();
	}

	/** Receives a JPIPResponse returning null if EOS reached */
	@SuppressWarnings({ "resource" })
	public @Nullable JPIPResponse receive() throws IOException
	{
		HTTPResponse httpRes = (HTTPResponse) super.receive();
		if (httpRes == null)
			return null;

		JPIPResponse res = new JPIPResponse(httpRes);
		InputStream input = new BufferedInputStream(getInputStream(),65536);
		
		String contentEncoding = res.getHeader("Content-Encoding");
		//System.out.println("Content encoding JPIP: "+contentEncoding);
		if(contentEncoding!=null)
			switch(contentEncoding.toLowerCase())
			{
				case "gzip":
					input=new GZIPInputStream(input,8192);
					break;
				case "deflate":
					input=new DeflateInputStream(input);
					break;
				default:
					throw new IOException("Unknown encoding: "+contentEncoding);
			}

		if (res.status != 200)
		{
			byte[] buf = new byte[8192];
			try (InputStream is = input)
			{
				int off = 0;
				for (;;)
				{
					int read = is.read(buf, off, buf.length - off);
					if (read == -1 || off == buf.length)
						break;
					off += read;
				}
			}
			catch (Exception _e)
			{
				Telemetry.trackException(_e);
			}

			throw new IOException("Invalid status code returned (" + res.status + ") " + res.reason + "\n"
					+ new String(buf, StandardCharsets.UTF_8).trim());
		}
		
		if (res.getHeader("Content-Type") != null && !"image/jpp-stream".equals(res.getHeader("Content-Type")))
			throw new IOException("Expected image/jpp-stream content!\n" + getResponseHeadersAsString(res));

		String transferEncoding = res.getHeader("Transfer-Encoding") == null ? "" : res.getHeader("Transfer-Encoding").trim();
		switch (transferEncoding)
		{
			case "":
			case "identity":
				String contentLengthString = res.getHeader("Content-Length") == null ? "" : res.getHeader("Content-Length").trim();
				try
				{
					int contentLength = Integer.parseInt(contentLengthString);
					input = new FixedSizedInputStream(input, contentLength);
				}
				catch (NumberFormatException _nfe)
				{
					throw new IOException("Invalid Content-Length header: " + contentLengthString + "\n"
							+ getResponseHeadersAsString(res));
				}
				break;
			case "chunked":
				input = new ChunkedInputStream(input);
				break;
			default:
				throw new IOException(
						"Unsupported transfer encoding: " + transferEncoding + "\n" + getResponseHeadersAsString(res));
		}

		replyTextTm = System.currentTimeMillis();

		JPIPDataInputStream jpip = new JPIPDataInputStream(input);

		JPIPDataSegment seg;
		while ((seg = jpip.readSegment()) != null)
			res.addJpipDataSegment(seg);

		if (res.getHeader("Connection") != null && "close".equals(res.getHeader("Connection")))
			super.close();
		
		replyDataTm = System.currentTimeMillis();
		receivedData = jpip.getNumberOfBytesRead();
		// System.out.format("Bandwidth: %.2f KB/seg.\n", (double)(receivedData
		// * 1.0) / (double)(replyDataTm - tini));

		return res;
	}

	/**
	 * Returns the time when received the last reply text
	 */
	public long getReplyTextTime()
	{
		return replyTextTm;
	}

	/**
	 * Returns the time when received the last reply data
	 */
	public long getReplyDataTime()
	{
		return replyDataTm;
	}

	/**
	 * Returns the amount of data (bytes) of the last response.
	 */
	public int getReceivedData()
	{
		return receivedData;
	}

}