package org.helioviewer.jhv.base.wcs;

/**
 * Defines the unit a {@link CoordinateDimension} is defined in. See the
 * {@link UnitConverterFactory} for ways to convert to different Units.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public enum Unit {
    Meter("m"), Kilometer("km"), Radian("rad"), Degree("\u00B0"), Pixel("px");

    private String abbreviation;

    private Unit(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return this.abbreviation;
    }
}
