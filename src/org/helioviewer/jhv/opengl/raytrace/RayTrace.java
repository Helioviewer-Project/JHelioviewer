package org.helioviewer.jhv.opengl.raytrace;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.opengl.camera.CameraMode.MODE;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class RayTrace {
	public enum HitpointType{
		SPHERE, PLANE, SPHERE_PLANE;
	}
	
	private Sphere sphere;
	private Plane plane;
	
	public RayTrace() {
		sphere = new Sphere(new Vector3d(0, 0, 0), Constants.SUN_RADIUS);
		plane = new Plane(new Vector3d(1, 0, 0).cross(new Vector3d(0, 1, 0)), 0);
	}
	
	public Ray cast(int x, int y, MainPanel mainPanel){
		double newX = (x-mainPanel.getWidth()/2.)/ mainPanel.getWidth();
		double newY = (y-mainPanel.getHeight()/2.)/ mainPanel.getWidth();

		double width = Math.tan(Math.toRadians(MainPanel.FOV/2.0)) * 2;
		
		Vector3d origin;
		Vector3d direction;
		if (CameraMode.mode == MODE.MODE_3D){
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1));
			direction = new Vector3d(newX * width, newY * width, -1).normalize();
		}
		else {
			width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * mainPanel.getTranslation().z * 2;
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1)).add(new Vector3d(newX * width, newY * width, 0));
			direction = new Vector3d(0, 0, -1).normalize();
		}
		
		Ray ray = new Ray(origin, direction);
		return intersect(ray);
	}
	
	public Ray castScene(int x, int y, MainPanel mainPanel){
		double newX = (x-mainPanel.getWidth()/2.)/ mainPanel.getWidth();
		double newY = (y-mainPanel.getHeight()/2.)/ mainPanel.getWidth();

		double width = Math.tan(Math.toRadians(MainPanel.FOV/2.0)) * 2;
		
		Vector3d origin;
		Vector3d direction;
		if (CameraMode.mode == MODE.MODE_3D){
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1));
			direction = new Vector3d(newX * width, newY * width, -1).normalize();
		}
		else {
			width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * mainPanel.getTranslation().z * 2;
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1)).add(new Vector3d(newX * width, newY * width, 0));
			direction = new Vector3d(0, 0, -1).normalize();
		}
		
		Vector4d tmpOrigin = new Vector4d(origin.x, origin.y, origin.z, 0);
		Vector4d tmpDirection = new Vector4d(direction.x, direction.y, direction.z, 0);
		
		Vector3d rayORot = mainPanel.getTransformation().multiply(origin);
		Vector3d rayDRot = mainPanel.getTransformation().multiply(direction);
				
		Vector4d rayORot1 = mainPanel.getTransformation().multiply(tmpOrigin);
		Vector4d rayDRot1 = mainPanel.getTransformation().multiply(tmpDirection);
		
		rayORot = new Vector3d(rayORot1.x, rayORot1.y, rayORot1.z);
		rayDRot = new Vector3d(rayDRot1.x, rayDRot1.y, rayDRot1.z);
		//plane.normal = camera.getTransformation().multiply(plane.normal);
		Ray rayOriginal = new Ray(origin, direction);
		Ray ray = new Ray(rayORot, rayDRot);

		return intersect(ray);
		
	}
	
	public Vector2d castTexturepos(int x, int y, MetaData metaData, MainPanel mainPanel){		
		
		double newX = (x-mainPanel.getWidth()/2.)/ mainPanel.getWidth();
		double newY = (y-mainPanel.getHeight()/2.)/ mainPanel.getWidth();
		double width = Math.tan(Math.toRadians(MainPanel.FOV/2.0)) * 2;
		
		Vector3d origin;
		Vector3d direction;
		if (CameraMode.mode == MODE.MODE_3D){
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1));
			direction = new Vector3d(newX * width, newY * width, -1).normalize();
		}
		else {
			width = Math.tan(Math.toRadians(MainPanel.FOV / 2.0)) * mainPanel.getTranslation().z * 2.0;
			origin = mainPanel.getTransformation().multiply(new Vector3d(0, 0, 1)).add(new Vector3d(newX * width, newY * width, 0));
			direction = new Vector3d(0, 0, -1).normalize();
		}
		Vector4d tmpOrigin = new Vector4d(origin.x, origin.y, origin.z, 0);
		Vector4d tmpDirection = new Vector4d(direction.x, direction.y, direction.z, 0);
		
		Vector3d rayORot = mainPanel.getTransformation().multiply(origin);
		Vector3d rayDRot = mainPanel.getTransformation().multiply(direction);
				
		Vector4d rayORot1 = mainPanel.getTransformation().multiply(tmpOrigin);
		Vector4d rayDRot1 = mainPanel.getTransformation().multiply(tmpDirection);
		
		rayORot = new Vector3d(rayORot1.x, rayORot1.y, rayORot1.z);
		rayDRot = new Vector3d(rayDRot1.x, rayDRot1.y, rayDRot1.z);
		//plane.normal = camera.getTransformation().multiply(plane.normal);
		Ray rayOriginal = new Ray(origin, direction);
		Ray ray = new Ray(rayORot, rayDRot);
		ray = intersect(ray);
		rayOriginal.t = ray.t;
		if (ray.hitpointType == HitpointType.SPHERE && ray.getHitpoint().z < 0){
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
			ray.hitpointType = HitpointType.SPHERE;
			ray.tSphere = tSphere;
		}
		if (tPlane > 0.0 && tSphere < 0.){
			ray.hitpointType = HitpointType.PLANE;
			ray.tPlane = tPlane;
		}
		else if (tPlane > 0.0 && (tSphere < 0.)){
			ray.hitpointType = HitpointType.SPHERE_PLANE;
			ray.tPlane = tPlane;
		}
		return ray;
	}
	
	public static class Ray{
		private Vector3d origin;
		private Vector3d direction;
		private double t = -1;
		private double tSphere = -1;
		private double tPlane = -1;
		private HitpointType hitpointType;
		
		private Ray(Vector3d origin, Vector3d direction) {
			this.origin = origin;
			this.direction = direction;
		}
		
		public Vector3d getHitpoint(){
			if (this.hitpointType == HitpointType.SPHERE || this.hitpointType == HitpointType.SPHERE_PLANE){
				return this.getHitpointOnSphere();
			}
			else {
				return this.getHitpointOnPlane();
			}
		}
		
		public HitpointType getHitpointType(){
			return hitpointType;
		}
		
		public Vector3d getHitpointOnSphere(){
			return this.origin.add(this.direction.scale(this.tSphere));
		}
		
		public Vector3d getHitpointOnPlane(){
			return this.origin.add(this.direction.scale(this.tPlane));
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
