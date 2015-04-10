package org.helioviewer.jhv.viewmodel.region;

import org.helioviewer.jhv.base.math.RectangleDouble;
import org.helioviewer.jhv.base.math.Vector2d;

/**
 * Implementation of {@link BasicRegion}.
 * 
 * @author Ludwig Schmidt
 * */
public class StaticRegion implements BasicRegion {

    private final Vector2d lowerLeftCorner;
    private final Vector2d sizeVector;

    /**
     * Constructor where to pass the information as double values.
     * 
     * @param newLowerLeftX
     *            x coordinate of lower left corner of the region.
     * @param newLowerLeftY
     *            y coordinate of lower left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * */
    public StaticRegion(final double newLowerLeftX, final double newLowerLeftY, final double newWidth, final double newHeight) {
        lowerLeftCorner = new Vector2d(newLowerLeftX, newLowerLeftY);
        sizeVector = new Vector2d(newWidth, newHeight);
    }

    /**
     * Constructor where to pass the lower left corner information as a vector
     * and the size of the region as double values.
     * 
     * @param newLowerLeftCorner
     *            Vector2dDouble object which describes the position of the
     *            lower left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * */
    public StaticRegion(final Vector2d newLowerLeftCorner, final double newWidth, final double newHeight) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = new Vector2d(newWidth, newHeight);
    }

    /**
     * Constructor where to pass the left lower corner information as double
     * values and the size of the region as a Vector.
     * 
     * @param newLowerLeftX
     *            x coordinate of lower left corner of the region.
     * @param newLowerLeftY
     *            y coordinate of lower left corner of the region.
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the region.
     * */
    public StaticRegion(final double newLowerLeftX, final double newLowerLeftY, final Vector2d newSizeVector) {
        lowerLeftCorner = new Vector2d(newLowerLeftX, newLowerLeftY);
        sizeVector = newSizeVector;
    }

    /**
     * Constructor where to pass the left lower corner information and the size
     * of the region as a Vector.
     * 
     * @param newLowerLeftCorner
     *            Vector2dDouble object which describes the position of the
     *            lower left corner of the region.
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the region.
     * */
    public StaticRegion(final Vector2d newLowerLeftCorner, final Vector2d newSizeVector) {
        lowerLeftCorner = newLowerLeftCorner;
        sizeVector = newSizeVector;
    }

    /**
     * Constructor where to pass the region information by a rectangle.
     * 
     * @param newRectangle
     *            RectangleDouble object which represents the basic information
     *            of a region.
     * */
    public StaticRegion(final RectangleDouble newRectangle) {
        lowerLeftCorner = newRectangle.getLowerLeftCorner();
        sizeVector = newRectangle.getSize();
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2d getLowerLeftCorner() {
        return lowerLeftCorner;
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2d getSize() {
        return sizeVector;
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newLowerLeftCorner
     *            Vector2dDouble object which describes the position of the
     *            lower left corner of the region.
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final Vector2d newLowerLeftCorner, final Vector2d newSizeVector) {
        return new Region(new StaticRegion(newLowerLeftCorner, newSizeVector));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newCornerX
     *            x coordinate of lower left corner of the region.
     * @param newCornerY
     *            y coordinate of lower left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final double newCornerX, final double newCornerY, final double newWidth, final double newHeight) {
        return new Region(new StaticRegion(newCornerX, newCornerY, newWidth, newHeight));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newLowerLeftX
     *            x coordinate of lower left corner of the region.
     * @param newLowerLeftY
     *            y coordinate of lower left corner of the region.
     * @param newSizeVector
     *            Vector2dDouble object which describes the size of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final double newLowerLeftX, final double newLowerLeftY, final Vector2d newSizeVector) {
        return new Region(new StaticRegion(newLowerLeftX, newLowerLeftY, newSizeVector));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newLowerLeftCorner
     *            Vector2dDouble object which describes the position of the
     *            lower left corner of the region.
     * @param newWidth
     *            width of the region.
     * @param newHeight
     *            height of the region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final Vector2d newLowerLeftCorner, final double newWidth, final double newHeight) {
        return new Region(new StaticRegion(newLowerLeftCorner, newWidth, newHeight));
    }

    /**
     * Creates a RegionAdapter object by using the passed region information.
     * 
     * @param newRectangle
     *            RectangleDouble object which represents the basic information
     *            of a region.
     * @return a new RegionAdapter object.
     * */
    public static Region createAdaptedRegion(final RectangleDouble newRectangle) {
        return new Region(new StaticRegion(newRectangle));
    }

}
