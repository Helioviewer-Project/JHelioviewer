package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Vector;

import org.helioviewer.jhv.base.math.Interval;

public class HEKCacheLoadingModel {

    HEKCache cache;
    HEKCacheModel cacheModel;

    public final static int PATH_LOADING = 2;
    public final static int PATH_QUEUED = 1;
    public final static int PATH_NOTHING = 0;

    private HashMap<HEKPath, Integer> loadingState = new HashMap<HEKPath, Integer>();

    public HEKCacheLoadingModel(HEKCache cache) {
        this.cache = cache;
        this.cacheModel = cache.getModel();
    }

    /**
     * Check if a node is in loading state
     * 
     * @param p
     *            - Path to be asking for
     * @param belowHidden
     *            - if true, the path will be seen as loading if any of its
     *            children is loading. Even if p has been explicitly set to
     *            "non-loading"! GEFH�RLICH! TODO
     * @return
     */
    public int getState(HEKPath p, boolean belowHidden) {

        if (loadingState.containsKey(p)) {
            int current_state = loadingState.get(p);
            if (!belowHidden) {
                return current_state;
            } else {
                if (current_state != PATH_NOTHING) {
                    return current_state;
                }
            }
        }

        int state = PATH_NOTHING;

        if (belowHidden) { // || !getExpansionState(p)) {
            Vector<HEKPath> childs = cacheModel.getChildren(p, true);
            for (HEKPath c : childs) {
                int current_state = getState(c, true);
                state = state | current_state;
            }
        }

        return state;
    }

    public HashMap<HEKPath, Vector<Interval<Date>>> filterState(HashMap<HEKPath, Vector<Interval<Date>>> needed, int state) {

        HashMap<HEKPath, Vector<Interval<Date>>> result = new HashMap<HEKPath, Vector<Interval<Date>>>();

        for (Entry<HEKPath,Vector<Interval<Date>>> e : needed.entrySet()) {
            if (getState(e.getKey(), false) == state) {
                result.put(e.getKey(), e.getValue());
            }

        }

        return result;
    }

    public void setState(HEKPath p, int state) {
        loadingState.put(p, state);
        // TODO: Move to Controller
        cache.getModel().fireCacheStateChanged();
    }

    public String toString() {
        return "HEKCacheLoadingModel " + loadingState.toString();
    }

}
