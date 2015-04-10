package org.helioviewer.jhv.viewmodel.viewport;

import org.helioviewer.jhv.base.math.Vector2i;

/**
 * Extension of {@link BasicViewport}, representing a region.
 * 
 * It might be useful to get the basic information of a viewport in another way.
 * The methods provide a mapping of the basic values in different formats.
 * 
 * @author Ludwig Schmidt
 * */
public interface Viewport {

    /**
     * Returns the size of the viewport.
     * 
     * @return size of the viewport represented by a Vector2dInt object.
     * */
    public Vector2i getSize();

    /**
     * Returns the width of the viewport.
     * 
     * @return width of the viewport.
     * */
    public int getWidth();

    /**
     * Returns the height of the viewport.
     * 
     * @return height of the viewport.
     * */
    public int getHeight();

    /**
     * Returns the ratio of width and height.
     * 
     * @return ratio of width and height.
     * */
    public double getAspectRatio();

    public boolean equals(Viewport v);

}
