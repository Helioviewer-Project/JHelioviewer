package org.helioviewer.jhv.opengl;

import java.awt.geom.Rectangle2D;

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
		SPHERE, PLANE, SPHERE_AND_PLANE;
	}

	private Sphere sphere;
	private Plane plane;

	public RayTrace()
	{
		sphere = new Sphere(new Vector3d(0, 0, 0), Constants.SUN_RADIUS);
		plane = new Plane(new Vector3d(1, 0, 0).cross(new Vector3d(0, 1, 0)), 0);
	}

	public RayTrace(Matrix4d rotation)
	{
		sphere = new Sphere(new Vector3d(0, 0, 0), Constants.SUN_RADIUS);
		plane = new Plane(rotation.multiply(new Vector3d(0, 0, 1)), 0);
	}

	public Ray cast(int x, int y, MainPanel mainPanel)
	{
		double newX = (x - mainPanel.getWidth() / 2.) / mainPanel.getWidth();
		double newY = (y - mainPanel.getHeight() / 2.) / mainPanel.getWidth();

		double width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * 2;

		Vector3d origin;
		Vector3d direction;
		if (CameraMode.mode == MODE.MODE_3D)
		{
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1));
			direction = new Vector3d(newX * width, newY * width, -1).normalized();
		}
		else
		{
			width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * mainPanel.getTranslationCurrent().z * 2;
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1))
					.add(new Vector3d(newX * width, newY * width, 0));
			direction = new Vector3d(0, 0, -1).normalized();
		}

		Ray ray = new Ray(origin, direction);
		return intersect(ray);
	}

	public Ray castScene(int x, int y, MainPanel mainPanel)
	{
		double newX = (x - mainPanel.getWidth() / 2.) / mainPanel.getWidth();
		double newY = (y - mainPanel.getHeight() / 2.) / mainPanel.getWidth();

		double width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * 2;

		Vector3d origin;
		Vector3d direction;
		if (CameraMode.mode == MODE.MODE_3D)
		{
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1));
			direction = new Vector3d(newX * width, newY * width, -1).normalized();
		}
		else
		{
			width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * mainPanel.getTranslationCurrent().z * 2;
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1))
					.add(new Vector3d(newX * width, newY * width, 0));
			direction = new Vector3d(0, 0, -1).normalized();
		}

		Vector4d tmpOrigin = new Vector4d(origin.x, origin.y, origin.z, 0);
		Vector4d tmpDirection = new Vector4d(direction.x, direction.y, direction.z, 0);

		Vector3d rayORot = mainPanel.getTransformation().multiply(origin);
		Vector3d rayDRot = mainPanel.getTransformation().multiply(direction);

		Vector4d rayORot1 = mainPanel.getTransformation().multiply(tmpOrigin);
		Vector4d rayDRot1 = mainPanel.getTransformation().multiply(tmpDirection);

		rayORot = new Vector3d(rayORot1.x, rayORot1.y, rayORot1.z);
		rayDRot = new Vector3d(rayDRot1.x, rayDRot1.y, rayDRot1.z);
		// plane.normal = camera.getTransformation().multiply(plane.normal);
		// Ray rayOriginal = new Ray(origin, direction);
		Ray ray = new Ray(rayORot, rayDRot);

		return intersect(ray);

	}

	public @Nullable Vector2d castTexturepos(int _pixelX, int _pixelY, MetaData _metaData, MainPanel _mainPanel)
	{
		plane = new Plane(_metaData.rotation.toMatrix().multiply(new Vector3d(0, 0, 1)), 0);
		double newX = (_pixelX - _mainPanel.getWidth() / 2.) / _mainPanel.getWidth();
		double newY = (_pixelY - _mainPanel.getHeight() / 2.) / _mainPanel.getWidth();
		double width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * 2;

		Vector3d origin;
		Vector3d direction;
		if (CameraMode.mode == MODE.MODE_3D)
		{
			origin = _mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1));
			direction = new Vector3d(newX * width, newY * width, -1).normalized();
		}
		else
		{
			width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * _mainPanel.getTranslationCurrent().z * 2.0;
			origin = _mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1))
					.add(new Vector3d(newX * width, newY * width, 0));
			direction = new Vector3d(0, 0, -1).normalized();
		}
		Vector4d tmpOrigin = new Vector4d(origin.x, origin.y, origin.z, 0);
		Vector4d tmpDirection = new Vector4d(direction.x, direction.y, direction.z, 0);

		Vector3d rayORot = _mainPanel.getTransformation().multiply(origin);
		Vector3d rayDRot = _mainPanel.getTransformation().multiply(direction);

		Vector4d rayORot1 = _mainPanel.getTransformation().multiply(tmpOrigin);
		Vector4d rayDRot1 = _mainPanel.getTransformation().multiply(tmpDirection);

		rayORot = new Vector3d(rayORot1.x, rayORot1.y, rayORot1.z);
		rayDRot = new Vector3d(rayDRot1.x, rayDRot1.y, rayDRot1.z);
		Ray rayOriginal = new Ray(origin, direction);
		Ray ray = new Ray(rayORot, rayDRot);
		ray = intersect(ray);
		rayOriginal.t = ray.t;
		if (ray.hitpointType == HitpointType.SPHERE
				&& _metaData.rotation.inversed().toMatrix().multiply(ray.getHitpoint()).z < 0)
			return null;

		Vector3d original = _metaData.rotation.inversed().toMatrix().multiply(ray.getHitpoint());
		Rectangle2D physicalImageSize = _metaData.getPhysicalImageSize();
		if (physicalImageSize == null)
			return null;

		double imageX = (Math.max(Math.min(original.x, physicalImageSize.getX() + physicalImageSize.getWidth()),
				physicalImageSize.getX()) - physicalImageSize.getX()) / physicalImageSize.getWidth();
		double imageY = (Math.max(Math.min(original.y, physicalImageSize.getY() + physicalImageSize.getHeight()),
				physicalImageSize.getY()) - physicalImageSize.getY()) / physicalImageSize.getHeight();
		return new Vector2d(imageX, imageY);
	}

	private Ray intersect(Ray ray)
	{
		double tSphere = sphere.intersect(ray);
		double tPlane = plane.intersect(ray);
		if (tSphere > 0)
		{
			ray.hitpointType = HitpointType.SPHERE;
			ray.tSphere = tSphere;
		}
		if (tPlane > 0.0 && tSphere < 0.)
		{
			ray.hitpointType = HitpointType.PLANE;
			ray.tPlane = tPlane;
		}
		else if (tPlane > 0.0 && (tSphere < 0.))
		{
			ray.hitpointType = HitpointType.SPHERE_AND_PLANE;
			ray.tPlane = tPlane;
		}
		return ray;
	}

	public static class Ray
	{
		private Vector3d origin;
		private Vector3d direction;
		private double t = -1;
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
		public Vector3d center;
		public double radius;

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
		public Vector3d normal;
		public double distance;

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
