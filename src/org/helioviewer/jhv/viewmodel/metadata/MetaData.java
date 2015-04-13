package org.helioviewer.jhv.viewmodel.metadata;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.RectangleDouble;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.region.StaticRegion;
import org.helioviewer.jhv.viewmodel.view.jp2view.ImmutableDateTime;

public abstract class MetaData {
  private Vector2d lowerLeftCorner;
    private Vector2d sizeVector;

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
    protected ImmutableDateTime time;

    protected double innerRadius;
    protected double outerRadius;
    protected double flatDistance;
    protected double maskRotation;
    protected Vector2d occulterCenter;
    protected Vector3d orientation = new Vector3d(0.00,0.00,1.00);
    protected Quaternion3d defaultRotation = new Quaternion3d();
    
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

    
    protected boolean hasCorona = false;
    protected boolean hasSphere = false;
    protected boolean hasRotation = false;
    protected LocalDateTime localDateTime;
	protected final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_DATE_TIME;
    
    /**
     * Default constructor, does not set size or position.
     */
    public MetaData(MetaDataContainer metaDataContainer) {
        lowerLeftCorner = null;
        sizeVector = null;
        
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
     * Constructor, setting size and position.
     * 
     * @param newLowerLeftCorner
     *            Physical lower left corner of the corresponding image
     * @param newSizeVector
     *            Physical size of the corresponding image
     */
    public MetaData(Vector2d newLowerLeftCorner, Vector2d newSizeVector) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = newSizeVector;
    }

    /**
     * Constructor, setting size and position.
     * 
     * @param newLowerLeftCornerX
     *            Physical lower left x-coordinate of the corresponding image
     * @param newLowerLeftCornerY
     *            Physical lower left y-coordinate of the corresponding image
     * @param newWidth
     *            Physical width of the corresponding image
     * @param newHeight
     *            Physical height of the corresponding image
     */
    public MetaData(double newLowerLeftCornerX, double newLowerLeftCornerY, double newWidth, double newHeight) {
        lowerLeftCorner = new Vector2d(newLowerLeftCornerX, newLowerLeftCornerY);
        sizeVector = new Vector2d(newWidth, newHeight);
    }

    /**
     * Constructor, setting size and position.
     * 
     * @param newLowerLeftCorner
     *            Physical lower left corner of the corresponding image
     * @param newWidth
     *            Physical width of the corresponding image
     * @param newHeight
     *            Physical height of the corresponding image
     */
    public MetaData(Vector2d newLowerLeftCorner, double newWidth, double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new Vector2d(newWidth, newHeight);
    }

    /**
     * Constructor, setting size and position.
     * 
     * @param newLowerLeftCornerX
     *            Physical lower left x-coordinate of the corresponding image
     * @param newLowerLeftCornerY
     *            Physical lower left y-coordinate of the corresponding image
     * @param newSizeVector
     *            Physical size of the corresponding image
     */
    public MetaData(double newLowerLeftCornerX, double newLowerLeftCornerY, Vector2d newSizeVector) {
        lowerLeftCorner = new Vector2d(newLowerLeftCornerX, newLowerLeftCornerY);
        sizeVector = newSizeVector;
    }

    /**
     * Constructor, setting size and position.
     * 
     * @param newRectangle
     *            Full physical rectangle of the corresponding image
     */
    public MetaData(RectangleDouble newRectangle) {
        lowerLeftCorner = newRectangle.getLowerLeftCorner();
        sizeVector = newRectangle.getSize();
    }

    /**
     * Copy constructor
     * 
     * @param original
     *            Object to copy
     */
    public MetaData(MetaData original) {
        lowerLeftCorner = new Vector2d(original.lowerLeftCorner);
        sizeVector = new Vector2d(original.sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2d getPhysicalImageSize() {
        return sizeVector;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2d getPhysicalLowerLeft() {
        return lowerLeftCorner;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double getPhysicalImageHeight() {
        return sizeVector.y;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double getPhysicalImageWidth() {
        return sizeVector.x;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2d getPhysicalLowerRight() {
        return lowerLeftCorner.add(sizeVector.getXVector());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2d getPhysicalUpperLeft() {
        return lowerLeftCorner.add(sizeVector.getYVector());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Vector2d getPhysicalUpperRight() {
        return lowerLeftCorner.add(sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized RectangleDouble getPhysicalRectangle() {
        return new RectangleDouble(lowerLeftCorner, sizeVector);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Region getPhysicalRegion() {
        return StaticRegion.createAdaptedRegion(lowerLeftCorner, sizeVector);
    }

    /**
     * Sets the physical size of the corresponding image.
     * 
     * @param newImageSize
     *            Physical size of the corresponding image
     */
    protected synchronized void setPhysicalImageSize(Vector2d newImageSize) {
        sizeVector = newImageSize;
    }

    /**
     * Sets the physical lower left corner the corresponding image.
     * 
     * @param newlLowerLeftCorner
     *            Physical lower left corner the corresponding image
     */
    protected synchronized void setPhysicalLowerLeftCorner(Vector2d newlLowerLeftCorner) {
        lowerLeftCorner = newlLowerLeftCorner;
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
    public Vector2i getResolution() {
        return pixelImageSize;
    }

    /**
     * {@inheritDoc}
     */
    public double getUnitsPerPixel() {
        return meterPerPixel;
    }

    /**
     * {@inheritDoc}
     */
    public ImmutableDateTime getDateTime() {
        return time;
    }
    
    public LocalDateTime getLocalDateTime(){
    	return localDateTime;
    }
    
    abstract boolean updatePixelParameters();

	public boolean hasSphere() {
		// TODO Auto-generated method stub
		return hasSphere;
	}

	public boolean hasCorona() {
		// TODO Auto-generated method stub
		return hasCorona;
	}
	
	public boolean hasRotation(){
		return hasRotation;
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

	public boolean checkForModifications() {
        boolean changed = updatePixelParameters();

        double currentMaskRotation = Math.toRadians(metaDataContainer.tryGetDouble("CROTA"));
        if (changed || Math.abs(maskRotation - currentMaskRotation) > Math.toRadians(1)) {
            maskRotation = currentMaskRotation;
            changed = true;
        }

        return changed;
	}

	public void updateDateTime(ImmutableDateTime newDateTime) {
        time = newDateTime;		
	}

	public double getRadiusSuninArcsec() {
        double distanceToSun = metaDataContainer.tryGetDouble("DSUN_OBS");
        return Math.atan(Constants.SUN_RADIUS / distanceToSun) * MathUtils.RAD_TO_DEG * 3600;
	}

	protected void calcDefaultRotation() {
		defaultRotation = Quaternion3d.calcRotation(orientation,
				new Vector3d(0, 0, 1));
	}
	
	public Quaternion3d getRotation(){
		return this.defaultRotation;
	}
}
