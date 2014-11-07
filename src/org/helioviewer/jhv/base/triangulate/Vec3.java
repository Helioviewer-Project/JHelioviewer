package org.helioviewer.jhv.base.triangulate;

public class Vec3
{
  public double x;
  public double y;
  public double z;
    
  public Vec3()
  {
    x=0;
    y=0;
    z=0;
  }
  
  public Vec3(double _x,double _y,double _z)
  {
    x=_x;
    y=_y;
    z=_z;
  }
  
  public final void set(Vec3 _t)
  {
    x=_t.x;
    y=_t.y;
    z=_t.z;
  }
  
  public final float length()
  {
    return (float)Math.sqrt(this.x*this.x+this.y*this.y+this.z*this.z);
  }
  
  public final void cross(Vec3 v1,Vec3 v2)
  {
    double x=v1.y*v2.z-v1.z*v2.y;
    double y=v2.x*v1.z-v2.z*v1.x;
    
    z=v1.x*v2.y-v1.y*v2.x;
    this.x=x;
    this.y=y;
  }
  
  public final void sub(Vec3 t1,Vec3 t2)
  {
    x=t1.x-t2.x;
    y=t1.y-t2.y;
    z=t1.z-t2.z;
  }
  
  public final void negate()
  {
    x=-x;
    y=-y;
    z=-z;
  }
  
  public boolean equals(Vec3 _t)
  {
    if(_t==null)
      return false;
    
    return(this.x==_t.x&&this.y==_t.y&&this.z==_t.z);
  }
  
  @Override
  public int hashCode()
  {
      long a=Double.doubleToRawLongBits(x)^Long.rotateRight(Double.doubleToRawLongBits(y),21)^Long.rotateRight(Double.doubleToRawLongBits(z),42);
      return (int)(a ^ (a>>32));
  }
  
  public boolean equals(Object _t)
  {
    try
    {
      return equals((Vec3)_t);
    }
    catch(ClassCastException e1)
    {
      return false;
    }
  }
}
