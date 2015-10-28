package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Date;
import java.util.List;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.plugins.hekplugin.Interval;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;
import org.json.JSONException;
import org.json.JSONObject;

class HEKRequestStructureThread extends HEKRequest implements Runnable
{

	private HEKCacheController cacheController;

	public HEKRequestStructureThread(HEKCacheController cacheController, Interval<Date> interval)
	{
		this.cacheController = cacheController;
		this.interval = interval;
	}

	public void cancel()
	{
		cancel = true;

		// we are not loading anymore
		this.finishRequest();
	}

	protected void finishRequest()
	{
		cacheController.setState(cacheController.getRootPath(), HEKCacheLoadingModel.PATH_NOTHING);
		cacheController.expandToLevel(1, true, false);
		cacheController.fireEventsChanged(cacheController.getRootPath());
	}

	public void run()
	{

		if (cancel)
			return;

		cacheController.setState(cacheController.getRootPath(), HEKCacheLoadingModel.PATH_LOADING);
		cacheController.fireEventsChanged(cacheController.getRootPath());

		requestStructure(interval);

		if (!cancel)
		{
			this.finishRequest();
		}

	}

	public void requestStructure(Interval<Date> interval)
	{
		int page = 1;
		boolean hasMorePages = true;
		try
		{
			while (hasMorePages && page < (HEKSettings.REQUEST_STRUCTURE_MAXPAGES - 1))
			{
				// return if the current operation was canceled
				if (cancel)
					return;

				String startDate = HEKConstants.getSingletonInstance().getDateFormat().format(interval.start);
				String endDate = HEKConstants.getSingletonInstance().getDateFormat().format(interval.end);

				String fields = "";

				for (String field : HEKSettings.DOWNLOADER_DOWNLOAD_STRUCTURE_FIELDS)
					fields = fields + field + ",";

				fields = fields.substring(0, fields.length() - 1);

				String uri = "http://www.lmsal.com/hek/her?cosec=2&cmd=search&type=column&event_type=**&event_starttime="
						+ startDate + "&event_endtime=" + endDate
						+ "&event_coordsys=helioprojective&x1=-1200&x2=1200&y1=-1200&y2=1200&return=" + fields
						+ "&temporalmode=overlap&result_limit=" + HEKSettings.REQUEST_STRUCTURE_PAGESIZE + "&page="
						+ page;

				HTTPRequest httpRequest = Plugins.generateAndStartHTPPRequest(uri, DownloadPriority.MEDIUM);

				while (!httpRequest.isFinished())
				{
					try
					{
						Thread.sleep(20);
						// return if the current operation was canceled
						if (cancel)
							return;
					}
					catch (InterruptedException e)
					{
						Telemetry.trackException(e);
					}
				}

				JSONObject json;
				try
				{
					json = new JSONObject(httpRequest.getDataAsString());
					parseFeedAndUpdateGUI(json, interval);

					hasMorePages = json.getBoolean("overmax");
					page++;
				}
				catch (JSONException e)
				{
					throw e;
				}
				catch (Throwable e)
				{
					Telemetry.trackException(e);
				}
			}
		}
		catch (JSONException e)
		{
			System.err.println("Error Parsing the HEK Response.");
			System.err.println("");
			Telemetry.trackException(e);
		}
	}

	public void parseFeedAndUpdateGUI(JSONObject json, Interval<Date> timeRange)
	{
		// this code might need to change if requestStructure changes
		List<HEKPath> paths = HEKEventFactory.getSingletonInstance().parseStructure(json);
		cacheController.feedStructure(paths, timeRange);
	}

}
