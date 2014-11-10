package org.helioviewer.jhv.base.triangulate;

class Vec2
{
    double x;
    double y;

    Vec2(double _x,double _y)
    {
        x=_x;
        y=_y;
    }

    Vec2()
    {
        x=0;
        y=0;
    }

    public final void set(Vec2 _t)
    {
        x=_t.x;
        y=_t.y;
    }

    public boolean equals(Vec2 _t)
    {
        if(_t==null)
            return false;

        return(this.x==_t.x&&this.y==_t.y);
    }

    public boolean equals(Object _t)
    {
        try
        {
            return equals((Vec2)_t);
        }
        catch(ClassCastException e1)
        {
            return false;
        }
    }
}
