package org.helioviewer.jhv.gui.components.newComponents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class InstrumentModel {

	private final String URL_DATASOURCE = "http://api.helioviewer.org/v2/getDataSources/?";
	private LinkedHashMap<String, Observatory> observatories = new LinkedHashMap<String, InstrumentModel.Observatory>();
	public static InstrumentModel singelton = new InstrumentModel();
	
	private InstrumentModel() {
		this.initModel();
	}
	
	public Collection<Observatory> getObservatories(){
		return observatories.values();
	}
	
	public static void main(String[] args) {
		InstrumentModel instrumentModel = InstrumentModel.singelton;
	}
	
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
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = observatories.sortedKeys();
		while(iterator.hasNext()){
			String observatoryName = iterator.next();
			Observatory observatory = new Observatory(observatoryName);
			try {
				JSONObject jsonObservatory = observatories.getJSONObject(observatoryName);
				addInstrument(jsonObservatory, observatory);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			this.observatories.put(observatoryName, observatory);
		}
	}
	
	public void addInstrument(JSONObject jsonObservatory, Observatory observatory){
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = jsonObservatory.sortedKeys();
		while(iterator.hasNext()){
			String instrumentName = iterator.next();
			Instrument instrument = new Instrument(instrumentName);
			observatory.addInstrument(instrumentName, instrument);
			try {
				JSONObject jsonInstrument = jsonObservatory.getJSONObject(instrumentName);
				addFilter(jsonInstrument, instrument, observatory);
			} catch (JSONException e) {
				addUILabel(jsonObservatory, observatory);
			}
		}
	}
	
	private void addFilter(JSONObject jsonInstrument, Instrument instrument, Observatory observatory){
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = jsonInstrument.sortedKeys();
		while (iterator.hasNext()) {
			String filterName = iterator.next();
			Filter filter = new Filter(filterName);
			instrument.addFilter(filterName, filter);
			try {				
				JSONObject jsonFilter = jsonInstrument.getJSONObject(filterName);
				addFilter(jsonFilter, filter, observatory);
			} catch (Exception e) {
				addUILabel(jsonInstrument, observatory);
			}
		}
	}
	
	private void addFilter(JSONObject jsonFilter, Filter filter, Observatory observatory){
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = jsonFilter.sortedKeys();
		while (iterator.hasNext()) {
			String filterName = iterator.next();
			try {
				JSONObject jsonFilter1 = jsonFilter.getJSONObject(filterName);
				Filter detector1 = new Filter(filterName);
				filter.addFilter(filterName, detector1);
				addFilter(jsonFilter1, detector1, observatory);
			} catch (Exception e) {
				addUILabel(jsonFilter, observatory);
				addData(jsonFilter, filter);
				return;
			}
		}
	}
	
	private void addUILabel(JSONObject jsonObject, Observatory observatory){
		JSONArray uiLabels;
		try {
			ArrayList<String> uiLabel = new ArrayList<String>();
			uiLabels = (JSONArray) jsonObject.get("uiLabels");
			for (int i = 1; i < uiLabels.length(); i++){
				JSONObject obj = (JSONObject) uiLabels.get(i);
				uiLabel.add(obj.getString("label"));
				observatory.uiLabels = uiLabel;
			}
			System.out.println(uiLabels);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private void addData(JSONObject jsonObject, Filter filter){
		try {
			Object end = jsonObject.get("end");
			Object start = jsonObject.get("start");
			if (start != null && end != null){
				filter.hasDates = true;
			}
			filter.layeringOrder = Integer.parseInt((String) jsonObject.getString("layeringOrder"));
			filter.nickname = (String) jsonObject.get("nickname");
			filter.sourceId = Integer.parseInt((String) jsonObject.getString("sourceId"));
		} catch (JSONException e) {
		}
	}
	
	public class Observatory {
		LinkedHashMap<String, Instrument> instruments = new LinkedHashMap<String, InstrumentModel.Instrument>(); 
		private String name;
		private ArrayList<String> uiLabels;
		
		public Observatory(String name) {
			this.name = name;
		}
		
		public void addInstrument(String name, Instrument instrument){
			instruments.put(name, instrument);
		}
		
		public Collection<Instrument> getInstruments(){
			return this.instruments.values();
		}
		
		public ArrayList<String> getUiLabels(){
			return uiLabels;
		}
		
		@Override
		public String toString() {
			return this.name;
		}
	}
	
	public class Instrument{
		private LinkedHashMap<String, Filter> filters = new LinkedHashMap<String, InstrumentModel.Filter>();
		private String name;
		
		public Instrument(String name) {
			this.name = name;
		}
		
		public void addFilter(String name, Filter detector){
			filters.put(name, detector);
		}
		
		public Collection<Filter> getFilters(){
			return this.filters.values();
		}
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return this.name;
		}
	}
		
	public class Filter{
		private LinkedHashMap<String, Filter> filters = new LinkedHashMap<String, InstrumentModel.Filter>();
		private String name;
		
		public Boolean hasDates = false;
		public Date start;
		public Date end;
		public int layeringOrder;
		public String nickname;
		public int sourceId;
		public HashMap<String, String> uiLabels = new HashMap<String, String>();
		
		public Filter(String name) {
			this.name = name;
		}

		public void addFilter(String name, Filter filter){
			filters.put(name, filter);
		}
		
		public Collection<Filter> getFilters(){
			return this.filters.values();
		}
		
		@Override
		public String toString() {
			return this.name;
		}
		
		

	}
		
}
