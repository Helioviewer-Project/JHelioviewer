package org.helioviewer.jhv.base.triangulate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class GeometryInfo
{
  private Vec3 coordinates[]=null;
  private int coordinateIndices[]=null;
  private int stripCounts[]=null;
  private int contourCounts[]=null;
  private Triangulator tr=new Triangulator();
  
  public GeometryInfo()
  {
  }
  
  public void setCoordinates(Vec3 _coordinates[])
  {
    coordinates=_coordinates;
  }
  
  public Vec3[] getCoordinates()
  {
    return coordinates;
  }
  
  public void setCoordinateIndices(int _coordinateIndices[])
  {
    coordinateIndices=_coordinateIndices;
  }
  
  public int[] getCoordinateIndices()
  {
    return coordinateIndices;
  }
  
  public void setStripCounts(int _stripCounts[])
  {
    stripCounts=_stripCounts;
  }
  
  public int[] getStripCounts()
  {
    return stripCounts;
  }
  
  public void setContourCounts(int _contourCounts[])
  {
    contourCounts=_contourCounts;
  }
  
  public int[] getContourCounts()
  {
    return contourCounts;
  }
  
  private int[] getListIndices(Object list[])
  {
    int indices[]=new int[list.length];
    HashMap<Object,Integer> table=new HashMap<Object,Integer>(list.length);
    
    for(int i=0;i<list.length;i++)
      if(!table.containsKey(list[i]))
      {
        indices[i]=i;
        table.put(list[i],i);
      }
      else
        indices[i]=table.get(list[i]);
    
    return indices;
  }
  
  void indexify()
  {
    if(coordinateIndices!=null)
      return;
    
    coordinateIndices=getListIndices(coordinates);
  }
  
  public List<Triangle> getGeometryArray()
  {
    tr.triangulate(this);
    
    if(coordinateIndices!=null)
    {
      Vec3[] unindexedCoordinates=new Vec3[coordinateIndices.length];
      for(int i=0;i<coordinateIndices.length;i++)
        unindexedCoordinates[i]=coordinates[coordinateIndices[i]];
      coordinates=unindexedCoordinates;
      coordinateIndices=null;
    }
    
    List<Triangle> res=new ArrayList<Triangle>();
    for(int i=0;i<coordinates.length;i+=3)
      res.add(new Triangle(coordinates[i+0].x,coordinates[i+0].y,coordinates[i+1].x,coordinates[i+1].y,coordinates[i+2].x,coordinates[i+2].y));
    return res;
  }
}
