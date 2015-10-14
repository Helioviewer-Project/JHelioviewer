package org.helioviewer.jhv.io;

import javax.annotation.Nullable;

/**
 * Struct for a JHV request from the command line
 */
class CommandLineRequest
{
    public @Nullable String startTime = null;
    public @Nullable String endTime = null;
    public double imageScale = -1;
    public @Nullable JHVRequestLayer[] imageLayers = null;
	public @Nullable String cadence = null;
    public boolean linked = false;
}
