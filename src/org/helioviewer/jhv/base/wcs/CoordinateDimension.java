package org.helioviewer.jhv.base.wcs;

/**
 * A Dimension of a {@link CoordinateSystem}.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public interface CoordinateDimension {
    public String getDescription();

    public double getMinValue();

    public double getMaxValue();

    public Unit getUnit();
}
