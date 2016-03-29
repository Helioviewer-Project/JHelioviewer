package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import javax.annotation.Nullable;

import org.apache.http.client.entity.DeflateInputStream;
import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.viewmodel.jp2view.io.ChunkedInputStream;
import org.helioviewer.jhv.viewmodel.jp2view.io.FixedSizedInputStream;
import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPResponse;
import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPSocket;

public class JPIPSocket extends HTTPSocket
{
	/**
	 * The path supplied on the uri line of the HTTP message. Generally for the
	 * first request it is the image path in relative terms, but the response
	 * could change it. The Kakadu server seems to change it to /jpip.
	 */
	private @Nullable String jpipPath;

	public JPIPSocket(URI _uri, int _timeout) throws IOException
	{
		super(_uri, _timeout);
	}

	public void close() throws IOException
	{
		if (!isClosed())
			super.close();
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
	public @Nullable JPIPResponse send(String _query, Consumer<JPIPDataSegment> _callback) throws IOException
	{
		StringBuilder str = new StringBuilder();

		// Adds the URI line.
		str.append("GET "+uri.getPath()+"?"+_query);
		str.append(" "+VERSION_TEXT+CRLF);

		str.append("Cache-Control: no-cache"+CRLF);
		str.append("Accept-Encoding: gzip, deflate"+CRLF);
		str.append("Host: "+host+":"+port+CRLF);
		str.append(CRLF);

		try
		{
			connect(new InetSocketAddress(host, port), timeout);
	
			getOutputStream().write(str.toString().getBytes(StandardCharsets.UTF_8));
			
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
						off += read;
						if (read == -1 || off == buf.length)
							break;
					}
				}
				catch (Exception _e)
				{
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
	
			JPIPDataInputStream jpip = new JPIPDataInputStream(input);
	
			JPIPDataSegment seg;
			while ((seg = jpip.readSegment()) != null)
				if(seg.isEOR)
					res.statusI = seg.binID;
				else
					_callback.accept(seg);
			
			return res;
		}
		finally
		{
			try
			{
				close();
			}
			catch(Throwable _t)
			{
				Telemetry.trackException(_t);
			}
		}
	}
}