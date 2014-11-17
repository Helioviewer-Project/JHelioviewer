package org.helioviewer.jhv.base.math;


public class Triangle
{
    public double x1,y1,x2,y2,x3,y3,z1,z2,z3;

    public Triangle(double _x1,double _y1,double _z1,double _x2,double _y2,double _z2,double _x3,double _y3,double _z3)
    {
        x1=_x1;
        y1=_y1;
        z1=_z1;
        x2=_x2;
        y2=_y2;
        z2=_z2;
        x3=_x3;
        y3=_y3;
        z3=_z3;
    }
    
    public Triangle(Vector3d _a,Vector3d _b,Vector3d _c)
    {
        x1=_a.x;
        y1=_a.y;
        z1=_a.z;
        x2=_b.x;
        y2=_b.y;
        z2=_b.z;
        x3=_c.x;
        y3=_c.y;
        z3=_c.z;
    }
    
    public Triangle(Vector2d _a,Vector2d _b,Vector2d _c)
    {
        x1=_a.x;
        y1=_a.y;
        x2=_b.x;
        y2=_b.y;
        x3=_c.x;
        y3=_c.y;
    }
}
