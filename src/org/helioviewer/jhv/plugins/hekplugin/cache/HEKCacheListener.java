package org.helioviewer.jhv.plugins.hekplugin.cache;

public interface HEKCacheListener {

    void cacheStateChanged();

    void eventsChanged(HEKPath path);

    void structureChanged(HEKPath path);

}
