package org.helioviewer.jhv.plugins.hekplugin;

import java.util.ArrayList;
import java.util.List;

public class IntervalContainer<TimeFormat extends Comparable<TimeFormat>, ItemFormat extends IntervalComparison<TimeFormat>> {

    boolean partial = false;
    int downloadableEvents = 0;

    public boolean isPartial() {
        return partial;
    }

    public void setPartial(boolean partial) {
        this.partial = partial;
    }

    List<ItemFormat> items = new ArrayList<ItemFormat>();

    public IntervalContainer(List<ItemFormat> newItems) {
        items = newItems;
    }

    public IntervalContainer() {
    }

    public List<ItemFormat> getItems() {
        return items;
    }

    public void incDownloadableEvents() {
        downloadableEvents++;
    }

    public int getDownloadableEvents() {
        return downloadableEvents;
    }

    public String toString() {
        return "[ IntervalContainer: Partial: " + partial + ", Downloadable Events: " + downloadableEvents + ", Items: " + items + " ]";
    }

}
