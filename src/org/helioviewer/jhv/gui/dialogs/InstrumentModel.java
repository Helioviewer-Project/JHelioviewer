package org.helioviewer.jhv.gui.dialogs;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest;
import org.helioviewer.jhv.base.downloadmanager.AbstractRequest.PRIORITY;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.layers.Layers;
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
				HTTPRequest httpRequest = new HTTPRequest(URL_DATASOURCE,
						PRIORITY.LOW, AbstractRequest.INFINITE);
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
						jsonObject = new JSONObject(
								httpRequest.getDataAsString());
						addObservatories(jsonObject);
						addLayerPanel.updateGUI();
						boolean startUpMovie = Boolean.parseBoolean(Settings
								.getProperty("startup.loadmovie"));
						if (startUpMovie) {
							Filter instrument = observatories.get("SDO").filters.get("AIA").filters.get("171");
							LocalDateTime start = instrument.end.minusDays(1);
							Layers.addLayer(instrument.sourceId, start,
									instrument.end, 1728);
							System.out.println(instrument.start);
						}
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

	public static Collection<Observatory> getObservatories() {
		return observatories.values();
	}

	private static void addObservatories(JSONObject observatories) {
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = observatories.sortedKeys();
		while (iterator.hasNext()) {
			String observatoryName = iterator.next();
			Observatory observatory = new Observatory(observatoryName);
			try {
				JSONObject jsonObservatory = observatories
						.getJSONObject(observatoryName);
				addFilter(jsonObservatory, observatory);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			InstrumentModel.observatories.put(observatoryName, observatory);
		}
	}

	private static void addFilter(JSONObject jsonFilter, Observatory observatory){
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = jsonFilter.sortedKeys();
		while (iterator.hasNext()) {
			String filterName = iterator.next();
			try {
				JSONObject jsonFilter1 = jsonFilter.getJSONObject(filterName);
				Filter detector1 = new Filter(filterName);
				observatory.addFilter(filterName, detector1);
				addFilter(jsonFilter1, detector1, observatory);
			} catch (Exception e) {
				addUILabel(jsonFilter, observatory);
				return;
			}
		}
	}
	
	private static void addFilter(JSONObject jsonFilter, Filter filter,
			Observatory observatory) {
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

	private static void addUILabel(JSONObject jsonObject,
			Observatory observatory) {
		JSONArray uiLabels;
		try {
			ArrayList<String> uiLabel = new ArrayList<String>();
			uiLabels = (JSONArray) jsonObject.get("uiLabels");
			for (int i = 1; i < uiLabels.length(); i++) {
				JSONObject obj = (JSONObject) uiLabels.get(i);
				uiLabel.add(obj.getString("label"));
				observatory.uiLabels = uiLabel;
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void addData(JSONObject jsonObject, Filter filter) {
		try {
			DateTimeFormatter reader = DateTimeFormatter
					.ofPattern("yyyy-MM-dd HH:mm:ss");
			String end = jsonObject.getString("end");
			String start = jsonObject.getString("start");
			if (start != null && end != null && end != "null" && start != "null") {
				LocalDateTime endDateTime = LocalDateTime.parse(end, reader);
				LocalDateTime startDateTime = LocalDateTime.parse(start, reader);
				filter.start = startDateTime;
				filter.end = endDateTime;
				filter.hasDates = true;
			}
			filter.layeringOrder = jsonObject
					.getInt("layeringOrder");
			filter.nickname = (String) jsonObject.getString("nickname");
			filter.sourceId = jsonObject
					.getInt("sourceId");
		} catch (JSONException e) {
		}
	}

	static class Observatory {
		private LinkedHashMap<String, Filter> filters = new LinkedHashMap<String, Filter>();
		private String name;
		private ArrayList<String> uiLabels;

		private Observatory(String name) {
			this.name = name;
		}

		private void addFilter(String name, Filter filter) {
			filters.put(name, filter);
		}

		public Collection<Filter> getInstruments() {
			return this.filters.values();
		}

		public ArrayList<String> getUiLabels() {
			return uiLabels;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

	static class Filter {
		private LinkedHashMap<String, Filter> filters = new LinkedHashMap<String, InstrumentModel.Filter>();
		private String name;

		private Boolean hasDates = false;
		private LocalDateTime start;
		private LocalDateTime end;
		private int layeringOrder;
		private String nickname;
		int sourceId;

		private Filter(String name) {
			this.name = name;
		}

		private void addFilter(String name, Filter filter) {
			filters.put(name, filter);
		}

		public Collection<Filter> getFilters() {
			return this.filters.values();
		}

		@Override
		public String toString() {
			return this.name;
		}
		
		public LocalDateTime getStart(){
			return start;
		}
		
		public LocalDateTime getEnd(){
			return end;
		}

	}

	public static void addAddLayerPanel(AddLayerPanel addLayerPanel) {
		InstrumentModel.addLayerPanel = addLayerPanel;
	}

}
