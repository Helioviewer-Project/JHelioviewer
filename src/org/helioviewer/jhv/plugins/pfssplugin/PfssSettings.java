package org.helioviewer.jhv.plugins.pfssplugin;

import org.helioviewer.jhv.base.math.Vector3d;

/**
 * Important settings
 * 
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssSettings {

	/**
	 * Needed for the JHV plugin initialization
	 */
	public final static String PLUGIN_LOCATION = "PfssPlugin";

	/**
	 * Maximum size of cache data.
	 */
	public final static int DATA_CACHE_SIZE = 1000;
	
	/**
	 * Number of Preloaded PFSSdata
	 */
	public final static int DATA_READ_AHEAD_SIZE = 200;

	
	/**
	 * URL of the dataserver
	 */
	public final static String SERVER_URL = "http://soleil.i4ds.ch/sol-win/v1/";

	
	//TODO: get rid of these - plugin shouldn't depend on the cadence of files
	/**
	 * Deltas between the PFSS Files. Currently there is a file for every 6 hours
	 */
	public final static int FITS_FILE_D_HOUR = 6;
	public final static int FITS_FILE_D_MINUTES = 0;
	
	/**
	 * Color of the line (from sunradius to outside)
	 */
	public final static Vector3d SUN_OUT_LINE_COLOR = new Vector3d(0f, 1f, 0f);

	/**
	 * Color of the line (from outside to sunradius)
	 */
	public final static Vector3d OUT_SUN_LINE_COLOR = new Vector3d(1f, 0f, 1f);

	/**
	 * Color of the line (from sunradius to sunradius)
	 */
	public final static Vector3d SUN_SUN_LINE_COLOR = new Vector3d(1f, 1f, 1f);

	/**
	 * Alpha-value of lines
	 */
	public final static float LINE_ALPHA = 1.0f;

	/**
	 * Linewidth for the OpenGL visualization
	 */
	public final static float LINE_WIDTH = 1.5f;
}
