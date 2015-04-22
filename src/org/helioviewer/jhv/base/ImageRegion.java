package org.helioviewer.jhv.base;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.opengl.OpenGLHelper;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;

public class ImageRegion {
	
	
	private Rectangle2D imageData;
	
	private Rectangle imageSize;
	
	private double absolutScaleFactor;
	private double relativeScaleFactor;
	
	public ImageRegion() {
		imageData = new Rectangle();
	}
			
	public void setImageData(Rectangle2D imageData){
		this.imageData = imageData;
	}
		
	public Vector2d calculateImageSize(LayerInterface layerInterface, Camera camera){
		MetaData metaData = layerInterface.getMetaData();
		Vector2i resolution = metaData.getResolution();
		
		double z = metaData.getPhysicalImageHeight() * Math.tan(Math.toRadians(camera.getFOV()));
		
		return Vector2d.scale(resolution, camera.getTranslation().z / z); 
	}
	
	public Rectangle2D getImageData(){
		return this.imageData;
	}

	public boolean contains(ImageRegion imageRegion) {
		return imageData.contains(imageData);
	}

	public double getScaleFactor(){
		return this.absolutScaleFactor;
	}
	
	public boolean compareScaleFactor(ImageRegion imageRegion) {
		return (this.absolutScaleFactor == imageRegion.getScaleFactor() || imageRegion.getScaleFactor() > 1);
	}

	public void calculateScaleFactor(LayerInterface layerInterface, Camera camera) {
		MetaData metaData = layerInterface.getMetaData();
		Vector2i resolution = metaData.getResolution();
		
		double z = metaData.getPhysicalImageHeight() * Math.tan(Math.toRadians(camera.getFOV()));
		
		this.absolutScaleFactor = z / camera.getTranslation().z;
		System.out.println("scaleFactor : " + absolutScaleFactor);
		calculateImageSize(layerInterface);
	}	
	
	private void calculateImageSize(LayerInterface layerInterface){
		MetaData metaData = layerInterface.getMetaData();
		Vector2i resolution = metaData.getResolution();
		int maxX = getNextInt(resolution.getX() * (imageData.getX() + imageData.getWidth()));
		int maxY = getNextInt(resolution.getY() * (imageData.getY() + imageData.getHeight()));
		int minX = (int)(resolution.getX() * imageData.getX());
		int minY = (int)(resolution.getY() * imageData.getY());
		int width = maxX - minX;
		int height = maxY - minY;
		System.out.println("width : " + width);
		System.out.println("height: " + height);
		System.out.println("zW : " + GuiState3DWCS.mainComponentView.getCanavasSize().getWidth() / width);
		System.out.println("zH : " + GuiState3DWCS.mainComponentView.getCanavasSize().getHeight() / height);
		width = width > GuiState3DWCS.mainComponentView.getComponent().getSurfaceWidth() ? GuiState3DWCS.mainComponentView.getComponent().getSurfaceWidth() : width; 
		height = height > GuiState3DWCS.mainComponentView.getComponent().getSurfaceHeight() ? GuiState3DWCS.mainComponentView.getComponent().getSurfaceHeight() : height;
		System.out.println("width : " + width);
		System.out.println("height: " + height);
		int width2 = OpenGLHelper.nextPowerOfTwo(width);
		int height2 = OpenGLHelper.nextPowerOfTwo(height);
		
		if (width2 >= resolution.getX() && height2 >= resolution.getY()){
			this.imageSize = new Rectangle(width2, height2);
			this.imageData = new Rectangle2D.Double(0, 0, 1, 1);
		}
		else {
			this.imageSize = new Rectangle(width, height);
		}
		System.out.println("imageSize : " + imageSize);
		
	}
	
	private static int getNextInt(double d){
		return d % 1 == 0 ? (int) d : (int)(d - d % 1) + 1;
	}
	
	public float getZoomFactor(){
		return nextZoomFraction(relativeScaleFactor * absolutScaleFactor);
	}
	
	public static float nextZoomFraction(double zoomFactor){
		int powerOfTwo = OpenGLHelper.nextPowerOfTwo(getNextInt((1/zoomFactor)));
		return 1/(float)powerOfTwo;
	}
	
	public static void main(String[] args) {
		double x = 0.30;
		System.out.println(nextZoomFraction(x));
	}
}
