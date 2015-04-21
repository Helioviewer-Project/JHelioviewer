package org.helioviewer.jhv.base.wcs.impl;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.wcs.Cartesian2DCoordinateSystem;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.GenericCoordinateDimension;
import org.helioviewer.jhv.base.wcs.Unit;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;

/**
 * The {@link TextureCoordinateSystem} defines texture coordinates of an image
 * that is stored in a GL Texture. It also provides the Region that is actually
 * captured by the texture.
 * 
 * @author Simon Spoerri (simon.spoerri@fhnw.ch)
 * 
 */
public class TextureCoordinateSystem extends Cartesian2DCoordinateSystem {
    private PhysicalRegion region;
    private Vector2d textureScale;

    public TextureCoordinateSystem(Vector2d textureScale, PhysicalRegion region) {
        super(new GenericCoordinateDimension(Unit.Pixel, "Texture XCoordinate", 0, 1.0), new GenericCoordinateDimension(Unit.Pixel, "Texture YCoordinate", 0, 1.0));

        this.region = region;
        this.textureScale = textureScale;
    }

    public CoordinateConversion getConversion(CoordinateSystem coordinateSystem) {
        return super.getConversion(coordinateSystem);
    }

    public PhysicalRegion getRegion() {
        return region;
    }

    public double getTextureScaleX() {
        return this.textureScale.x;
    }

    public double getTextureScaleY() {
        return this.textureScale.y;
    }
}
