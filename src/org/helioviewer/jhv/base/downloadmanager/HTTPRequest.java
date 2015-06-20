package org.helioviewer.jhv.base.downloadmanager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HTTPRequest extends AbstractRequest {
	private static final int DEFAULT_TIMEOUT = 10000;

	private static final int DEFAULT_BUFFER_SIZE = 16384;

	public String uri;

	private byte[] rawData;

	public HTTPRequest(String uri, PRIORITY priority, int timeOut, int retries){
		this(uri, priority);
		this.retries = retries;
		this.timeOut = timeOut;
	}

	public HTTPRequest(String uri, PRIORITY priority, int retries){
		this(uri, priority);
		this.retries = retries;
	}

	public HTTPRequest(String uri, PRIORITY priority) {
		super(priority);
		System.out.println("connect to : " + uri);
		this.uri = uri;
	}

	public void execute() throws IOException {
			receiveData();
			
	}

	private void receiveData() throws IOException {
		HttpURLConnection httpURLConnection = null;
		InputStream inputStream = null;
		ByteArrayOutputStream byteArrayOutputStream = null;
		URL url;
		url = new URL(this.url);
		httpURLConnection = (HttpURLConnection) url.openConnection();
		httpURLConnection.setReadTimeout(timeOut);
		httpURLConnection.setRequestMethod("GET");
		httpURLConnection.connect();
		int response = httpURLConnection.getResponseCode();

		if (response == HttpURLConnection.HTTP_OK) {
			inputStream = httpURLConnection.getInputStream();
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
		finished = true;
	}

	public String getDataAsString() throws IOException {
		return new String(getData(),StandardCharsets.UTF_8);
	}

	public byte[] getData() throws IOException {
		if (ioException != null) throw ioException;
		return rawData;
	}
}
