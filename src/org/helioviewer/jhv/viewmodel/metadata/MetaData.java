package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.LUT.Lut;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//TODO: look at memory consumption and instance count of this class
//TODO: make immutable
public abstract class MetaData
{
    private static final DateTimeFormatter SOHO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd'T'HH:mm:ss.SSS");

    private final Rectangle2D physicalImageSize;
    
    protected String instrument;
    protected @Nullable String detector;
    protected final String measurement;
    protected final String observatory;
    protected @Nullable String fullName;
    protected final double solarPixelRadius;
    protected final Vector2d sunPixelPosition;

    protected final double meterPerPixel;

    protected double innerRadius;
    protected double outerRadius;
    protected double flatDistance;
    protected double maskRotation;
    protected @Nullable Vector2d occulterCenter;
    protected Vector3d orientation = new Vector3d(0, 0, Constants.SUN_RADIUS);
    protected Quaternion3d defaultRotation = new Quaternion3d();
    
    protected final double heeqX;
    protected final double heeqY;
    protected final double heeqZ;
    protected final boolean heeqAvailable;

    protected final double heeX;
    protected final double heeY;
    protected final double heeZ;
    protected final boolean heeAvailable;

    protected final double crlt;
    protected final double crln;
    protected final double dobs;
    protected final boolean carringtonAvailable;

    protected final double stonyhurstLongitude;
    protected final double stonyhurstLatitude;
    protected final boolean stonyhurstAvailable;
    
    protected LocalDateTime localDateTime;
    
	protected Lut defaultLUT = Lut.GRAY;
	
	protected Vector2i newResolution;

	private double arcsecPerPixelX;
	private double arcsecPerPixelY;
	
	public MetaData(Document _doc, Vector2i _resolution, @Nullable String _observatory, @Nullable String _measurement)
    {
		if(_measurement==null)
			throw new UnsuitableMetaDataException();
		
		if(_observatory==null)
			throw new UnsuitableMetaDataException();
		
		measurement = _measurement;
		observatory = _observatory;
		
    	int width = tryGetInt(_doc, "NAXIS1");
    	int height = tryGetInt(_doc, "NAXIS2");
    	
    	if (width > 0 && height > 0)
    		newResolution = new Vector2i(width, height);
    	else
    	{
    		System.err.println("Weird, this image has no resolution... Using default");
    		Telemetry.trackEvent("Missing resolution info", "Observatory", _observatory, "Measurement", _measurement);
    		newResolution = _resolution;
    	}
        
        detector = get(_doc, "DETECTOR");
        
        String instrume = get(_doc, "INSTRUME");
        if (instrume == null)
            throw new UnsuitableMetaDataException("No instrument specified in metadata (INSTRUME)");
        
        instrument = instrume;
        
        String observedDate = get(_doc, "DATE_OBS");
        if(observedDate==null || "".equals(observedDate))
        	observedDate = get(_doc, "DATE-OBS");
        
        String observedTime = get(_doc, "TIME_OBS");
        if(observedDate!=null && !observedDate.contains("T") && observedTime!=null && !"".equals(observedTime))
        	observedDate += "T" + get(_doc, "TIME_OBS");
        
        if(observedDate==null)
            throw new UnsuitableMetaDataException("No date/time specified in metadata (DATE_OBS)");
        
        try
        {
        	localDateTime = LocalDateTime.parse(observedDate, DateTimeFormatter.ISO_DATE_TIME);
        }
        catch(DateTimeParseException _dtpe)
        {
        	localDateTime = LocalDateTime.parse(observedDate, SOHO_DATE_TIME_FORMATTER);
        }
        
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

        
        stonyhurstLatitude = tryGetDouble(_doc, "HGLT_OBS");
        stonyhurstLongitude = tryGetDouble(_doc, "HGLN_OBS");
        stonyhurstAvailable = !Double.isNaN(stonyhurstLatitude) && !Double.isNaN(stonyhurstLongitude) && (stonyhurstLatitude != 0.0 || stonyhurstLongitude != 0.0);
        
        double newSolarPixelRadius = -1.0;
        
        double sunX = tryGetDouble(_doc, "CRPIX1");
        double sunY = tryGetDouble(_doc, "CRPIX2");
        sunPixelPosition = new Vector2d(sunX, sunY);

        arcsecPerPixelX = tryGetDouble(_doc, "CDELT1");
        arcsecPerPixelY = tryGetDouble(_doc, "CDELT2");
        
        double radiusSunInArcsec = Math.atan(Constants.SUN_RADIUS / dobs) * MathUtils.RAD_TO_DEG * 3600;

        if (!Double.isNaN(dobs) && dobs > 0)
            newSolarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;        	
        else
        {
        	//TODO: move from general metadata into instrument specific class
        	if ("C2".equals(detector))
        		newSolarPixelRadius = 80.814221;
        	else if ("C3".equals(detector))
        		newSolarPixelRadius = 17.173021;
        	else if (newResolution.x == 1024)
        		newSolarPixelRadius = 360;
        	else if(newResolution.x == 512)
        		newSolarPixelRadius = 180;
        }

        solarPixelRadius = newSolarPixelRadius;
        meterPerPixel = Constants.SUN_RADIUS / solarPixelRadius;
        
        physicalImageSize = new Rectangle2D.Double(sunPixelPosition.x * -meterPerPixel, sunPixelPosition.y * -meterPerPixel, newResolution.x * meterPerPixel, newResolution.y * meterPerPixel);
    }

    public @Nullable Rectangle2D getPhysicalImageSize()
    {
        return physicalImageSize;
    }

    public double getPhysicalImageHeight()
    {
        return getResolution().y * getUnitsPerPixel();
    }
    
    public double getPhysicalImageWidth()
    {
        return getResolution().x * getUnitsPerPixel();
    }

    public @Nullable String getDetector()
    {
        return detector;
    }

    public @Nullable String getInstrument()
    {
        return instrument;
    }

    public @Nullable String getMeasurement()
    {
        return measurement;
    }

    public @Nullable String getObservatory()
    {
        return observatory;
    }

    public @Nullable String getFullName()
    {
        return fullName;
    }

    public double getSunPixelRadius()
    {
        return solarPixelRadius;
    }

    public Vector2d getSunPixelPosition()
    {
        return sunPixelPosition;
    }

    public Vector2i getResolution()
    {
        return newResolution;
    }

    public double getUnitsPerPixel()
    {
        return meterPerPixel;
    }

    public LocalDateTime getLocalDateTime()
    {
    	return localDateTime;
    }
    
	public double getHEEX()
	{
        return heeX;
    }

    public double getHEEY()
    {
        return heeqY;
    }

    public double getHEEZ()
    {
        return heeZ;
    }

    public boolean isHEEProvided()
    {
        return heeAvailable;
    }

	public double getHEEQX()
	{
		return heeqX;
	}

	public double getHEEQY()
	{
		return heeqY;
	}

	public double getHEEQZ()
	{
		return heeqZ;
	}

	public boolean isHEEQProvided()
	{
		return heeqAvailable;
	}

	public double getCrln()
	{
		return crln;
	}

	public double getCrlt()
	{
		return crlt;
	}

	public double getDobs()
	{
		return dobs;
	}

	public boolean isCarringtonProvided()
	{
		return carringtonAvailable;
	}

	public boolean isStonyhurstProvided()
	{
		return stonyhurstAvailable;
	}

	public double getStonyhurstLatitude()
	{
		return stonyhurstLatitude;
	}

	public double getStonyhurstLongitude()
	{
		return stonyhurstLongitude;
	}

	public double getInnerPhysicalOcculterRadius()
	{
		return innerRadius;
	}

	public double getOuterPhysicalOcculterRadius()
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

	public double getMaskRotation()
	{
		return maskRotation;
	}

	public Quaternion3d getRotation()
	{
		return defaultRotation;
	}
	
	public Lut getDefaultLUT()
	{
		return defaultLUT;
	}
	
	public Vector2d getArcsecPerPixel()
	{
		return new Vector2d(arcsecPerPixelX,arcsecPerPixelY);
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
