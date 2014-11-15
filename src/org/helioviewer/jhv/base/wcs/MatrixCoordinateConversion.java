package org.helioviewer.jhv.base.wcs;

import org.helioviewer.jhv.base.math.GL3DMat4d;

public interface MatrixCoordinateConversion extends CoordinateConversion {
    public GL3DMat4d getConversionMatrix();
}
