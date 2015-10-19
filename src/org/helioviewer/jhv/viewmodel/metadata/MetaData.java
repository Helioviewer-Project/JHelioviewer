package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.LUT.Lut;

//TODO: look at memory consumption and instance count of this class
//TODO: make immutable
public abstract class MetaData
{
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
    
    protected final LocalDateTime localDateTime;
    
	protected Lut defaultLUT = Lut.GRAY;
	
	protected Vector2i newResolution;

	private double arcsecPerPixelX;
	private double arcsecPerPixelY;
	
    /**
     * Default constructor, does not set size or position.
     */
	public MetaData(MetaDataContainer _metaDataContainer, Vector2i _resolution, @Nullable String _observatory, @Nullable String _measurement)
    {
		if(_measurement==null)
			throw new UnsuitableMetaDataException();
		
		if(_observatory==null)
			throw new UnsuitableMetaDataException();
		
		measurement = _measurement;
		observatory = _observatory;
		
    	int width = _metaDataContainer.tryGetInt("NAXIS1");
    	int height = _metaDataContainer.tryGetInt("NAXIS2");
    	
    	if (width > 0 && height > 0)
    		newResolution = new Vector2i(width, height);
    	else
    		newResolution = _resolution;
        
        detector = _metaDataContainer.get("DETECTOR");
        
        String instrume = _metaDataContainer.get("INSTRUME");
        if (instrume == null)
            throw new UnsuitableMetaDataException("No instrument specified in metadata (INSTRUME)");
        
        instrument = instrume;
        
        String observedDate = _metaDataContainer.get("DATE_OBS");
        String observedTime = _metaDataContainer.get("TIME_OBS");
        if(observedDate!=null && !observedDate.contains("T") && observedTime!=null && !"".equals(observedTime))
        	observedDate += "T" + _metaDataContainer.get("TIME_OBS");
        
        if(observedDate==null)
            throw new UnsuitableMetaDataException("No date/time specified in metadata (DATE_OBS)");
        
        localDateTime = LocalDateTime.parse(observedDate, DateTimeFormatter.ISO_DATE_TIME);
        
        heeqX = _metaDataContainer.tryGetDouble("HEQX_OBS");
        heeqY = _metaDataContainer.tryGetDouble("HEQY_OBS");
        heeqZ = _metaDataContainer.tryGetDouble("HEQZ_OBS");
        heeqAvailable = heeqX != 0.0 || heeqY != 0.0 || heeqZ != 0.0;

        heeX = _metaDataContainer.tryGetDouble("HEEX_OBS");
        heeY = _metaDataContainer.tryGetDouble("HEEY_OBS");
        heeZ = _metaDataContainer.tryGetDouble("HEEZ_OBS");
        heeAvailable = heeX != 0.0 || heeY != 0.0 || heeZ != 0.0;

        crlt = _metaDataContainer.tryGetDouble("CRLT_OBS");
        crln = _metaDataContainer.tryGetDouble("CRLN_OBS");
        dobs = _metaDataContainer.tryGetDouble("DSUN_OBS");
        carringtonAvailable = crlt != 0.0 || crln != 0.0;

        stonyhurstLatitude = _metaDataContainer.tryGetDouble("HGLT_OBS");
        stonyhurstLongitude = _metaDataContainer.tryGetDouble("HGLN_OBS");
        stonyhurstAvailable = stonyhurstLatitude != 0.0 || stonyhurstLongitude != 0.0;
        
        
        double newSolarPixelRadius = -1.0;
        
        double sunX = _metaDataContainer.tryGetDouble("CRPIX1");
        double sunY = _metaDataContainer.tryGetDouble("CRPIX2");
        sunPixelPosition = new Vector2d(sunX, sunY);

        arcsecPerPixelX = _metaDataContainer.tryGetDouble("CDELT1");
        arcsecPerPixelY = _metaDataContainer.tryGetDouble("CDELT2");
        
        double distanceToSun = _metaDataContainer.tryGetDouble("DSUN_OBS");
        double radiusSunInArcsec = Math.atan(Constants.SUN_RADIUS / distanceToSun) * MathUtils.RAD_TO_DEG * 3600;

        if (distanceToSun > 0)
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
}
