package org.helioviewer.jhv.plugins.hekplugin.cache;

import java.util.Date;

import org.helioviewer.jhv.base.math.Interval;

abstract class HEKRequest {

    /**
     * The path for which this request retrieves data
     */
    protected HEKPath path;

    /**
     * Interval for which this request retrieves data
     */
    protected Interval<Date> interval;

    /**
     * Flag showing if this request should be canceled
     */
    protected boolean cancel = false;

    /**
     * Sets the cancel flag to true and closes the currently used InputStream
     */
    public void cancel() {

        cancel = true;

        // we are not loading anymore
        this.finishRequest();
    }

    /**
     * Method to be called when the request is finished
     */
    protected abstract void finishRequest();

}
