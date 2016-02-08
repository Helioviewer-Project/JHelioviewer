package org.helioviewer.jhv.base;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import javax.annotation.Nullable;
import javax.swing.SwingUtilities;

import org.helioviewer.jhv.base.downloadmanager.AbstractDownloadRequest;
import org.helioviewer.jhv.base.downloadmanager.DownloadManager;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Observatories
{
	private static TreeMap<String, Observatory> observatories = new TreeMap<>(new AlphanumComparator());
	private static final ArrayList<Runnable> updateListeners = new ArrayList<>();
	
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
						DownloadManager.addRequest(httpRequest);
						final JSONObject json = new JSONObject(httpRequest.getDataAsString());
						
						//TODO: show some error indication to the user, perhaps during startup?!
						if(json.has("error"))
							throw new Exception("Error when loading observatories: "+httpRequest.getDataAsString());

						SwingUtilities.invokeLater(new Runnable()
						{
							@Override
							public void run()
							{
								addObservatories(json);
								for(Runnable ul:updateListeners)
									ul.run();
							}
						});
						
						break;
					}
					catch(InterruptedException _e)
					{
						break;
					}
					catch (Throwable _e)
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
			uiLabels = (JSONArray) jsonObject.get("uiLabels");
			observatory.uiLabels.clear();
			for (int i = 1; i < uiLabels.length(); i++)
			{
				JSONObject obj = (JSONObject) uiLabels.get(i);
				observatory.uiLabels.add(obj.getString("label"));
			}
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
			if (start != null && end != null && !"null".equals(end) && !"null".equals(start))
			{
				filter.start = LocalDateTime.parse(start, reader);
				filter.end = LocalDateTime.parse(end, reader);
			}
			//filter.layeringOrder = jsonObject.getInt("layeringOrder");
			filter.nickname = jsonObject.getString("nickname");
			filter.sourceId = jsonObject.getInt("sourceId");
		}
		catch (JSONException e)
		{
			Telemetry.trackException(e);
		}
	}

	public static class Observatory
	{
		private final TreeMap<String, Filter> filters = new TreeMap<>(new AlphanumComparator());
		private final String name;
		private ArrayList<String> uiLabels= new ArrayList<>();

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
		private TreeMap<String, Filter> filters = new TreeMap<>(new AlphanumComparator());
		private final String name;
		
		@Nullable private LocalDateTime start;
		@Nullable private LocalDateTime end;
		private String nickname;
		public int sourceId;

		private Filter(String _name)
		{
			name = nickname = _name;
		}

		private void addFilter(String name, Filter filter)
		{
			filters.put(name, filter);
		}

		public Collection<Filter> getFilters()
		{
			return filters.values();
		}

		@Override
		public String toString()
		{
			return name;
		}
		
		public @Nullable LocalDateTime getStart()
		{
			return start;
		}
		
		public @Nullable LocalDateTime getEnd()
		{
			return end;
		}
		
		public String getNickname()
		{
			return nickname;
		}
	}
}
