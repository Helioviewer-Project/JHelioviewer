package org.helioviewer.jhv.opengl.raytrace;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class RayTrace {
	public enum HITPOINT_TYPE{
		SPHERE, PLANE;
	}
	
	private Camera camera;
	private Sphere sphere;
	private Plane plane;
	
	public RayTrace(Camera camera) {
		this.camera = camera;
		sphere = new Sphere(new Vector3d(0, 0, 0), Constants.SUN_RADIUS);
		plane = new Plane(new Vector3d(1, 0, 0).cross(new Vector3d(0, 1, 0)), 0);
	}
	
	public Ray cast(int x, int y){
		Vector3d origin = camera.getTransformation().multiply(new Vector3d(0, 0, 1));
		double newX = (x-GuiState3DWCS.mainComponentView.getComponent().getSize().getWidth()/2.)/ GuiState3DWCS.mainComponentView.getComponent().getSize().getWidth();
		double newY = (y-GuiState3DWCS.mainComponentView.getComponent().getSize().getHeight()/2.)/ GuiState3DWCS.mainComponentView.getComponent().getSize().getHeight();

		double width = Math.tan(Math.toRadians(camera.getFOV()));
		Vector3d direction = new Vector3d(-newX * 2 * width, newY * 2 * width, -1).normalize();
		Ray ray = new Ray(origin, direction);
		return intersect(ray);
	}
	
	public Vector2d castTexturepos(int x, int y, MetaData metaData){		
		Vector3d origin = camera.getTransformation().multiply(new Vector3d(0, 0, 1));
		
		double newX = (x-GuiState3DWCS.mainComponentView.getComponent().getSize().getWidth()/2.)/ GuiState3DWCS.mainComponentView.getComponent().getSize().getWidth();
		double newY = (y-GuiState3DWCS.mainComponentView.getComponent().getSize().getHeight()/2.)/ GuiState3DWCS.mainComponentView.getComponent().getSize().getHeight();
		double width = Math.tan(Math.toRadians(camera.getFOV()));
		Vector3d direction = new Vector3d(-newX * 2 * width, newY * 2 * width, -1).normalize();
		
		Vector4d tmpOrigin = new Vector4d(origin.x, origin.y, origin.z, 0);
		Vector4d tmpDirection = new Vector4d(direction.x, direction.y, direction.z, 0);
		
		Vector3d rayORot = camera.getTransformation().multiply(origin);
		Vector3d rayDRot = camera.getTransformation().multiply(direction);
		
		Vector4d rayORot1 = camera.getTransformation().multiply(tmpOrigin);
		Vector4d rayDRot1 = camera.getTransformation().multiply(tmpDirection);
		
		rayORot = new Vector3d(rayORot1.x, rayORot1.y, rayORot1.z);
		rayDRot = new Vector3d(rayDRot1.x, rayDRot1.y, rayDRot1.z);
		//plane.normal = camera.getTransformation().multiply(plane.normal);
		Ray rayOriginal = new Ray(origin, direction);
		Ray ray = new Ray(rayORot, rayDRot);
		ray = intersect(ray);
		rayOriginal.t = ray.t;
		if (ray.hitpointType == HITPOINT_TYPE.SPHERE && ray.getHitpoint().z < 0){
			return null;
		}
		Vector3d original = ray.getHitpoint();
		double imageX = (Math.max(Math.min(original.x, metaData.getPhysicalUpperRight().x), metaData.getPhysicalLowerLeft().x) - metaData.getPhysicalLowerLeft().x) / metaData.getPhysicalImageWidth();
		double imageY = (Math.max(Math.min(original.y, metaData.getPhysicalUpperRight().y), metaData.getPhysicalLowerLeft().y) - metaData.getPhysicalLowerLeft().y) / metaData.getPhysicalImageHeight();
		return new Vector2d(imageX, imageY);
	}
	
	private Ray intersect(Ray ray){
		double tSphere = sphere.intersect(ray);
		double tPlane = plane.intersect(ray);
		if (tSphere > 0){
			ray.hitpointType = HITPOINT_TYPE.SPHERE;
			ray.t = tSphere;
		}
		if (tPlane > 0.0 && (tPlane < tSphere || tSphere < 0.)){
			ray.hitpointType = HITPOINT_TYPE.PLANE;
			ray.t = tPlane;
		}
		return ray;
	}
	
	public class Ray{
		public Vector3d origin;
		public Vector3d direction;
		public double t = -1;
		public HITPOINT_TYPE hitpointType;
		
		public Ray(Vector3d origin, Vector3d direction) {
			this.origin = origin;
			this.direction = direction;
		}
		
		public Vector3d getHitpoint(){
			return this.origin.add(this.direction.scale(this.t));
		}
		
		@Override
		public String toString() {
			return getHitpoint() + "";
		}
	}
	
	private class Sphere{
		public Vector3d center;
		public double radius;
		
		public Sphere(Vector3d center, double radius) {
			this.center = center;
			this.radius = radius;
		}
		
		public double intersect(Ray ray){
			double t = -1;
			Vector3d oc = ray.origin.subtract(this.center);
			double b = 2 * oc.dot(ray.direction);
			double c = oc.dot(oc) - this.radius * this.radius;
			double determinant = (b * b)  - (4 * c);
			if (determinant >= 0)
				t = (-b - Math.sqrt(determinant))/2.0;
			return t;
		}
	}
	
	private class Plane{
		public Vector3d normal;
		public double distance;

		public Plane(Vector3d normal, double distance) {
			this.normal = normal;
			this.distance = distance;
		}
		
		public double intersect(Ray ray){
			return -(this.distance + ray.origin.dot(this.normal)) / ray.direction.dot(this.normal);
		}
	}
}
