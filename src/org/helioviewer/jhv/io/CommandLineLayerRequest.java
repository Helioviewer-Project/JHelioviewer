package org.helioviewer.jhv.io;

import javax.annotation.Nullable;

/**
 * Struct for a single image layer within a JHV request from the command line
 */
class CommandLineLayerRequest
{
    public static final int numFields = 6;

    public static final int OBSERVATORY_INDEX = 0;
    public static final int INSTRUMENT_INDEX = 1;
    public static final int DETECTOR_INDEX = 2;
    public static final int MEASUREMENT_INDEX = 3;
    public static final int VISIBILITY_INDEX = 4;
    public static final int OPACITY_INDEX = 5;

    public @Nullable String observatory;
    public @Nullable String instrument;
    public @Nullable String detector;
    public @Nullable String measurement;
    public int opacity;
}
