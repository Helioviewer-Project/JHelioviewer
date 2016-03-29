package org.helioviewer.jhv.opengl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.opengl.camera.CameraMode.MODE;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class RayTrace
{
	public enum HitpointType
	{
		SPHERE, PLANE, SPHERE_AND_PLANE
	}

	private Sphere sphere;
	private Plane plane;

	public RayTrace()
	{
		sphere = new Sphere(new Vector3d(0, 0, 0), Constants.SUN_RADIUS);
		plane = new Plane(new Vector3d(1, 0, 0).cross(new Vector3d(0, 1, 0)), 0);
	}

	public Ray cast(int x, int y, MainPanel mainPanel)
	{
		double aspect = Math.min(1, mainPanel.getAspect());
		
		double newX = (x - mainPanel.getWidth() / 2.) / mainPanel.getWidth() * aspect;
		double newY = (y - mainPanel.getHeight() / 2.) / mainPanel.getWidth() * aspect;

		double tanFOV = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * 2;

		Vector3d origin;
		Vector3d direction;
		
		Matrix4d transformation = Matrix4d.createTranslationMatrix(mainPanel.getTranslationCurrent())
				.multiplied(mainPanel.getRotationCurrent().toMatrix());
		
		if (CameraMode.mode == MODE.MODE_3D)
		{
			origin = transformation.multiply(new Vector3d(0, 0, 0));
			direction = new Vector3d(newX * tanFOV, newY * tanFOV, -1).normalized();
		}
		else
		{
			tanFOV *= mainPanel.getTranslationCurrent().z;
			origin = transformation.multiply(new Vector3d(0, 0, 1))
					.add(new Vector3d(newX * tanFOV, newY * tanFOV, 0));
			direction = new Vector3d(0, 0, -1).normalized();
		}

		Ray ray = new Ray(origin, direction);
		return intersect(ray);
	}

	//this method is the exact equivalent of CoronaFragment.glsl and SphereFragment.glsl
	public List<Vector2d> castTexturepos(int _pixelX, int _pixelY, MainPanel _mainPanel, MetaData _metaData, Matrix4d _transformation)
	{
		ArrayList<Vector2d> res = new ArrayList<Vector2d>(2);
		
		double tanFOV = Math.tan(Math.toRadians(MainPanel.FOV / 2.0));
		Vector2d uv = new Vector2d(_pixelX / (double)_mainPanel.getWidth() * 2 - 1, _pixelY / (double)_mainPanel.getHeight() * 2 - 1);
		double xSunOffset =  (double) ((_metaData.sunPixelPosition.x - _metaData.resolution.x / 2.0) / (double)_metaData.resolution.x);
		double ySunOffset = -(double) ((_metaData.sunPixelPosition.y - _metaData.resolution.y / 2.0) / (double)_metaData.resolution.y);
		
		
	    /* MV --> z */
	    Ray ray;
	    double zTranslation = _transformation.multiply(new Vector4d(0,0,0,1)).z;
	    if (CameraMode.mode == MODE.MODE_2D)
	    {
		    //2D
	        Vector2d center = uv.scaled(zTranslation * tanFOV);
	        ray = new Ray(
	    	        (_transformation.multiply(new Vector4d(0,0,1,1))).xyz().add(new Vector3d(center, 0)),
	    	        new Vector3d(0, 0, -1.0)
        		);
	    }
	    else
	    {
		    //3D
	        ray = new Ray(
		        _transformation.multiply(new Vector4d((_transformation.multiply(new Vector4d(0,0,0,1))).xyz(),0)).xyz(),
		        _transformation.multiply(new Vector4d(uv.scaled(tanFOV), -1.0, 0)).normalized().xyz()
	        );
	    }
	    
	    double tSphere = sphere.intersect(ray);
	    double tPlane = plane.intersect(ray);
	    
	    
	   	if (tPlane > 0 && (tSphere<=0 || tPlane<tSphere))
	   	{
		    Vector2d pos = ray.origin.xy().add(ray.direction.xy().scaled(tPlane));
		    Vector2d texPos = pos.scaled(1/_metaData.getPhysicalImageWidth(), 1/_metaData.getPhysicalImageHeight()).add(0.5+xSunOffset, 0.5+ySunOffset);
	   		res.add(texPos);
	   	}
	   	
	   	if(tSphere > 0)
	   	{
		    Vector3d spherePos = ray.origin.add(ray.direction.scaled(tSphere));
		    if(spherePos.z >= 0)
		    {
			    Vector2d texPosSphere = spherePos.xy().scaled(1/_metaData.getPhysicalImageWidth(), 1/_metaData.getPhysicalImageHeight()).add(0.5+xSunOffset, 0.5+ySunOffset);
			    res.add(texPosSphere);
		    }
	   	}

	    return res;
	}

	private Ray intersect(Ray ray)
	{
		double tSphere = sphere.intersect(ray);
		double tPlane = plane.intersect(ray);
		if (tPlane > 0 && tSphere > 0)
		{
			ray.hitpointType = HitpointType.SPHERE_AND_PLANE;
			ray.tPlane = tPlane;
			ray.tSphere = tSphere;
		}
		else if (tSphere > 0)
		{
			ray.hitpointType = HitpointType.SPHERE;
			ray.tSphere = tSphere;
		}
		else if (tPlane > 0)
		{
			ray.hitpointType = HitpointType.PLANE;
			ray.tPlane = tPlane;
		}
		return ray;
	}

	public static class Ray
	{
		private final Vector3d origin;
		private final Vector3d direction;
		private double tSphere = -1;
		private double tPlane = -1;
		private @Nullable HitpointType hitpointType;

		private Ray(Vector3d _origin, Vector3d _direction)
		{
			origin = _origin;
			direction = _direction;
		}

		public Vector3d getHitpoint()
		{
			if (hitpointType == HitpointType.SPHERE || hitpointType == HitpointType.SPHERE_AND_PLANE)
				return getHitpointOnSphere();
			else
				return getHitpointOnPlane();
		}

		public @Nullable HitpointType getHitpointType()
		{
			return hitpointType;
		}

		public Vector3d getHitpointOnSphere()
		{
			return origin.add(direction.scaled(tSphere));
		}

		public Vector3d getHitpointOnPlane()
		{
			return origin.add(direction.scaled(tPlane));
		}

		@Override
		public String toString()
		{
			return getHitpoint() + "";
		}
	}

	static private class Sphere
	{
		public final Vector3d center;
		public final double radius;

		public Sphere(Vector3d _center, double _radius)
		{
			center = _center;
			radius = _radius;
		}

		public double intersect(Ray ray)
		{
			double t = -1;
			Vector3d oc = ray.origin.subtract(this.center);
			double b = 2 * oc.dot(ray.direction);
			double c = oc.dot(oc) - this.radius * this.radius;
			double determinant = (b * b) - (4 * c);
			if (determinant >= 0)
				t = (-b - Math.sqrt(determinant)) / 2.0;
			return t;
		}
	}

	static private class Plane
	{
		public final Vector3d normal;
		public final double distance;

		public Plane(Vector3d _normal, double _distance)
		{
			normal = _normal;
			distance = _distance;
		}

		public double intersect(Ray ray)
		{
			return -(this.distance + ray.origin.dot(this.normal)) / ray.direction.dot(this.normal);
		}
	}
}
