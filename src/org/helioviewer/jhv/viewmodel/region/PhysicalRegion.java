package org.helioviewer.jhv.viewmodel.region;

import org.helioviewer.jhv.base.math.RectangleDouble;
import org.helioviewer.jhv.base.math.Vector2d;

/**
 * Extension of {@link BasicRegion}, representing a region.
 * 
 * It might be useful to get the basic information of a region in another way.
 * The methods provide a mapping of the basic values in different formats.
 * 
 * @author Ludwig Schmidt
 * */
public class PhysicalRegion implements BasicRegion {

    private final BasicRegion region;

    /**
     * Default constructor.
     * 
     * @param newRegion
     *            BasicRegion object which holds the minimal region description.
     * */
    public PhysicalRegion(BasicRegion newRegion) {
        region = newRegion;
    }

    /**
     * {@inheritDoc}
     * */
    public double getCornerX() {
        return region.getLowerLeftCorner().x;
    }

    /**
     * {@inheritDoc}
     * */
    public double getCornerY() {
        return region.getLowerLeftCorner().y;
    }

    /**
     * {@inheritDoc}
     * */
    public double getHeight() {
        return region.getSize().y;
    }

    /**
     * {@inheritDoc}
     * */
    public double getWidth() {
        return region.getSize().x;
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2d getLowerLeftCorner() {
        return region.getLowerLeftCorner();
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2d getSize() {
        return region.getSize();
    }

    /**
     * {@inheritDoc}
     * */
    public RectangleDouble getRectangle() {
        return new RectangleDouble(region.getLowerLeftCorner(), region.getSize());
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2d getLowerRightCorner() {
        return region.getLowerLeftCorner().add(region.getSize().getXVector());
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2d getUpperLeftCorner() {
        return region.getLowerLeftCorner().add(region.getSize().getYVector());
    }

    /**
     * {@inheritDoc}
     * */
    public Vector2d getUpperRightCorner() {
        return region.getLowerLeftCorner().add(region.getSize());
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (!(o instanceof PhysicalRegion)) {
            return false;
        }

        PhysicalRegion r = (PhysicalRegion) o;
        return r.getSize().equals(getSize()) && r.getLowerLeftCorner().equals(getLowerLeftCorner());

    }

    /**
     * {@inheritDoc}
     */

    public String toString() {
        return "[RegionAdapter: Rectangle " + this.getRectangle() + "]";
    }
}
