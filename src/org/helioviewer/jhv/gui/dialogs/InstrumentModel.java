package org.helioviewer.jhv.gui.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class InstrumentModel {

	private static final String URL_DATASOURCE = "http://api.helioviewer.org/v2/getDataSources/?";
	private static LinkedHashMap<String, Observatory> observatories = new LinkedHashMap<String, InstrumentModel.Observatory>();
	private static AddLayerPanel addLayerPanel;
	
	static {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				HTTPRequest httpRequest = new HTTPRequest(URL_DATASOURCE, PRIORITY.LOW, AbstractRequest.INFINITE);
				UltimateDownloadManager.addRequest(httpRequest);
				
				while (!httpRequest.isFinished()) {
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				try {
					JSONObject jsonObject;
					try {
						jsonObject = new JSONObject(httpRequest.getDataAsString());
						addObservatories(jsonObject);
						addLayerPanel.updateGUI();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}, "MODEL_LOAD");
		thread.start();
	}
	
	public static Collection<Observatory> getObservatories(){
		return observatories.values();
	}
	
	private static void addObservatories(JSONObject observatories){
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
			InstrumentModel.observatories.put(observatoryName, observatory);
		}
	}
	
	private static void addInstrument(JSONObject jsonObservatory, Observatory observatory){
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
	
	private static void addFilter(JSONObject jsonInstrument, Instrument instrument, Observatory observatory){
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
	
	private static void addFilter(JSONObject jsonFilter, Filter filter, Observatory observatory){
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
	
	private static void addUILabel(JSONObject jsonObject, Observatory observatory){
		JSONArray uiLabels;
		try {
			ArrayList<String> uiLabel = new ArrayList<String>();
			uiLabels = (JSONArray) jsonObject.get("uiLabels");
			for (int i = 1; i < uiLabels.length(); i++){
				JSONObject obj = (JSONObject) uiLabels.get(i);
				uiLabel.add(obj.getString("label"));
				observatory.uiLabels = uiLabel;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void addData(JSONObject jsonObject, Filter filter){
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
	
	static class Observatory {
		private LinkedHashMap<String, Instrument> instruments = new LinkedHashMap<String, InstrumentModel.Instrument>(); 
		private String name;
		private ArrayList<String> uiLabels;
		
		private Observatory(String name) {
			this.name = name;
		}
		
		private void addInstrument(String name, Instrument instrument){
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
	
	static class Instrument{
		private LinkedHashMap<String, Filter> filters = new LinkedHashMap<String, InstrumentModel.Filter>();
		private String name;
		
		private Instrument(String name) {
			this.name = name;
		}
		
		private void addFilter(String name, Filter detector){
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
		
	static class Filter{
		private LinkedHashMap<String, Filter> filters = new LinkedHashMap<String, InstrumentModel.Filter>();
		private String name;
		
		private Boolean hasDates = false;
		private int layeringOrder;
		private String nickname;
		int sourceId;
				
		private Filter(String name) {
			this.name = name;
		}

		private void addFilter(String name, Filter filter){
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

	public static void addAddLayerPanel(AddLayerPanel addLayerPanel) {
		InstrumentModel.addLayerPanel = addLayerPanel;
	}
		
}
