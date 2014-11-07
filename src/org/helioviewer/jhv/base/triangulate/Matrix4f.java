package org.helioviewer.jhv.base.triangulate;

public class Matrix4f
{
  public double m00;
  public double m01;
  public double m02;
  public double m03;
  public double m10;
  public double m11;
  public double m12;
  public double m13;
  public double m20;
  public double m21;
  public double m22;
  public double m23;
  public double m30;
  public double m31;
  public double m32;
  public double m33;

  public Matrix4f()
  {
    m00=0.0f;m01=0.0f;m02=0.0f;m03=0.0f;
    m10=0.0f;m11=0.0f;m12=0.0f;m13=0.0f;
    m20=0.0f;m21=0.0f;m22=0.0f;m23=0.0f;
    m30=0.0f;m31=0.0f;m32=0.0f;m33=0.0f;
  }
  
  public final void transform(Vec3 point,Vec3 pointOut)
  {
    double x=m00*point.x+m01*point.y+m02*point.z+m03;
    double y=m10*point.x+m11*point.y+m12*point.z+m13;
    pointOut.z=m20*point.x+m21*point.y+m22*point.z+m23;
    pointOut.x=x;
    pointOut.y=y;
  }
}
