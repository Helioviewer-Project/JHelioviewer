package org.helioviewer.jhv.opengl.model;

import java.util.List;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.base.wcs.conversion.SolarImageToSolarSphereConversion;
import org.helioviewer.jhv.base.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.jhv.base.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DImageTextureView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderProgram;

/**
 * Maps the solar disc part of an image layer onto an adaptive mesh that either covers the entire solar disc or the just the part that is visible in the view
 * frustum.
 * 
 * @author Simon Sp���rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DImageSphere extends GL3DImageMesh
{
  private SolarImageToSolarSphereConversion solarImageToSolarSphereConversion;

  private SolarImageCoordinateSystem solarImageCS=new SolarImageCoordinateSystem();
  private SolarSphereCoordinateSystem solarSphereCS=new SolarSphereCoordinateSystem();

  private GL3DImageLayer layer;

  private Matrix4d phiRotation=null;

  public GL3DImageSphere(GL3DImageTextureView imageTextureView,GLVertexShaderProgram vertexShaderProgram,GLFragmentShaderProgram fragmentShaderProgram,GL3DImageLayer imageLayer)
  {
    super("Sphere",imageTextureView,vertexShaderProgram,fragmentShaderProgram);
    layer=imageLayer;

  }

  public GL3DMeshPrimitive createMesh(GL3DState state,List<Vector3d> positions,List<Vector3d> normals,List<Vector2d> textCoords,List<Integer> indices,List<Vector4d> colors)
  {
    if(this.capturedRegion!=null)
    {
      solarImageToSolarSphereConversion=(SolarImageToSolarSphereConversion)solarImageCS.getConversion(solarSphereCS);
      solarImageToSolarSphereConversion.setAutoAdjustToValidValue(true);

      CoordinateVector orientationVector=this.layer.getOrientation();
      CoordinateConversion toViewSpace=this.layer.getCoordinateSystem().getConversion(state.activeCamera.getViewSpaceCoordinateSystem());

      Vector3d orientation=toViewSpace.convert(orientationVector).toVector3d().normalize();
      
      phiRotation = Quaternion3d.calcRotation(orientation,new Vector3d(0,0,1)).toMatrix();	        
      
      if(!(orientation.equals(new Vector3d(0,1,0))))
      {
        Vector3d orientationXZ=new Vector3d(orientation.x,0,orientation.z);
        double phi=0-Math.acos(orientationXZ.z);

        if(orientationXZ.x<0)
        {
          phi=0-phi;
        }
        phiRotation=Matrix4d.rotation(phi,new Vector3d(0,1,0));
      }
	
      double resolutionX=50;
      double resolutionY=50;

      for(int latNumber=0;latNumber<=resolutionX;latNumber++)
      {
        double theta=latNumber*Math.PI/resolutionX;
        double sinTheta=Math.sin(theta);
        double cosTheta=Math.cos(theta);

        for(int longNumber=0;longNumber<=resolutionY;longNumber++)
        {
          double phi=longNumber*2*Math.PI/resolutionY;
          double sinPhi=Math.sin(phi);
          double cosPhi=Math.cos(phi);

          double x=cosPhi*sinTheta;
          double y=cosTheta;
          double z=sinPhi*sinTheta;

          positions.add(new Vector3d(Constants.SUN_RADIUS*x,Constants.SUN_RADIUS*y,Constants.SUN_RADIUS*z));

          createVertex(solarSphereCS.createCoordinateVector(Constants.SUN_RADIUS*x,Constants.SUN_RADIUS*y,Constants.SUN_RADIUS*z),normals,textCoords,colors);

        }
      }

      Vector3d tmpSolarSphereVec;
      for(int latNumber=0;latNumber<resolutionX;latNumber++)
      {
        for(int longNumber=0;longNumber<resolutionY;longNumber++)
        {
          int first=(int)(latNumber*(resolutionY+1))+longNumber;
          int second=(int)(first+resolutionY+1);

          tmpSolarSphereVec=positions.get(first);
          double z0=tmpSolarSphereVec.z;
          if(phiRotation!=null)
          {
            z0=tmpSolarSphereVec.x*phiRotation.m[2]+tmpSolarSphereVec.y*phiRotation.m[6]+tmpSolarSphereVec.z*phiRotation.m[10]+phiRotation.m[14];
          }

          tmpSolarSphereVec=positions.get(first+1);
          double z1=tmpSolarSphereVec.z;
          if(phiRotation!=null)
          {
            z1=tmpSolarSphereVec.x*phiRotation.m[2]+tmpSolarSphereVec.y*phiRotation.m[6]+tmpSolarSphereVec.z*phiRotation.m[10]+phiRotation.m[14];
          }

          tmpSolarSphereVec=positions.get(second+1);
          double z2=tmpSolarSphereVec.z;
          if(phiRotation!=null)
          {
            z2=tmpSolarSphereVec.x*phiRotation.m[2]+tmpSolarSphereVec.y*phiRotation.m[6]+tmpSolarSphereVec.z*phiRotation.m[10]+phiRotation.m[14];
          }

          tmpSolarSphereVec=positions.get(second);
          double z3=tmpSolarSphereVec.z;
          if(phiRotation!=null)
          {
            z3=tmpSolarSphereVec.x*phiRotation.m[2]+tmpSolarSphereVec.y*phiRotation.m[6]+tmpSolarSphereVec.z*phiRotation.m[10]+phiRotation.m[14];
          }

          if(z0>=0&&z1>=0&&z2>=0&&z3>=0)
          {

            indices.add(first);
            indices.add(first+1);
            indices.add(second+1);
            indices.add(first);

            indices.add(second+1);
            indices.add(second);

          }
        }
      }
    }
    return GL3DMeshPrimitive.TRIANGLES;
  }

  private Vector3d createVertex(CoordinateVector solarSphereCoordinate,List<Vector3d> normals,List<Vector2d> texCoords,List<Vector4d> colors)
  {
    double x=solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.X_COORDINATE);
    double y=solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.Y_COORDINATE);
    double z=solarSphereCoordinate.getValue(SolarSphereCoordinateSystem.Z_COORDINATE);

    Vector3d position=new Vector3d(x,y,z);
    colors.add(new Vector4d(0,0,0,1.0));

    double cx=x*phiRotation.m[0]+y*phiRotation.m[4]+z*phiRotation.m[8]+phiRotation.m[12];
    double cy=x*phiRotation.m[1]+y*phiRotation.m[5]+z*phiRotation.m[9]+phiRotation.m[13];
    MetaData metaData=this.layer.metaDataView.getMetaData();

    double tx=(cx-metaData.getPhysicalLowerLeft().x)/(metaData.getPhysicalImageWidth());
    double ty=(cy-metaData.getPhysicalLowerLeft().y)/(metaData.getPhysicalImageHeight());

    texCoords.add(new Vector2d(tx,ty));

    return position;
  }

  public GL3DImageTextureView getImageTextureView()
  {
    return imageTextureView;
  }

}
