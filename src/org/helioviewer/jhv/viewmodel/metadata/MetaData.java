package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;

public abstract class MetaData
{
    private Rectangle2D physicalImageSize;
    
    protected MetaDataContainer metaDataContainer = null;
    protected String instrument = "";
    protected String detector = "";
    protected String measurement = " ";
    protected String observatory = " ";
    protected String fullName = "";
    protected Vector2i pixelImageSize = new Vector2i();
    protected double solarPixelRadius = -1;
    protected Vector2d sunPixelPosition = new Vector2d();

    protected double meterPerPixel;

    protected double innerRadius;
    protected double outerRadius;
    protected double flatDistance;
    protected double maskRotation;
    protected Vector2d occulterCenter;
    protected Vector3d orientation = new Vector3d(0.00,0.00, Constants.SUN_RADIUS);
    private Quaternion3d defaultRotation = new Quaternion3d();
    
    protected double heeqX;
    protected double heeqY;
    protected double heeqZ;
    protected boolean heeqAvailable = false;

    protected double heeX;
    protected double heeY;
    protected double heeZ;
    protected boolean heeAvailable = false;

    protected double crlt;
    protected double crln;
    protected double dobs;
    protected boolean carringtonAvailable = false;

    protected double stonyhurstLongitude;
    protected double stonyhurstLatitude;
    protected boolean stonyhurstAvailable = false;
    
    protected LocalDateTime localDateTime;
	protected final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_DATE_TIME;
    
	protected LUT_ENTRY defaultLUT = LUT_ENTRY.GRAY;
	
	protected Rectangle newResolution;

	private double arcsecPerPixelX;

	private double arcsecPerPixelY;
	
    /**
     * Default constructor, does not set size or position.
     */
    public MetaData(MetaDataContainer metaDataContainer, Rectangle resolution) {
    	int width = metaDataContainer.tryGetInt("NAXIS1");
    	int height = metaDataContainer.tryGetInt("NAXIS2");
    	if (width > 0 && height > 0){
    		this.newResolution = new Rectangle(width, height);
    	}
    	else{
    		this.newResolution = resolution;
    	}
        
        if (metaDataContainer.get("INSTRUME") == null)
            return;

        detector = metaDataContainer.get("DETECTOR");
        instrument = metaDataContainer.get("INSTRUME");

        if (detector == null) {
            detector = " ";
        }
        if (instrument == null) {
            instrument = " ";
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Rectangle2D getPhysicalImageSize() {
        return physicalImageSize;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double getPhysicalImageHeight() {
        return this.getResolution().getHeight() * this.getUnitsPerPixel();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double getPhysicalImageWidth() {
        return this.getResolution().getWidth() * this.getUnitsPerPixel();
    }

    /**
     * Sets the physical size of the corresponding image.
     * 
     * @param newImageSize
     *            Physical size of the corresponding image
     */
    protected synchronized void setPhysicalImageSize(double x, double y, double width, double height) {
    	physicalImageSize = new Rectangle2D.Double(x, y, width, height);
    }
    
    /**
     * {@inheritDoc}
     */
    public String getDetector() {
        return detector;
    }

    /**
     * {@inheritDoc}
     */
    public String getInstrument() {
        return instrument;
    }

    /**
     * {@inheritDoc}
     */
    public String getMeasurement() {
        return measurement;
    }

    /**
     * {@inheritDoc}
     */
    public String getObservatory() {
        return observatory;
    }

    public String getFullName() {
        return fullName;
    }

    /**
     * {@inheritDoc}
     */
    public double getSunPixelRadius() {
        return solarPixelRadius;
    }

    /**
     * {@inheritDoc}
     */
    public Vector2d getSunPixelPosition() {
        return sunPixelPosition;
    }

    /**
     * {@inheritDoc}
     */
    public Rectangle getResolution() {
        return newResolution;
    }

    /**
     * {@inheritDoc}
     */
    public double getUnitsPerPixel() {
        return meterPerPixel;
    }

    public LocalDateTime getLocalDateTime(){
    	return localDateTime;
    }
    
	protected void updatePixelParameters()
	{
        double newSolarPixelRadius = -1.0;
        
        double sunX = metaDataContainer.tryGetDouble("CRPIX1");
        double sunY = metaDataContainer.tryGetDouble("CRPIX2");
        sunPixelPosition = new Vector2d(sunX, sunY);

        arcsecPerPixelX = metaDataContainer.tryGetDouble("CDELT1");
        arcsecPerPixelY = metaDataContainer.tryGetDouble("CDELT2");
        
        double distanceToSun = metaDataContainer.tryGetDouble("DSUN_OBS");
        double radiusSunInArcsec = Math.atan(Constants.SUN_RADIUS / distanceToSun) * MathUtils.RAD_TO_DEG * 3600;

        if (distanceToSun > 0)
        {
            newSolarPixelRadius = radiusSunInArcsec / arcsecPerPixelX;        	
        }
        else
        {
        	if (detector.equals("C2"))
        	{
        		newSolarPixelRadius = 80.814221;
        	}
        	else if (detector.equals("C3"))
        	{
        		newSolarPixelRadius = 17.173021;
        	}
        	else if (newResolution.getWidth() == 1024)
        	{
        		newSolarPixelRadius = 360;
        	}
        	else if(newResolution.getWidth() == 512)
        	{
        		newSolarPixelRadius = 180;
        	}
        }

        solarPixelRadius = newSolarPixelRadius;
        meterPerPixel = Constants.SUN_RADIUS / solarPixelRadius;
        
        
        setPhysicalImageSize(sunPixelPosition.x * -meterPerPixel, sunPixelPosition.y * -meterPerPixel, newResolution.getWidth() * meterPerPixel, newResolution.getHeight() * meterPerPixel);
	}
	
	public double getHEEX() {
        return heeX;
    }

    public double getHEEY() {
        return heeqY;
    }

    public double getHEEZ() {
        return heeZ;
    }

    public boolean isHEEProvided() {
        return heeAvailable;
    }

    public double getHEEQX() {
        return this.heeqX;
    }

    public double getHEEQY() {
        return this.heeqY;
    }

    public double getHEEQZ() {
        return this.heeqZ;
    }

    public boolean isHEEQProvided() {
        return this.heeqAvailable;
    }

    public double getCrln() {
        return crln;
    }

    public double getCrlt() {
        return crlt;
    }

    public double getDobs() {
        return dobs;
    }

    public boolean isCarringtonProvided() {
        return carringtonAvailable;
    }

    public boolean isStonyhurstProvided() {
        return stonyhurstAvailable;
    }

    public double getStonyhurstLatitude() {
        return stonyhurstLatitude;
    }

    public double getStonyhurstLongitude() {
        return stonyhurstLongitude;
    }
    
    /**
     * {@inheritDoc}
     */
    public double getInnerPhysicalOcculterRadius() {
        return innerRadius;
    }

    /**
     * {@inheritDoc}
     */
    public double getOuterPhysicalOcculterRadius() {
        return outerRadius;
    }

    /**
     * {@inheritDoc}
     */
    public double getPhysicalFlatOcculterSize() {
        return flatDistance;
    }

    public Vector2d getOcculterCenter() {
        return occulterCenter;
    }

	public double getMaskRotation() {
        return maskRotation;
	}

	public double getRadiusSuninArcsec() {
        double distanceToSun = metaDataContainer.tryGetDouble("DSUN_OBS");
        return Math.atan(Constants.SUN_RADIUS / distanceToSun) * MathUtils.RAD_TO_DEG * 3600;
	}

	protected void calcDefaultRotation() {
		defaultRotation = Quaternion3d.calcRotation(new Vector3d(0, 0, Constants.SUN_RADIUS), orientation);
	}
	
	public Quaternion3d getRotation()
	{
		return this.defaultRotation;
	}
	
	public LUT_ENTRY getDefaultLUT()
	{
		return defaultLUT;
	}
		
	//FIXME: don't assume that appX == appY
	public double getArcsecPerPixel(){
		return arcsecPerPixelX;
	}
}
