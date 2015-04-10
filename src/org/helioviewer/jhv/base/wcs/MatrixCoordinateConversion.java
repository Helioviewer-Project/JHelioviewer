package org.helioviewer.jhv.base.wcs;

import org.helioviewer.jhv.base.math.Matrix4d;

public interface MatrixCoordinateConversion extends CoordinateConversion {
    public Matrix4d getConversionMatrix();
}
