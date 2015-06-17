package org.helioviewer.jhv.base.downloadmanager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HTTPRequest extends AbstractRequest {
	private static final int DEFAULT_PORT = 80;
	private static final int DEFAULT_TIMEOUT = 10000;

	private static final int DEFAULT_BUFFER_SIZE = 16384;

	public String uri;
	public int port;

	private byte[] rawData;

	public HTTPRequest(String uri, int port, PRIORITY priority) {
		super(priority);
		System.out.println("connect to : " + uri);
		this.uri = uri;
		this.port = port;
	}

	public HTTPRequest(String uri, PRIORITY priority) {
		this(uri, DEFAULT_PORT, priority);
	}

	public void execute() throws IOException {
			receiveData();
			finished = true;
		
	}

	private void receiveData() throws IOException {
		HttpURLConnection httpURLConnection = null;
		InputStream inputStream = null;
		ByteArrayOutputStream byteArrayOutputStream = null;
		URL url;
		url = new URL(uri);
		httpURLConnection = (HttpURLConnection) url.openConnection();
		httpURLConnection.setRequestMethod("GET");
		httpURLConnection.connect();
		int response = httpURLConnection.getResponseCode();

		if (response == HttpURLConnection.HTTP_OK) {
			int contentLength = httpURLConnection.getContentLength();

			inputStream = httpURLConnection.getInputStream();
			int bytesRead = 0;
			int length = 0;
			byteArrayOutputStream = new ByteArrayOutputStream(
					DEFAULT_BUFFER_SIZE);
			byte[] buf = new byte[DEFAULT_BUFFER_SIZE];

			while ((length = inputStream.read(buf)) > 0) {
				byteArrayOutputStream.write(buf, 0, length);
			}
			rawData = byteArrayOutputStream.toByteArray();
			byteArrayOutputStream.close();
		}

		if (inputStream != null) {
			byteArrayOutputStream.close();
			inputStream.close();
			httpURLConnection.disconnect();
		}

	}

	public String getDataAsString() {
		return new String(rawData);
	}

	public byte[] getData() {
		return rawData;
	}

	public static void main(String[] args) {
		HTTPRequest request = new HTTPRequest(
				"http://api.helioviewer.org/v2/getJPX/?startTime=2014-04-04T19:46:37Z&endTime=2014-04-04T21:26:17Z&sourceId=9&jpip=true&verbose=true&cadence=20",
				PRIORITY.HIGH);
		try {
			request.receiveData();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(request.getDataAsString());

	}
}
