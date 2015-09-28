package org.helioviewer.jhv.viewmodel;

import org.helioviewer.jhv.base.math.Vector2d;

/**
 * Represents a region.
 * 
 * A region describes an area in an image. BasicRegion holds the minimal needed
 * information to define a region.
 * <p>
 * A region usually will be scaled with the units per pixel (
 * {@link ImageSizeMetaData#getUnitsPerPixel}) value which is defined in the
 * meta data of an image. Apart from that, for solar images, the origin of
 * physical coordinate system is the center of the sun.
 * */
interface BasicRegion {

    /**
     * Returns the position of the lower left corner of the region.
     * 
     * @return a Vector2dDouble object which points to the lower left corner of
     *         the region.
     * */
    public Vector2d getLowerLeftCorner();

    /**
     * Returns the size of the region.
     * 
     * @return aVector2dDouble object which describes the size of the region.
     * */
    public Vector2d getSize();

}
