package org.helioviewer.jhv.base;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.Globals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.AbstractDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.downloadmanager.UltimateDownloadManager;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Observatories
{
	private static TreeMap<String, Observatory> observatories = new TreeMap<String, Observatories.Observatory>(new AlphanumComparator()); 
	private static final ArrayList<Runnable> updateListeners = new ArrayList<Runnable>(); 
	
	public static void addUpdateListener(Runnable _listener)
	{
		updateListeners.add(_listener);
	}
	
	static
	{
		Thread thread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				for(;;)
					try
					{
						final HTTPRequest httpRequest = new HTTPRequest(Globals.OBSERVATORIES_DATASOURCE, DownloadPriority.HIGH, AbstractDownloadRequest.INFINITE_TIMEOUT);
						UltimateDownloadManager.addRequest(httpRequest);
						final JSONObject json = new JSONObject(httpRequest.getDataAsString());
						
						
						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								addObservatories(json);
								
								if (Boolean.parseBoolean(Settings.getProperty("startup.loadmovie")))
								{
									try
									{
										Filter instrument = observatories.get("SDO").filters.get("AIA").filters.get("171");
										LocalDateTime start = instrument.end.minusWeeks(2);
										Layers.addLayer(new KakaduLayer(instrument.sourceId, start, instrument.end, 60*60, instrument.getNickname()));
									}
									catch(NullPointerException _npe)
									{
										Telemetry.trackException(_npe);
									}
								}
								
								for(Runnable ul:updateListeners)
									ul.run();
							}
						});
						
						break;
					}
					catch (IOException | JSONException _e)
					{
						Telemetry.trackException(_e);
						
						try
						{
							Thread.sleep(1000);
						}
						catch(InterruptedException _ie)
						{
							break;
						}
					}
					catch(InterruptedException _e)
					{
						break;
					}
			}
		}, "MODEL_LOAD");
		thread.setDaemon(true);
		thread.start();
	}

	public static Collection<Observatory> getObservatories()
	{
		return observatories.values();
	}

	private static void addObservatories(JSONObject _observatories)
	{
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = _observatories.sortedKeys();
		while (iterator.hasNext())
		{
			String observatoryName = iterator.next();
			Observatory observatory = new Observatory(observatoryName);
			try
			{
				JSONObject jsonObservatory = _observatories.getJSONObject(observatoryName);
				addFilter(jsonObservatory, observatory);
			}
			catch (JSONException _e)
			{
				Telemetry.trackException(_e);
			}
			observatories.put(observatoryName, observatory);
		}
	}
	
	private static void addFilter(JSONObject jsonFilter, Observatory observatory)
	{
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = jsonFilter.sortedKeys();
		while (iterator.hasNext())
		{
			String filterName = iterator.next();
			try
			{
				JSONObject jsonFilter1 = jsonFilter.getJSONObject(filterName);
				Filter detector1 = new Filter(filterName);
				observatory.addFilter(filterName, detector1);
				addFilter(jsonFilter1, detector1, observatory);
			}
			catch (Exception e)
			{
				addUILabel(jsonFilter, observatory);
				return;
			}
		}
	}
	
	private static void addFilter(JSONObject jsonFilter, Filter filter, Observatory observatory)
	{
		@SuppressWarnings("unchecked")
		Iterator<String> iterator = jsonFilter.sortedKeys();
		while (iterator.hasNext())
		{
			String filterName = iterator.next();
			try
			{
				JSONObject jsonFilter1 = jsonFilter.getJSONObject(filterName);
				Filter detector1 = new Filter(filterName);
				filter.addFilter(filterName, detector1);
				addFilter(jsonFilter1, detector1, observatory);
			}
			catch (Exception e)
			{
				addUILabel(jsonFilter, observatory);
				addData(jsonFilter, filter);
				return;
			}
		}
	}

	private static void addUILabel(JSONObject jsonObject, Observatory observatory)
	{
		JSONArray uiLabels;
		try
		{
			ArrayList<String> uiLabel = new ArrayList<String>();
			uiLabels = (JSONArray) jsonObject.get("uiLabels");
			for (int i = 1; i < uiLabels.length(); i++)
			{
				JSONObject obj = (JSONObject) uiLabels.get(i);
				uiLabel.add(obj.getString("label"));
			}
			observatory.uiLabels = uiLabel;
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
	}

	private static void addData(JSONObject jsonObject, Filter filter)
	{
		try
		{
			DateTimeFormatter reader = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			String end = jsonObject.getString("end");
			String start = jsonObject.getString("start");
			if (start != null && end != null && end != "null" && start != "null")
			{
				LocalDateTime endDateTime = LocalDateTime.parse(end, reader);
				LocalDateTime startDateTime = LocalDateTime.parse(start, reader);
				filter.start = startDateTime;
				filter.end = endDateTime;
			}
			//filter.layeringOrder = jsonObject.getInt("layeringOrder");
			filter.nickname = (String) jsonObject.getString("nickname");
			filter.sourceId = jsonObject.getInt("sourceId");
		}
		catch (JSONException e)
		{
		}
	}

	public static class Observatory
	{
		private final TreeMap<String, Filter> filters = new TreeMap<String, Filter>(new AlphanumComparator());
		private final String name;
		private ArrayList<String> uiLabels;

		private Observatory(String _name)
		{
			name = _name;
		}

		private void addFilter(String name, Filter filter)
		{
			filters.put(name, filter);
		}

		public Collection<Filter> getInstruments()
		{
			return filters.values();
		}

		public ArrayList<String> getUiLabels()
		{
			return uiLabels;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	public static class Filter
	{
		private TreeMap<String, Filter> filters = new TreeMap<String, Observatories.Filter>(new AlphanumComparator());
		private final String name;
		
		private LocalDateTime start;
		private LocalDateTime end;
		@Nullable private String nickname;
		public int sourceId;

		private Filter(String _name)
		{
			name = _name;
		}

		private void addFilter(String name, Filter filter)
		{
			filters.put(name, filter);
		}

		public @Nullable Collection<Filter> getFilters()
		{
			return filters.values();
		}

		@Override
		public String toString()
		{
			return name;
		}
		
		public LocalDateTime getStart()
		{
			return start;
		}
		
		public LocalDateTime getEnd()
		{
			return end;
		}
		
		public String getNickname()
		{
			return nickname;
		}
	}
}
