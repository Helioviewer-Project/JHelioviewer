package org.helioviewer.jhv.base;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.helioviewer.jhv.base.math.Vector2d;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;

public class ImageRegion {
	
	
	private Rectangle2D imageData;
	
	private Rectangle imageSize;
	
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

	public boolean compareScaleFactor(ImageRegion imageRegion) {
		return false;
	}

	public void calculateScaleFactor(LayerInterface layerInterface, Camera camera) {
		MetaData metaData = layerInterface.getMetaData();
		Vector2i resolution = metaData.getResolution();
		
		double z = metaData.getPhysicalImageHeight() * Math.tan(Math.toRadians(camera.getFOV()));
		Vector2d newResolution = Vector2d.scale(resolution, camera.getTranslation().z / z);
		
		System.out.println("newResolution : " + newResolution);
	}
}
