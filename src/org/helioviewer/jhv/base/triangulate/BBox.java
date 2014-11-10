package org.helioviewer.jhv.base.triangulate;

class BBox
{
    int imin;
    int imax;
    double ymin;
    double ymax;

    BBox(Triangulator _triRef,int _i,int _j)
    {
        imin=Math.min(_i,_j);
        imax=Math.max(_i,_j);
        ymin=Math.min(_triRef.points[imin].y,_triRef.points[imax].y);
        ymax=Math.max(_triRef.points[imin].y,_triRef.points[imax].y);
    }

    boolean pntInBBox(Triangulator _triRef,int _i)
    {
        if(imax<_i)
            return false;

        if(imin>_i)
            return false;

        if(ymax<_triRef.points[_i].y)
            return false;

        if(ymin>_triRef.points[_i].y)
            return false;

        return true;
    }

    boolean BBoxOverlap(BBox _bb)
    {
        if(imax<_bb.imin)
            return false;

        if(imin>_bb.imax)
            return false;

        if(ymax<_bb.ymin)
            return false;

        if(ymin>_bb.ymax)
            return false;

        return true;
    }
}
