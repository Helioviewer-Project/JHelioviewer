package org.helioviewer.jhv.gui.components.newComponents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.json.JSONException;
import org.json.JSONObject;

public class InstrumentModel {

	private final String URL_DATASOURCE = "http://api.helioviewer.org/v2/getDataSources/?";
	private LinkedHashMap<String, Observatory> observatories = new LinkedHashMap<String, InstrumentModel.Observatory>();
	
	public void initModel(){
		StringBuilder sb = new StringBuilder();
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(URL_DATASOURCE).openStream()))){
			String line = null;
			
			while((line = in.readLine()) != null)
			{
				sb.append(line);
			}

			JSONObject jsonObject = new JSONObject(sb.toString());
			addObservatories(jsonObject);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addObservatories(JSONObject observatories){
		Iterator<String> iterator = observatories.sortedKeys();
		while(iterator.hasNext()){
			String observatoryName = iterator.next();
			Observatory observatory = new Observatory(observatoryName);
			try {
				JSONObject jsonObservatory = observatories.getJSONObject(observatoryName);
				addInstrument(jsonObservatory, observatory);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void addInstrument(JSONObject jsonObservatory, Observatory observatory){
		Iterator<String> iterator = jsonObservatory.sortedKeys();
		while(iterator.hasNext()){
			String instrumentName = iterator.next();
			Instrument instrument = new Instrument(instrumentName);
			observatory.addInstrument(instrumentName, instrument);
			try {
				JSONObject jsonInstrument = jsonObservatory.getJSONObject(instrumentName);
				addDetector(jsonInstrument, instrument);
			} catch (JSONException e) {
				System.out.println("not avaible instrument : " + instrumentName);
			}
		}
	}
	
	private void addDetector(JSONObject jsonInstrument, Instrument instrument){
		Iterator<String> iterator = jsonInstrument.sortedKeys();
		while (iterator.hasNext()) {
			String detectorName = iterator.next();
			Detector detector = new Detector(detectorName);
			instrument.addDetector(detectorName, detector);
			try {				
				JSONObject jsonDetector = jsonInstrument.getJSONObject(detectorName);
				addDetector(jsonDetector, detector);
			} catch (Exception e) {
				System.out.println("not avaible detector : " + detectorName);
			}
		}
	}
	
	private void addDetector(JSONObject jsonDetector, Detector detector){
		Iterator<String> iterator = jsonDetector.sortedKeys();
		while (iterator.hasNext()) {
			String detectorName = iterator.next();
			try {
				JSONObject jsonDetector1 = jsonDetector.getJSONObject(detectorName);
				Detector detector1 = new Detector(detectorName);
				detector.addDetector(detectorName, detector1);
				addData(jsonDetector1, detector1);
			} catch (Exception e) {
				addData(jsonDetector, detector);
				return;
			}
		}
	}
	
	private void addData(JSONObject jsonObject, Detector detector){
		try {
			Object end = jsonObject.get("end");
			Object start = jsonObject.get("start");
			if (start != null && end != null){
				detector.hasDates = true;
			}
			detector.layeringOrder = Integer.parseInt((String) jsonObject.getString("layeringOrder"));
			detector.nickname = (String) jsonObject.get("nickname");
			detector.sourceId = Integer.parseInt((String) jsonObject.getString("sourceId"));
			JSONObject uiLabels = (JSONObject) jsonObject.get("uiLabels");
			Iterator<String> iterator = uiLabels.keys();
			while (iterator.hasNext()) {
				String key = iterator.next();
				String name = uiLabels.getString(key);
				detector.uiLabels.put(key, name);
			}
		} catch (JSONException e) {
		}
	}
	
	public class Observatory {
		LinkedHashMap<String, Instrument> instruments = new LinkedHashMap<String, InstrumentModel.Instrument>(); 
		private String name;
		
		public Observatory(String name) {
			this.name = name;
		}
		
		public void addInstrument(String name, Instrument instrument){
			instruments.put(name, instrument);
		}
	}
	
	public class Instrument{
		private LinkedHashMap<String, Detector> detectors = new LinkedHashMap<String, InstrumentModel.Detector>();
		private String name;
		
		public Instrument(String name) {
			this.name = name;
		}
		
		public void addDetector(String name, Detector detector){
			detectors.put(name, detector);
		}
	}
		
	public class Detector{
		private LinkedHashMap<String, Detector> detectors = new LinkedHashMap<String, InstrumentModel.Detector>();
		private String name;
		
		public Boolean hasDates = false;
		public Date start;
		public Date end;
		public int layeringOrder;
		public String nickname;
		public int sourceId;
		public HashMap<String, String> uiLabels = new HashMap<String, String>();
		
		public Detector(String name) {
			this.name = name;
		}

		public void addDetector(String name, Detector detector){
			detectors.put(name, detector);
		}
		
		

	}
	
	public static void main(String[] args) {
		new InstrumentModel().initModel();
	}
	
}
