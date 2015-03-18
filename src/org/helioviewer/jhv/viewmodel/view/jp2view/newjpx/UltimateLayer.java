package org.helioviewer.jhv.viewmodel.view.jp2view.newjpx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.text.DateFormatter;

public class UltimateLayer {
	
	public String observatory;
	public String instrument;
	public String measurement1;
	public String measurement2;
	public int sourceID;
	
	public NewReader reader;
	
	private static final int MAX_FRAME_SIZE = 10;
	private static final String URL = "http://api.helioviewer.org/v2/getJPX/?";
	
	private NewCache cache;
	
	public UltimateLayer(int sourceID, NewCache cache){
		this.sourceID = sourceID;
		this.cache = cache;
	}
	
	public UltimateLayer(String observatory, String instrument, String measurement1, String measurement2) {
		this.observatory = observatory;
		this.instrument = instrument;
		this.measurement1 = measurement1;
		this.measurement2 = measurement2;
	}	
	
	public void setTimeRange(LocalDateTime start, LocalDateTime end, int cadence){
		LocalDateTime tmp = LocalDateTime.MIN;
		while (tmp.isBefore(end)){			
			tmp = start.plusSeconds(cadence*MAX_FRAME_SIZE);
			StringBuilder sb = new StringBuilder();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			System.out.println("tmp : " + tmp);
			System.out.println("start : " + start);
			System.out.println("end : " + end);
			String request = "startTime="+start.format(formatter)+"&endTime="+tmp.format(formatter)+"&sourceId="+sourceID+"&jpip=true";

			System.out.println(URL+request);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(URL+request).openStream()))){
				String line = null;
				
				while((line = in.readLine()) != null)
				{
					sb.append(line);
				}
				String jpipURL = sb.toString();
				NewReader reader = new NewReader(jpipURL, sourceID);
				reader.getData();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			start = tmp;
		}
	}
	
	
	public void getImageData(){		
	}
	
	public static void main(String[] args) {
		LocalDateTime start = LocalDateTime.of(2014, 01, 01, 0, 0, 0);
		LocalDateTime end = LocalDateTime.of(2014, 01, 01, 0, 45, 0);
		UltimateLayer ultimateLayer = new UltimateLayer(10, new NewCache());
		ultimateLayer.setTimeRange(start, end, 10);
	}

}
