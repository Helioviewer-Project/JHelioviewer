package org.helioviewer.jhv.plugins.sdocutoutplugin;

public class SDOCutOutAPI {
	
    /** Date format the SDOCutOut API is using. */
	public final static String API_DATE_FORMAT = "yyyy-MM-dd";
	
	/** Time format the SDOCutOut API is using. */
	public final static String API_TIME_FORMAT = "HH:mm";
	
	/** Start date. */
	public final static String API_START_DATE = "startDate=";
	
	/** Start time. */
	public final static String API_START_TIME = "startTime=";
	
	/** Stop date. */
	public final static String API_STOP_DATE = "stopDate=";
	
	/** Stop time. */
	public final static String API_STOP_TIME = "stopTime=";

	/** Wavelengths. */
	public final static String API_WAVELENGTHS = "wavelengths=";
	
	/** Width*/
	public final static String API_WIDTH = "width=";
	
	/** Height. */
	public final static String API_HEIGHT = "height=";
	
	/** xCen. */
	public final static String API_XCEN = "xCen=";
	
	/** yCen. */
	public final static String API_YCEN = "yCen=";
	
	/** Cadence. */
	public final static String API_CADENCE = "cadence=";
	
	/** Cadence units. */
	public final static String API_CADENCEUNITS = "cadenceUnits=";
	
	
	/** URL of the SDOCutOut service (the date and time formats can be added in the url, i.e. {#StartDate,yyyy-MM-dd}. */
	public final static String API_URL = "http://www.lmsal.com/get_aia_data/?{#StartDate}&{#StartTime}&{#StopDate}&{#StopTime}&{#Wavelengths}&{#Width}&{#Height}&{#XCen}&{#YCen}&{#Cadence}";

}
