package org.helioviewer.jhv.base.triangulate;

import org.helioviewer.jhv.base.math.Vector3d;

class Matrix4f
{
    double m00;
    double m01;
    double m02;
    double m03;
    double m10;
    double m11;
    double m12;
    double m13;
    double m20;
    double m21;
    double m22;
    double m23;
    double m30;
    double m31;
    double m32;
    double m33;

    Matrix4f()
    {
        m00=0.0f;
        m01=0.0f;
        m02=0.0f;
        m03=0.0f;
        m10=0.0f;
        m11=0.0f;
        m12=0.0f;
        m13=0.0f;
        m20=0.0f;
        m21=0.0f;
        m22=0.0f;
        m23=0.0f;
        m30=0.0f;
        m31=0.0f;
        m32=0.0f;
        m33=0.0f;
    }

    public final Vector3d transform(Vector3d point)
    {
        return new Vector3d(
                m00*point.x+m01*point.y+m02*point.z+m03,
                m10*point.x+m11*point.y+m12*point.z+m13,
                m20*point.x+m21*point.y+m22*point.z+m23);
    }
}
