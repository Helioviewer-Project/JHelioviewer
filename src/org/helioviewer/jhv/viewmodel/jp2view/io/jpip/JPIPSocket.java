package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.viewmodel.jp2view.io.ChunkedInputStream;
import org.helioviewer.jhv.viewmodel.jp2view.io.FixedSizedInputStream;
import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPHeaderKey;
import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPRequest;
import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPRequest.Method;
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
    private String jpipChannelID;

    /**
     * The path supplied on the uri line of the HTTP message. Generally for the
     * first request it is the image path in relative terms, but the response
     * could change it. The Kakadu server seems to change it to /jpip.
     */
    private String jpipPath;

    /** Amount of data (bytes) of the last response */
    private int receivedData = 0;

    /** Time when received the last reply text */
    private long replyTextTm = 0;

    /** Time when received the last reply data */
    private long replyDataTm = 0;

    /** Default constructor. */
    public JPIPSocket() {
        super();
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
    public Object connect(URI _uri) throws IOException {
        super.connect(_uri);

        jpipPath = _uri.getPath();

        JPIPRequest req = new JPIPRequest(HTTPRequest.Method.GET);

        JPIPQuery query = new JPIPQuery();
        query.setField(JPIPRequestField.CNEW.toString(), "http");
        query.setField(JPIPRequestField.TYPE.toString(), "jpp-stream");
        query.setField(JPIPRequestField.TID.toString(), "0");
        req.setQuery(query.toString());

        JPIPResponse res = null;

        while (isConnected() && (res == null)) {
            send(req);
            res = receive();
        }

        if (res == null)
            throw new IOException("After conncting to the server, it did not send a response.");

        HashMap<String, String> map = null;
        String[] cnewParams = { "cid", "transport", "host", "path", "port", "auxport" };
        if (res.getHeader("JPIP-cnew") != null) {
            map = new HashMap<String, String>();
            String[] parts = res.getHeader("JPIP-cnew").split(",");
            for (int i = 0; i < parts.length; i++)
                for (int j = 0; j < cnewParams.length; j++)
                    if (parts[i].startsWith(cnewParams[j] + "="))
                        map.put(cnewParams[j], parts[i].substring(cnewParams[j].length() + 1));
        }

        if (map == null)
            throw new IOException("The header 'JPIP-cnew' was not sent by the server!");

        jpipChannelID = map.get("cid");

        jpipPath = "/" + map.get("path");

        if (jpipChannelID == null)
            throw new IOException("The channel id was not sent by the server");

        if (map.get("transport") == null || !map.get("transport").equals("http"))
            throw new IOException("The client currently only supports http transport.");

        return res;

    };

    /** Closes the JPIPSocket */
    public void close() throws IOException {
        if (this.isClosed())
            return;

        try {
            if (jpipChannelID != null) {
                JPIPRequest req = new JPIPRequest(HTTPRequest.Method.GET);

                JPIPQuery query = new JPIPQuery();
                query.setField(JPIPRequestField.CCLOSE.toString(), jpipChannelID);
                query.setField(JPIPRequestField.LEN.toString(), "0");
                req.setQuery(query.toString());

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
    public void send(JPIPRequest _req) throws IOException {
        String queryStr = _req.getQuery();

        // Adds some default headers if they were not already added.
        if (!_req.headerExists(HTTPHeaderKey.CACHE_CONTROL.toString()))
            _req.setHeader(HTTPHeaderKey.CACHE_CONTROL.toString(), "no-cache");
        if (!_req.headerExists(HTTPHeaderKey.HOST.toString()))
            _req.setHeader(HTTPHeaderKey.HOST.toString(), (getHost() + ":" + getPort()));
        // Adds a necessary JPIP request field
        if ((queryStr.indexOf("cid=") == -1) && (queryStr.indexOf("cclose") == -1) && jpipChannelID != null)
            queryStr += "&cid=" + jpipChannelID;

        if (_req.getMethod() == Method.GET) {
            if (!_req.headerExists(HTTPHeaderKey.CONNECTION.toString()))
                _req.setHeader(HTTPHeaderKey.CONNECTION.toString(), "Keep-Alive");
        } else if (_req.getMethod() == Method.POST) {
            if (!_req.headerExists(HTTPHeaderKey.CONTENT_TYPE.toString()))
                _req.setHeader(HTTPHeaderKey.CONTENT_TYPE.toString(), "application/x-www-form-urlencoded");
            if (!_req.headerExists(HTTPHeaderKey.CONTENT_LENGTH.toString()))
                _req.setHeader(HTTPHeaderKey.CONTENT_LENGTH.toString(), String.valueOf(queryStr.getBytes(StandardCharsets.UTF_8).length));
        }

        StringBuilder str = new StringBuilder();

        // Adds the URI line.
        str.append(_req.getMethod() + " ");
        str.append(jpipPath);
        if (_req.getMethod() == Method.GET)
            str.append("?" + queryStr);
        str.append(" ");
        str.append(versionText + CRLF);

        // Adds the headers
        for (String key : _req.getHeaders())
            str.append(key + ": " + _req.getHeader(key) + CRLF);
        str.append(CRLF);

        // Adds the message body if necessary.
        if (_req.getMethod() == HTTPRequest.Method.POST)
            str.append(queryStr);

        if (!isConnected())
            reconnect();
        
        // Writes the result to the output stream.
        getOutputStream().write(str.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String getResponseHeadersAsString(HTTPResponse res)
    {
    	StringBuffer result = new StringBuffer("Headers:");
        if (res != null && res.getHeaders() != null)
            for (String header : res.getHeaders())
                result.append("\n" + header + ": " + res.getHeader(header));
        
        return result.toString();
    }

    /** Receives a JPIPResponse returning null if EOS reached */
    public JPIPResponse receive() throws IOException
    {
        // long tini = System.currentTimeMillis();

        HTTPResponse httpRes = (HTTPResponse) super.receive();
        if (httpRes == null)
            return null;

        JPIPResponse res = new JPIPResponse(httpRes);
        InputStream input;

        if (res.getCode() != 200)
        {
            byte[] buf=new byte[8192];
            try(InputStream is = getInputStream())
            {
                int off=0;
                for(;;)
                {
                    int read=is.read(buf,off,buf.length-off);
                    if(read==-1 || off==buf.length)
                        break;
                    off+=read;
                }
            }
            catch(Exception _e)
            {
            }
            
            throw new IOException("Invalid status code returned (" + res.getCode() + ") " + res.getReason() + "\n" + new String(buf,StandardCharsets.UTF_8).trim());
        }
        if (res.getHeader("Content-Type") != null && !res.getHeader("Content-Type").equals("image/jpp-stream"))
            throw new IOException("Expected image/jpp-stream content!\n" + getResponseHeadersAsString(res));

        String transferEncoding = res.getHeader("Transfer-Encoding") == null ? "" : res.getHeader("Transfer-Encoding").trim();
        if (transferEncoding.equals("") || transferEncoding.equals("identity"))
        {
            String contentLengthString = res.getHeader("Content-Length") == null ? "" : res.getHeader("Content-Length").trim();
            try
            {
                int contentLength = Integer.parseInt(contentLengthString);
                input = new FixedSizedInputStream(new BufferedInputStream(getInputStream(),65536), contentLength);
            }
            catch (NumberFormatException _nfe)
            {
                throw new IOException("Invalid Content-Length header: " + contentLengthString + "\n" + getResponseHeadersAsString(res));
            }
        }
        else if (transferEncoding.equals("chunked"))
        {
            input = new ChunkedInputStream(new BufferedInputStream(getInputStream(),65536));
        }
        else
        {
            throw new IOException("Unsupported transfer encoding: " + transferEncoding + "\n" + getResponseHeadersAsString(res));
        }

        replyTextTm = System.currentTimeMillis();

        JPIPDataInputStream jpip = new JPIPDataInputStream(input);

        JPIPDataSegment seg;

        while ((seg = jpip.readSegment()) != null)
            res.addJpipDataSegment(seg);

        if (res.getHeader("Connection") != null && res.getHeader("Connection").equals("close")) {
            super.close();
        }
        replyDataTm = System.currentTimeMillis();
        receivedData = jpip.getNumberOfBytesRead();
        // System.out.format("Bandwidth: %.2f KB/seg.\n", (double)(receivedData
        // * 1.0) / (double)(replyDataTm - tini));

        return res;
    }

    /** Returns the JPIP channel ID */
    public String getJpipChannelID() {
        return jpipChannelID;
    }

    /**
     * Returns the JPIP path.
     */
    public String getJpipPath() {
        return jpipPath;
    }

    /**
     * Returns the time when received the last reply text
     */
    public long getReplyTextTime() {
        return replyTextTm;
    }

    /**
     * Returns the time when received the last reply data
     */
    public long getReplyDataTime() {
        return replyDataTm;
    }

    /**
     * Returns the amount of data (bytes) of the last response.
     */
    public int getReceivedData() {
        return receivedData;
    }

};