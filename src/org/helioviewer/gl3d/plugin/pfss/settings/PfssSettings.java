package org.helioviewer.gl3d.plugin.pfss.settings;

import org.helioviewer.gl3d.scenegraph.math.GL3DVec3f;

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
	 * Maximal number of frames the plugin reads ahead
	 */
	public final static int FRAME_PRELOAD = 10;
	
	/**
	 * size of frame cache, should be bigger than preload
	 */
	public final static int FRAME_CACHE = 20;

	/**
	 * Maximal size of cache data.
	 */
	public final static int DATA_CACHE_SIZE = 25;
	
	/**
	 * Number of Preloaded PFSSdata
	 */
	public final static int DATA_PRELOAD_SIZE = 25;

	/**
	 * Stepsize for the Loading data
	 */
	public final static int LOD_STEPS = 1;

	/**
	 * URL of the dataserver
	 */
	public final static String SERVER_URL = "http://soleil.i4ds.ch/sol-win/";

	
	/**
	 * Deltas between the PFSS Files. Currently there is a file for every 6 hours
	 */
	public final static int FITS_FILE_D_HOUR = 6;
	public final static int FITS_FILE_D_MINUTES = 0;
	
	/**
	 * Color of the line (from sunradius to outside)
	 */
	public final static GL3DVec3f SUN_OUT_LINE_COLOR = new GL3DVec3f(0f, 1f, 0f);

	/**
	 * Color of the line (from outside to sunradius)
	 */
	public final static GL3DVec3f OUT_SUN_LINE_COLOR = new GL3DVec3f(1f, 0f, 1f);

	/**
	 * Color of the line (from sunradius to sunradius)
	 */
	public final static GL3DVec3f SUN_SUN_LINE_COLOR = new GL3DVec3f(1f, 1f, 1f);

	/**
	 * Alpha-value of lines
	 */
	public final static float LINE_ALPHA = 1.0f;

	/**
	 * Cos of angle for LOD in degree or radian, if you would use degree
	 * Math.toRadian(DEGREEVALUE))
	 */
	public final static double ANGLE_OF_LOD = Math.cos(Math.toRadians(5.0));

	/**
	 * Linewidth for the OpenGL visualization
	 */
	public final static float LINE_WIDTH = 1.5f;
}
