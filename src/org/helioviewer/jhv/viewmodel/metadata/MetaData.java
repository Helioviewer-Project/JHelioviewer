package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.coordinates.SunPosition;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quaternion;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.LUT;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//TODO: split into FrameMetadata & LayerMetadata, layerMetadata should be available without loading anything
public abstract class MetaData
{
    private static final DateTimeFormatter SOHO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");

    //TODO: make immutable
    protected @Nullable Vector2d solarPixelRadius;
	protected LUT defaultLUT = LUT.GRAY;
    protected double innerRadius;
    protected double outerRadius;
    protected double flatDistance;
    protected @Nullable Vector2d occulterCenter;
	
    public final String instrument;
    public final @Nullable String detector;
    public final String measurement;
    public final String observatory;
    public final @Nullable String displayName;
    public final Vector2d sunPixelPosition;
    public final long timeMS;
	public final Vector2i resolution;
	public final Vector2d arcsecPerPixel;
    public final double maskRotation;
    public final Quaternion rotation;
    
    public final double heeqX;
    public final double heeqY;
    public final double heeqZ;
    public final boolean heeqAvailable;

    public final double heeX;
    public final double heeY;
    public final double heeZ;
    public final boolean heeAvailable;

    public final double crlt;
    public final double crln;
    public final double dobs;
    public final boolean carringtonAvailable;

    public final double stonyhurstLongitude;
    public final double stonyhurstLatitude;
    public final boolean stonyhurstAvailable;

    public static final int GROUP_FOR_OPACITY_SUN = 1;
    public static final int GROUP_FOR_OPACITY_CORONA_SMALL = 2;
    public static final int GROUP_FOR_OPACITY_CORONA_OUTSIDE = 4;
    
    public static final int[] OPACITY_GROUPS = new int[]{GROUP_FOR_OPACITY_SUN, GROUP_FOR_OPACITY_CORONA_SMALL, GROUP_FOR_OPACITY_CORONA_OUTSIDE};
    	    
	public final int groupForOpacity;
    
	public MetaData(Document _doc, Vector2i _defaultResolution, @Nullable String _observatory, @Nullable String _measurement, @Nullable String _displayName, int _groupForOpacity)
    {
		if(_measurement==null)
			throw new UnsuitableMetaDataException();
		
		if(_observatory==null)
			throw new UnsuitableMetaDataException();
		
		groupForOpacity = _groupForOpacity;
		
		measurement = _measurement;
		observatory = _observatory;
		displayName = _displayName;
        detector = get(_doc, "DETECTOR");
        String instrume = get(_doc, "INSTRUME");
        if (instrume == null)
            throw new UnsuitableMetaDataException("No instrument specified in metadata (INSTRUME)");
        
        instrument = instrume;
        
    	int width = tryGetInt(_doc, "NAXIS1");
    	int height = tryGetInt(_doc, "NAXIS2");
    	
    	if (width > 0 && height > 0)
    		resolution = new Vector2i(width, height);
    	else
    	{
    		System.err.println("Weird, this image has no resolution... Using default");

    		Telemetry.trackEvent("Missing resolution info", "Measurement", measurement, "Detector", detector, "Instrument", instrument, "Observatory", observatory, "DATE_OBS", get(_doc, "DATE_OBS"), "TIME_OBS", get(_doc, "TIME_OBS"));
    		resolution = _defaultResolution;
    	}
        
        String observedDate = get(_doc, "DATE_OBS");
        if(observedDate==null || "".equals(observedDate))
        	observedDate = get(_doc, "DATE-OBS");
        
        String observedTime = get(_doc, "TIME_OBS");
        if(observedDate!=null && !observedDate.contains("T") && observedTime!=null && !"".equals(observedTime))
        	observedDate += "T" + get(_doc, "TIME_OBS");
        
        if(observedDate==null)
            throw new UnsuitableMetaDataException("No date/time specified in metadata (DATE_OBS)");
        
        LocalDateTime ldt;
        try
        {
        	ldt = LocalDateTime.parse(observedDate, DateTimeFormatter.ISO_DATE_TIME);
        }
        catch(DateTimeParseException _dtpe)
        {
        	ldt = LocalDateTime.parse(observedDate, SOHO_DATE_TIME_FORMATTER);
        }
        timeMS = MathUtils.fromLDT(ldt);
        
        heeqX = tryGetDouble(_doc, "HEQX_OBS");
        heeqY = tryGetDouble(_doc, "HEQY_OBS");
        heeqZ = tryGetDouble(_doc, "HEQZ_OBS");
        heeqAvailable = !Double.isNaN(heeqX) && !Double.isNaN(heeqY) && !Double.isNaN(heeqZ) && (heeqX != 0.0 || heeqY != 0.0 || heeqZ != 0.0);

        heeX = tryGetDouble(_doc, "HEEX_OBS");
        heeY = tryGetDouble(_doc, "HEEY_OBS");
        heeZ = tryGetDouble(_doc, "HEEZ_OBS");
        heeAvailable = !Double.isNaN(heeX) && !Double.isNaN(heeY) && !Double.isNaN(heeZ) && (heeX != 0.0 || heeY != 0.0 || heeZ != 0.0);

        crlt = tryGetDouble(_doc, "CRLT_OBS");
        crln = tryGetDouble(_doc, "CRLN_OBS");
        dobs = tryGetDouble(_doc, "DSUN_OBS"); //distanceToSun
        carringtonAvailable = !Double.isNaN(crlt) && !Double.isNaN(crln) && !Double.isNaN(dobs) && (crlt != 0.0 || crln != 0.0);

        //FIXME: sometimes "nan", example: hmi continuum for 2014-02-25 00:40:00-00:50:00
        stonyhurstLatitude = tryGetDouble(_doc, "HGLT_OBS");
        stonyhurstLongitude = tryGetDouble(_doc, "HGLN_OBS");
        stonyhurstAvailable = !Double.isNaN(stonyhurstLatitude) && !Double.isNaN(stonyhurstLongitude) && (stonyhurstLatitude != 0.0 || stonyhurstLongitude != 0.0);
        
        double sunX = tryGetDouble(_doc, "CRPIX1");
        double sunY = tryGetDouble(_doc, "CRPIX2");
        sunPixelPosition = new Vector2d(sunX, sunY);

        arcsecPerPixel = new Vector2d(tryGetDouble(_doc, "CDELT1"), tryGetDouble(_doc, "CDELT2"));
        
        if (!Double.isNaN(dobs) && dobs > 0)
        {
            double radiusSunInArcsec = Math.atan(Constants.SUN_RADIUS / dobs) * MathUtils.RAD_TO_DEG * 3600;
            solarPixelRadius = new Vector2d(radiusSunInArcsec / arcsecPerPixel.x, radiusSunInArcsec / arcsecPerPixel.y);
        }
        else if (resolution.x == 1024)
    	{
        	if(!(this instanceof MetaDataLASCO) && !(this instanceof MetaDataEIT))
        		Telemetry.trackEvent("Move this to instrument specific class", "Measurement", measurement, "Detector", detector, "Instrument", instrument, "Observatory", observatory, "timeMS", Long.toString(timeMS));
    		solarPixelRadius = new Vector2d(360,360);
    	}
    	else if(resolution.x == 512)
    	{
    		if(!(this instanceof MetaDataLASCO) && !(this instanceof MetaDataEIT))
    			Telemetry.trackEvent("Move this to instrument specific class", "Measurement", measurement, "Detector", detector, "Instrument", instrument, "Observatory", observatory, "timeMS", Long.toString(timeMS));
    		solarPixelRadius = new Vector2d(180,180);
    	}
        
        SunPosition sunPosition = SunPosition.computeSunPos(MathUtils.toLDT(timeMS));
        if (stonyhurstAvailable)
        {
//        	rotation = new Quaternion(
//        			Math.toRadians( sunPosition.getLongitude() - stonyhurstLongitude),
//        			Math.toRadians(-stonyhurstLatitude)-sunPosition.getDec());

        	rotation = new Quaternion(
        			Math.toRadians(-stonyhurstLongitude),
        			Math.toRadians(-stonyhurstLatitude));
        }
        else if (carringtonAvailable)
        {
        	rotation = new Quaternion(
        			0,
        			Math.toRadians(-crlt));
        }
        else
        	rotation = new Quaternion(0, 0);
        
		maskRotation = Math.toRadians(tryGetDouble(_doc, "CROTA"));
		
		innerRadius = 0;
		outerRadius = Double.MAX_VALUE; //not POSITIVE_INFINITY for better glsl compat (NaN & inf undefined in glsl pre-4.1)
    }

    public Rectangle2D getPhysicalImageSize()
    {
        return new Rectangle2D.Double(sunPixelPosition.x * -getUnitsPerPixel().x, sunPixelPosition.y * -getUnitsPerPixel().y, resolution.x * getUnitsPerPixel().x, resolution.y * getUnitsPerPixel().y);
    }

    public double getPhysicalImageHeight()
    {
        return resolution.y * getUnitsPerPixel().y;
    }
    
    public double getPhysicalImageWidth()
    {
        return resolution.x * getUnitsPerPixel().x;
    }

	public Vector2d getUnitsPerPixel()
    {
    	if(solarPixelRadius==null)
    		throw new RuntimeException("SolarPixelRadius was not initialized properly... Class "+getClass().getName());
    	
        return new Vector2d(Constants.SUN_RADIUS / solarPixelRadius.x,Constants.SUN_RADIUS / solarPixelRadius.y);
    }

    public double getInnerPhysicalRadius()
	{
		return innerRadius;
	}

	public double getOuterPhysicalRadius()
	{
		return outerRadius;
	}

	public double getPhysicalFlatOcculterSize()
	{
		return flatDistance;
	}

	public @Nullable Vector2d getOcculterCenter()
	{
		return occulterCenter;
	}

	public LUT getDefaultLUT()
	{
		return defaultLUT;
	}
	
	protected static @Nullable String get(Document _doc, String key)
	{
        return getValueFromXML(_doc, key, "fits");
	}

	private static @Nullable String getValueFromXML(Document _doc, String _key, String _node)
	{
		NodeList current = _doc.getElementsByTagName("meta");
		NodeList nodes = ((Element) current.item(0)).getElementsByTagName(_node);
		NodeList value = ((Element) nodes.item(0)).getElementsByTagName(_key);
		Element line = (Element) value.item(0);
		if (line != null)
		{
			Node child = line.getFirstChild();
			if (child instanceof CharacterData)
				return ((CharacterData) child).getData();
		}
		return null;
	}

	protected static int tryGetInt(Document _doc, String _key)
	{
		String string = get(_doc, _key);
        if (string == null)
        	return 0;

        try
        {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException e)
        {
            Telemetry.trackException(e);
            return 0;
        }
    }

	protected static double tryGetDouble(Document _doc, String _key)
	{
        String string = get(_doc, _key);
        if (string == null)
        	return Double.NaN;
        
        if(string.equalsIgnoreCase("nan"))
        	return Double.NaN;
        
        try
        {
            return Double.parseDouble(string);
        }
        catch (NumberFormatException e)
        {
        	Telemetry.trackException(e);
            return Double.NaN;
        }
    }
}
