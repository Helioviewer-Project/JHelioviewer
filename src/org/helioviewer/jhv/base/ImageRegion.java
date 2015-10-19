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
	// relative image coordinates (0..1, 0..1) within source image
	public final Rectangle2D areaOfSourceImage;
	
	// scaling factor of decoded image (e.g. 0.5 = 50% of original resolution)
	public final float decodeZoomFactor;
	
	// where this region can be found in the texture, in texel coordinates of the downscaled image
	public final Rectangle texels;
	
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

		// Get the image resolution
		Vector2i resolution = _metaData.getResolution();
		
		// Calculate the current texelCount
		int texelMinX = (int)(resolution.x * newX1);
		int texelMinY = (int)(resolution.y * newY1);
		int texelMaxX = (int)Math.ceil(resolution.x * newX2);
		int texelMaxY = (int)Math.ceil(resolution.y * newY2);
		double texelCount = (texelMaxX - texelMinX) * (texelMaxY - texelMinY);
		
		// Calculate the current pixelCount
		double z = _metaData.getPhysicalImageWidth() / Math.tan(Math.toRadians(MainPanel.FOV));
		double zoom = _zTranslation / z;
		int screenMaxX = (int)Math.ceil(_screenSize.getWidth() * newX2 / zoom);
		int screenMaxY = (int)Math.ceil(_screenSize.getHeight() * newY2 / zoom);
		int screenMinX = (int)(_screenSize.getWidth() * newX1 / zoom);
		int screenMinY = (int)(_screenSize.getHeight() * newY1 / zoom);
		double pixelCount = (screenMaxX - screenMinX) * (screenMaxY - screenMinY);
		
		// Calculate the imageScalefactors
		double imageZoomFactor = pixelCount >= texelCount ? 1 : Math.sqrt(pixelCount / texelCount);
		float candidateDecodeZoomFactor = imageZoomFactor >= 1.0 ? 1 : nextZoomFraction(imageZoomFactor);
		
		for(;;)
		{
			//openGL implementations are required to support at least 2048 x 2048 textures
			if((texelMaxX - texelMinX)<=2048 && (texelMaxY - texelMinY)<=2048)
				break;
			
			//TODO: implement properly by limiting the initial calculation --> no looping
			candidateDecodeZoomFactor *= 0.5;
			texelMinX = (int)(resolution.x * newX1 * candidateDecodeZoomFactor);
			texelMinY = (int)(resolution.y * newY1 * candidateDecodeZoomFactor);
			texelMaxX = (int)Math.ceil(resolution.x * newX2 * candidateDecodeZoomFactor);
			texelMaxY = (int)Math.ceil(resolution.y * newY2 * candidateDecodeZoomFactor);
		}
		decodeZoomFactor=candidateDecodeZoomFactor;
		
		
		texels = new Rectangle(texelMinX, texelMinY, texelMaxX - texelMinX, texelMaxY - texelMinY);
		
		//we need to inverse the texture coordinate, because the texture coordinates are
		//rounded to integer coordinates --> to be prices, the areaOfSourceImage should
		//be rounded to the same texels
		newX1=texelMinX/(double)resolution.x/decodeZoomFactor;
		newX2=texelMaxX/(double)resolution.x/decodeZoomFactor;
		newY1=texelMinY/(double)resolution.y/decodeZoomFactor;
		newY2=texelMaxY/(double)resolution.y/decodeZoomFactor;
		areaOfSourceImage=new Rectangle2D.Double(newX1,newY1,newX2-newX1,newY2-newY1);
	}

	private static float nextZoomFraction(double zoomFactor)
	{
		int powerOfTwo = MathUtils.nextPowerOfTwo((int)Math.ceil(1/zoomFactor));
		powerOfTwo >>= 1;
		return 1/(float)powerOfTwo;
	}
}
