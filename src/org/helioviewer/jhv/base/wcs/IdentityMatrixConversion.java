package org.helioviewer.jhv.base.wcs;

import org.helioviewer.jhv.base.math.Matrix4d;

public class IdentityMatrixConversion implements MatrixCoordinateConversion {
    private static Matrix4d identity = Matrix4d.identity();

    private CoordinateSystem source;
    private CoordinateSystem target;

    public IdentityMatrixConversion(CoordinateSystem source, CoordinateSystem target) {
        this.source = source;
        this.target = target;
    }

    public CoordinateSystem getSourceCoordinateSystem() {
        return this.source;
    }

    public CoordinateSystem getTargetCoordinateSystem() {
        return this.target;
    }

    public CoordinateVector convert(CoordinateVector vector) {
        return vector;
    }

    public Matrix4d getConversionMatrix() {
        return identity;
    }
}
