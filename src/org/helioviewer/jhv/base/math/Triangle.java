package org.helioviewer.jhv.base.math;


public class Triangle
{
    public double x1,y1,x2,y2,x3,y3;
    
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
