package org.helioviewer.jhv.base.wcs.conversion;

import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.base.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.jhv.base.wcs.impl.TextureCoordinateSystem;

public class SolarImageToTextureConversion implements CoordinateConversion {
    private SolarImageCoordinateSystem solarImageCoordinateSystem;
    private TextureCoordinateSystem textureCoordinateSystem;

    public SolarImageToTextureConversion(SolarImageCoordinateSystem solarImageCoordinateSystem, TextureCoordinateSystem textureCoordinateSystem) {
        this.solarImageCoordinateSystem = solarImageCoordinateSystem;
        this.textureCoordinateSystem = textureCoordinateSystem;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        double x = vector.getValue(SolarImageCoordinateSystem.X_COORDINATE);
        double y = vector.getValue(SolarImageCoordinateSystem.Y_COORDINATE);

        // Relative Coordinates within Region
        double _x = (x - this.textureCoordinateSystem.getRegion().getCornerX()) / textureCoordinateSystem.getRegion().getWidth();
        double _y = (y - this.textureCoordinateSystem.getRegion().getCornerY()) / textureCoordinateSystem.getRegion().getHeight();

        // Relative Coordinates within Texture, because Texture is usually
        // larger than the
        // captured image, due to PowerOf2 Texture size Restrictions
        _x = _x * this.textureCoordinateSystem.getTextureScaleX();
        _y = _y * this.textureCoordinateSystem.getTextureScaleY();

        CoordinateVector textureCoordinate = textureCoordinateSystem.createCoordinateVector(_x, _y);

        return textureCoordinate;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.solarImageCoordinateSystem;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.textureCoordinateSystem;
    }
}
