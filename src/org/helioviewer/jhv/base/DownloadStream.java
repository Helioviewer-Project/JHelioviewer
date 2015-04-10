package org.helioviewer.jhv.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

/**
 * General connection class which gives to a given URL a proper InputStream with
 * the response back, trying to use compressed transmission if the server
 * support it and hopefully as many protocols as needed.
 * 
 * To use it: - Create a new DownloadStream object - Add post data with
 * .setOutput() - Connect with the current parameters .connect(), automatically
 * done if used getInput() - Get input stream .getInput() - Get output filename
 * .getOutputName()
 * 
 * To save data @see UploadStream
 * 
 * @author Helge Dietert
 */
public class DownloadStream {
    /**
     * Used pattern to extract from content-disposition the filename, group 1
     * must be the filename
     */
    final private Pattern findFilename = Pattern.compile("filename=\\\"(.*?)\\\"");
    /**
     * Input stream to read the data from
     */
    private InputStream in = null;
    /**
     * Output to send as a post request
     */
    private String output = null;
    /**
     * Suggested name to save (if wanted)
     */
    private String outputName = null;
    /**
     * Read timeout in ms
     */
    private int readTimeout;
    /**
     * Connect timeout in ms
     */
    private int connectTimeout;
    /**
     * Used url to connect
     */
    final private URL url;

    /**
     * Creates d download object for a given uri, assuming a file if not given a
     * scheme
     * 
     * @param uri
     *            The used uri to connect
     * @throws URISyntaxException
     *             if the uri is malformed
     * @throws MalformedURLException
     *             if the url is malformed
     */
    public DownloadStream(URI uri, int connectTimeout, int readTimeout) throws URISyntaxException, MalformedURLException {
        if (!uri.isAbsolute()) {
            uri = new URI("file:" + uri.toString());
        }
        url = uri.toURL();
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Creates a downloadstream with the given url
     * 
     * @param url
     *            The url to connect to
     */
    public DownloadStream(URL url, int connectTimeout, int readTimeout) {
        this.url = url;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Opens the connection with compression if the server supports
     * 
     * @throws IOException
     *             From accessing the network
     */
    public void connect() throws IOException {
        System.out.println("Connect to " + url);
        URLConnection connection = url.openConnection();
        // Set timeouts
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        // Try to get a better input stream supporting compression if using http
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpC = (HttpURLConnection) connection;
            // get compression if supported
            httpC.setRequestProperty("Accept-Encoding", "gzip, deflate");
            // Write post data if necessary
            if (output != null) {
                connection.setDoOutput(true);
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                out.write(output);
                out.close();
            }
            httpC.connect();
            // Check the connection code
            if (httpC.getResponseCode() >= 400) {
                System.err.println(">> DownloadStream.connect() > Error opening http connection to " + url + " Response code: " + httpC.getResponseCode());
                throw new IOException("Error opening http connection to " + url + " Response code: " + httpC.getResponseCode());
            }
            // Now according to the encoding open the right input Stream
            String encoding = httpC.getContentEncoding();
            System.out.println("Created http connection with encoding " + encoding);
            if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                in = new GZIPInputStream(httpC.getInputStream());
            } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
                in = new InflaterInputStream(httpC.getInputStream());
            } else {
                in = httpC.getInputStream();
            }
        } else {
            System.out.println("No http connection, try no compression");
            // Write post data if necessary
            if (output != null) {
                connection.setDoOutput(true);
                OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
                out.write(output);
                out.close();
            }
            // Okay just normal
            in = connection.getInputStream();
        }
        // Setting the default output name
        outputName = url.getFile().replace("/", "-");
        String disposition = connection.getHeaderField("Content-Disposition");
        if (disposition != null) {
            Matcher m = findFilename.matcher(disposition);
            if (m.find()) {
                outputName = m.group(1);
            }
        }
    }

    /**
     * Gives the outstream to read the response, after calling connect. If it is
     * not already connected it will automatically connect
     * 
     * @return output stream of the connection
     * @throws IOException
     *             Error from creating the connction
     */
    public InputStream getInput() throws IOException {
        if (in == null)
            connect();
        return in;
    }

    /**
     * After requesting the data the associated file name to save from
     * Content-Disposition or the url name
     * 
     * @return suggested download name
     */
    public String getOutputName() {
        return outputName;
    }

    /**
     * @return the read timeout
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * @return the connect timeout
     */
    public int getConnectTimeout() {
        return readTimeout;
    }

    /**
     * Set the output to send to the server (in HTTP as POST)
     * 
     * @param output
     *            Send output to the server, null if nothing (GET in HTTP)
     */
    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * @param timeout
     *            the timeout to set
     */
    public void setReadTimeout(int timeout) {
        this.readTimeout = timeout;
    }

    /**
     * @param timeout
     *            the timeout to set
     */
    public void setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
    }
}