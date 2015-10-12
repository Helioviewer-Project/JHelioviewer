package org.helioviewer.jhv.base;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class ImageRegion
{
	// relative image coordinates (0..1, 0..1) of source image
	public final Rectangle2D requiredOfSourceImage;
	
	// scaling factor of decoded image (e.g. 0.5 = 50% of original resolution)
	public final float decodeZoomFactor;
	
	// where this region can be found in the texture, in texel coordinates
	public final Rectangle texels;
	
	// where this region can be found in the texture, in 0..1 coordinates
	public final float texCoordX;
	public final float texCoordY;
	public final float texCoordWidth;
	public final float texCoordHeight;

	public ImageRegion(Rectangle2D _requiredOfSourceImage, double _zTranslation, MetaData _metaData, Dimension _screenSize)
	{
		this(_requiredOfSourceImage, _zTranslation, _metaData, _screenSize, 1.0);
	}
	
	public ImageRegion(Rectangle2D _requiredOfSourceImage, double _zTranslation, MetaData _metaData, Dimension _screenSize,double _safetyBorder)
	{
		double newX1=MathUtils.clip(_requiredOfSourceImage.getCenterX()-_requiredOfSourceImage.getWidth()*0.5*_safetyBorder, 0, 1);
		double newX2=MathUtils.clip(_requiredOfSourceImage.getCenterX()+_requiredOfSourceImage.getWidth()*0.5*_safetyBorder, 0, 1);
		double newY1=MathUtils.clip(_requiredOfSourceImage.getCenterY()-_requiredOfSourceImage.getHeight()*0.5*_safetyBorder, 0, 1);
		double newY2=MathUtils.clip(_requiredOfSourceImage.getCenterY()+_requiredOfSourceImage.getHeight()*0.5*_safetyBorder, 0, 1);

		requiredOfSourceImage=new Rectangle2D.Double(newX1,newY1,newX2-newX1,newY2-newY1);
		
		// Get the image resolution
		Vector2i resolution = _metaData.getResolution();
		
		// Calculate the current texelCount
		int texelMaxX = (int)Math.ceil(resolution.x * (requiredOfSourceImage.getX() + requiredOfSourceImage.getWidth()));
		int texelMaxY = (int)Math.ceil(resolution.y * (requiredOfSourceImage.getY() + requiredOfSourceImage.getHeight()));
		int texelMinX = (int)(resolution.x * requiredOfSourceImage.getX());
		int texelMinY = (int)(resolution.y * requiredOfSourceImage.getY());
		int textureWidth = texelMaxX - texelMinX;
		int textureHeight = texelMaxY - texelMinY;
		double texelCount = textureWidth * textureHeight;
		
		// Calculate the current pixelCount
		double z = _metaData.getPhysicalImageWidth() / Math.tan(Math.toRadians(MainPanel.FOV));
		double zoom = _zTranslation / z;
		int screenMaxX = (int)Math.ceil(_screenSize.getWidth() * (requiredOfSourceImage.getX() + requiredOfSourceImage.getWidth()) / zoom);
		int screenMaxY = (int)Math.ceil(_screenSize.getHeight() * (requiredOfSourceImage.getY() + requiredOfSourceImage.getHeight()) / zoom);
		int screenMinX = (int)(_screenSize.getWidth() * requiredOfSourceImage.getX() / zoom);
		int screenMinY = (int)(_screenSize.getHeight() * requiredOfSourceImage.getY() / zoom);
		int screenWidth = screenMaxX - screenMinX;
		int screenHeight = screenMaxY - screenMinY;
		double pixelCount = screenWidth * screenHeight;
		
		// Calculate the imageScalefactors
		double imageZoomFactor = pixelCount >= texelCount ? 1 : Math.sqrt(pixelCount / texelCount);
		float candidateDecodeZoomFactor = imageZoomFactor >= 1.0 ? 1 : nextZoomFraction(imageZoomFactor);
		
		for(;;)
		{
			texelMaxX = (int)Math.ceil(resolution.x * (requiredOfSourceImage.getX() + requiredOfSourceImage.getWidth()) * candidateDecodeZoomFactor);
			texelMaxY = (int)Math.ceil(resolution.y * (requiredOfSourceImage.getY() + requiredOfSourceImage.getHeight()) * candidateDecodeZoomFactor);
			texelMinX = (int)(resolution.x * requiredOfSourceImage.getX() * candidateDecodeZoomFactor);
			texelMinY = (int)(resolution.y * requiredOfSourceImage.getY() * candidateDecodeZoomFactor);
			textureWidth = texelMaxX - texelMinX;
			textureHeight = texelMaxY - texelMinY;
			
			//TODO: implement properly by limiting the initial calculation --> no looping
			if(textureWidth<=2048 && textureHeight<=2048)
				break;
			
			candidateDecodeZoomFactor *= 0.5;
		}
		decodeZoomFactor=candidateDecodeZoomFactor;
		
		texels = new Rectangle(texelMinX, texelMinY, textureWidth, textureHeight);
		texCoordX = texelMinX / (float)(resolution.x * decodeZoomFactor);
		texCoordY = texelMinY / (float)(resolution.y * decodeZoomFactor);
		texCoordWidth = textureWidth / (float)(resolution.x * decodeZoomFactor);
		texCoordHeight = textureHeight / (float)(resolution.y * decodeZoomFactor);
	}

	private static float nextZoomFraction(double zoomFactor)
	{
		int powerOfTwo = MathUtils.nextPowerOfTwo((int)Math.ceil(1/zoomFactor));
		powerOfTwo >>= 1;
		return 1/(float)powerOfTwo;
	}
}
