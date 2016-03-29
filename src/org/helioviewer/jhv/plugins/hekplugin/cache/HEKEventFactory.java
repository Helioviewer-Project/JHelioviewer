package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.plugins.hekplugin.Interval;
import org.helioviewer.jhv.plugins.hekplugin.settings.HEKSettings;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to parse JSON objects and create Event objects from them.
 * <p>
 * The class follows the singleton pattern.
 * */
class HEKEventFactory {

    // the sole instance of this class
    private static final HEKEventFactory SINGLETON = new HEKEventFactory();

    /**
     * The private constructor to support the singleton pattern.
     * */
    private HEKEventFactory() {
    }

    /**
     * Method returns the sole instance of this class.
     * 
     * @return the only instance of this class.
     * */
    public static HEKEventFactory getSingletonInstance() {
        return SINGLETON;
    }

    /**
     * Reads a JSONObject and creates a HEKEvent object from it.
     * 
     * @param json
     *            - Object to parse
     * @param sloppy
     *            - Return a not properly initialized HEKEvent if an error
     *            occurs (instead of returning null)
     * @return - the newly parsed event, null if an error occured
     * 
     * @see #UnsupportedFormat
     * @see #HEKEventFormat
     */
    public @Nullable HEKEvent parseHEK(JSONObject json, boolean sloppy) {
        HEKEvent result = new HEKEvent();

        try {
            String id = json.getString("kb_archivid");

            // for multithreading: each of these calls owns its own parser
            SimpleDateFormat hekDateFormat = new SimpleDateFormat(HEKSettings.API_DATE_FORMAT);
            Date start = hekDateFormat.parse(json.getString("event_starttime"));
            Date end = hekDateFormat.parse(json.getString("event_endtime"));

            Interval<Date> duration = new Interval<>(start, end);

            result.setDuration(duration);
            result.setId(id);
            result.setEventObject(json);
        } catch (ParseException | NumberFormatException | JSONException e) {
            if (!sloppy) {
                System.err.println("HEKEventFactory.ParseHEK(...) >> Could not parse HEK event: " + e.getMessage());
                result = null;
            }
        }

        return result;

    }

    /**
     * Generates a HEKPath from the current json object, limited to the category
     * of the event
     * 
     * @param cache
     *            - HEKCache for which this path is valid
     * @param json
     *            - json object to parse
     * @return - HEKPath
     */
    public HEKPath parseCategoryPath(HEKCache cache, JSONObject json) {

        HEKPath result = null;

        try {

            String type = json.getString("event_type").toLowerCase();
            String frm = json.getString("frm_name");

            String[] path = { "HEK", type, frm };
            result = new HEKPath(cache, path);

        } catch (JSONException e) {
            Telemetry.trackException(e);
        }

        return result;

    }

    /**
     * Generates a HEKPath from the current json object
     * 
     * @param cache
     *            - HEKCache for which this path is valid
     * @param json
     *            - json object to parse
     * @return - HEKPath
     */
    public HEKPath parseEventPath(HEKCache cache, JSONObject json) {

        HEKPath result = null;

        try {

            String type = json.getString("event_type").toLowerCase();
            String frm = json.getString("frm_name");
            String title = json.getString("kb_archivid");

            if (type == null || frm == null || title == null) {
                return null;
            }

            String[] path = { "HEK", type, frm, title };
            result = new HEKPath(cache, path);

        } catch (JSONException e) {
            Telemetry.trackException(e);
        }

        return result;

    }

    /**
     * Reads the HEK json response and parses it to a Map of HEKPaths and
     * HEKEvents
     * 
     * @param cache
     *            - Cache into this request should be filled later on
     * @param json
     *            - Response to parse
     * @return Map: HEKPath - HEKEvent
     */
    public HashMap<HEKPath, HEKEvent> parseEvents(JSONObject json) {
        HEKCache cache = HEKCache.getSingletonInstance();

        HashMap<HEKPath, HEKEvent> result = new HashMap<>();

        try {
            JSONArray jsonEvents = json.getJSONArray("result");

            for (int i = 0; i < jsonEvents.length(); i++) {
                JSONObject entry = jsonEvents.getJSONObject(i);

                HEKPath eventPath = parseEventPath(cache, entry);

                // Something went wrong when parsing the eventPath
                if (eventPath == null) {
                    System.err.println("Error parsing an event: Could not parse the eventPath");
                    continue;
                }

                @Nullable HEKEvent event = parseHEK(entry, false);

                // Something went wrong when parsing the event
                if (event == null) {
                	System.err.println("Error parsing an event: Could parse the eventPath, but not the event: " + eventPath);
                	continue;
                }
                
                if (event.getDuration() == null) {
                    System.err.println("Event has no Duration");
                    continue;
                }

                event.prepareCache();

                // set the events path
                event.setPath(eventPath);
                // and the backreference inside the eventpath
                eventPath.setObject(event);

                String lastPart = eventPath.getLastPart();

                int id_counter = 1;

                while (result.containsKey(eventPath)) {
                    id_counter++;
                    eventPath.setLastPart(lastPart + " " + id_counter);
                    System.out.println("ID NOT UNIQUE " + id_counter);
                }

                result.put(eventPath, event);
            }

        } catch (JSONException e) {
            Telemetry.trackException(e);
        }

        return result;
    }

    /**
     * Reads the HEK json response and parses it to a Map of HEKPaths and
     * HEKEvents
     * 
     * @param cache
     *            - Cache into this request should be filled later on
     * @param json
     *            - Response to parse
     * @return Map: HEKPath - HEKEvent
     */
    public List<HEKPath> parseStructure(JSONObject json) {
        HEKCache cache = HEKCache.getSingletonInstance();

        List<HEKPath> result = new ArrayList<>();

        try {
            JSONArray jsonEvents = json.getJSONArray("result");

            for (int i = 0; i < jsonEvents.length(); i++) {
                JSONObject entry = jsonEvents.getJSONObject(i);

                HEKPath eventPath = parseCategoryPath(cache, entry);
                result.add(eventPath);

            }

        } catch (JSONException e) {
            Telemetry.trackException(e);
        }

        return result;
    }
}
