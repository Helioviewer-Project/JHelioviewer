package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;

import org.helioviewer.jhv.base.downloadmanager.DownloadPriority;
import org.helioviewer.jhv.base.downloadmanager.HTTPRequest;
import org.helioviewer.jhv.base.math.Interval;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKConstants;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;
import org.helioviewer.jhv.plugins.plugin.Plugins;
import org.json.JSONException;
import org.json.JSONObject;

public class HEKRequestThread extends HEKRequest implements Runnable {

    private HEKCacheController cacheController;

    public HEKRequestThread(HEKCacheController cacheController, HEKPath path, Interval<Date> interval) {
        this.cacheController = cacheController;
        this.path = path;
        this.interval = interval;
    }

    public void run() {

        // don't even start
        if (cancel) {
            return;
        }

        cacheController.setState(path, HEKCacheLoadingModel.PATH_LOADING);
        cacheController.fireEventsChanged(path);

        requestEvents(interval, path.getType(), path.getFRM());

        if (!cancel) {
            this.finishRequest();
        }
    }

    protected void finishRequest() {
        cacheController.setState(path, HEKCacheLoadingModel.PATH_NOTHING);

        // if the download was canceled, the tree will be refreshed by the one
        // that canceled all downloads
        if (!cancel) {
            cacheController.fireEventsChanged(path);
        }
    }

    /**
     * Request the events with given TYPE and FRM available in the given
     * interval
     * 
     * @param interval
     *            - interval to request events for
     * @param type
     *            - event requirement
     * @param frm
     *            - event requirement
     * @return
     */
    public void requestEvents(Interval<Date> interval, String type, String frm) {

        int page = 1;
        boolean hasMorePages = true;

        try {

            while (hasMorePages && page < HEKSettings.REQUEST_EVENTS_MAXPAGES) {

                // return if the current operation was canceled
                if (cancel)
                    return;

                String startDate = HEKConstants.getSingletonInstance().getDateFormat().format(interval.start);
                String endDate = HEKConstants.getSingletonInstance().getDateFormat().format(interval.end);

                String encFRM = URLEncoder.encode(frm, "UTF-8");
                String encType = URLEncoder.encode(type, "UTF-8");

                // if we do not specify any return fields, just do not mention
                // anything about them in the request
                String fieldRequest = "";

                if (HEKSettings.DOWNLOADER_DOWNLOAD_EVENTS_FIELDS.length > 0) {

                    fieldRequest = "&return=";

                    for (String field : HEKSettings.DOWNLOADER_DOWNLOAD_EVENTS_FIELDS) {
                        fieldRequest = fieldRequest + field + ",";
                    }

                    // strip of the last ","
                    fieldRequest = fieldRequest.substring(0, fieldRequest.length() - 1);

                }


                String uri = "http://www.lmsal.com/hek/her?cosec=2&cmd=search&type=column&event_type=" + encType + fieldRequest + "&event_starttime=" + startDate + "&event_endtime=" + endDate + "&event_coordsys=helioprojective&x1=-1200&x2=1200&y1=-1200&y2=1200&temporalmode=overlap&param0=FRM_Name&op0==&value0=" + encFRM + "&result_limit=" + HEKSettings.REQUEST_EVENTS_PAGESIZE + "&page=" + page;
                System.out.println("Requesting Page " + page + " of HEK Events: " + uri);

                // this might take a while
                HTTPRequest httpRequest = Plugins.generateAndStartHTPPRequest(uri, DownloadPriority.MEDIUM);
                // return if the current operation was canceled

                while (!httpRequest.isFinished()) {
                	try {
						Thread.sleep(20);
	                    if (cancel)
	                        return;
					} catch (InterruptedException e) {
						
						e.printStackTrace();
					}
				}

                JSONObject json = new JSONObject(httpRequest.getDataAsString());
                parseAndFeed(json, interval);

                hasMorePages = json.getBoolean("overmax");
                page++;
            }

        } catch (IOException e) {
            System.err.println("Error Parsing the HEK Response.");
            System.err.println("");
            e.printStackTrace();
        } catch (JSONException e) {
            System.err.println("Error Parsing the HEK Response.");
            System.err.println("");
            e.printStackTrace();
        }

    }

    private void parseAndFeed(JSONObject json, Interval<Date> interval) {
        HashMap<HEKPath, HEKEvent> events = HEKEventFactory.getSingletonInstance().parseEvents(json);
        cacheController.feedEvents(events, interval);
    }

}
