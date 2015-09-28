package org.helioviewer.jhv.base;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

public class ImageRegion
{
	// relative image coordinates (0..1, 0..1) of 
	private Rectangle2D imageData;
	
	// image size, which have been decoded
	private Rectangle imageSize;
	
	// downsampling factor of decoded image (e.g. 0.5 = 50% of original resolution)
	private float imageScaleFactor;
	
	private float textureOffsetX = 0;
	private float textureOffsetY = 0;
	private float textureScaleWidth = 1;
	private float textureScaleHeight = 1;

	private LocalDateTime localDateTime;
	
	private int textureID;
	
	public int textureWidth;
	public int textureHeight;

	private float xTextureScale = 1;
	private float yTextureScale = 1;
	
	public ImageRegion(LocalDateTime localDateTime)
	{
		imageData = new Rectangle();
		this.localDateTime = localDateTime;
	}
			
	public void setImageData(Rectangle2D imageData)
	{
		this.imageData = imageData;
	}
	
	public Rectangle2D getImageData(){
		return this.imageData;
	}

	/**
	 * Function to check, whether the last decoded ImageRegion contains the given ImageRegion
	 * @param imageRegion
	 * @return TRUE --> contain, else FALSE
	 */
	public boolean contains(ImageRegion imageRegion)
	{
		return imageSize.contains(imageRegion.imageSize);
	}

	/**
	 * Function to compare the Scalefactor of two ImageRegion
	 * @param imageRegion
	 * @return TRUE --> current scalefactor is equal or higher, else FALSE
	 */
	public boolean equalOrHigherResolution(ImageRegion imageRegion)
	{
		return imageScaleFactor >= imageRegion.imageScaleFactor;
	}

	public void calculateScaleFactor(AbstractImageLayer layerInterface, MainPanel mainPanel, MetaData metaData, Dimension size)
	{
		// Get the image resolution
		Vector2i resolution = metaData.getResolution();
		
		// Calculate the current texelCount
		int texelMaxX = (int)Math.ceil(resolution.x * (imageData.getX() + imageData.getWidth()));
		int texelMaxY = (int)Math.ceil(resolution.y * (imageData.getY() + imageData.getHeight()));
		int texelMinX = (int)(resolution.x * imageData.getX());
		int texelMinY = (int)(resolution.y * imageData.getY());
		int textureWidth = texelMaxX - texelMinX;
		int textureHeight = texelMaxY - texelMinY;
		double texelCount = textureWidth * textureHeight;
		
		// Calculate the current pixelCount
		double z = metaData.getPhysicalImageWidth() / Math.tan(Math.toRadians(MainPanel.FOV));
		double zoom = mainPanel.getTranslation().z / z;
		int screenMaxX = (int)Math.ceil(size.getWidth() * (imageData.getX() + imageData.getWidth()) /  zoom);
		int screenMaxY = (int)Math.ceil(size.getHeight() * (imageData.getY() + imageData.getHeight()) / zoom);
		int screenMinX = (int)(size.getWidth() * imageData.getX() / zoom);
		int screenMinY = (int)(size.getHeight() * imageData.getY() / zoom);
		int screenWidth = screenMaxX - screenMinX;
		int screenHeight = screenMaxY - screenMinY;
		double pixelCount = screenWidth * screenHeight;

		// Calculate the imageScalefactors
		double imageZoomFactor = pixelCount >= texelCount ? 1 : Math.sqrt((pixelCount / texelCount));
		imageScaleFactor = imageZoomFactor >= 1.0 ? 1 : nextZoomFraction(imageZoomFactor);
		
		imageSize = new Rectangle(texelMinX, texelMinY, textureWidth, textureHeight);
		
		calculateImageSize(resolution);
	}	
	
	public float getInTextureOffsetX()
	{
		return textureOffsetX;
	}
	
	public float getInTextureOffsetY(){
		return textureOffsetY;
	}
	
	public float getInTextureWidth()
	{
		return textureScaleWidth * xTextureScale;
	}
	
	public float getInTextureHeight()
	{
		return textureScaleHeight * yTextureScale;
	}
	
	/**
	 * Function to calculate the current image size
	 * @param _resolution
	 */
	private void calculateImageSize(Vector2i _resolution)
	{
		int textureMaxX = (int)Math.ceil(_resolution.x * (imageData.getX() + imageData.getWidth()) * imageScaleFactor);
		int textureMaxY = (int)Math.ceil(_resolution.y * (imageData.getY() + imageData.getHeight()) * imageScaleFactor);
		int textureMinX = (int)(_resolution.x * imageData.getX() * imageScaleFactor);
		int textureMinY = (int)(_resolution.y * imageData.getY() * imageScaleFactor);
		int textureWidth = textureMaxX - textureMinX;
		int textureHeight = textureMaxY - textureMinY;
		
		imageSize = new Rectangle(textureMinX, textureMinY, textureWidth, textureHeight);
		textureOffsetX = textureMinX / (float)(_resolution.x * imageScaleFactor);
		textureOffsetY = textureMinY / (float)(_resolution.y * imageScaleFactor);
		textureScaleWidth = imageSize.width / (float)(_resolution.x * imageScaleFactor);
		textureScaleHeight = imageSize.height / (float)(_resolution.y * imageScaleFactor);
	}
	
	public Rectangle getImageSize()
	{
		return imageSize;
	}
	
	public float getZoomFactor()
	{
		return imageScaleFactor;
	}
	
	private static float nextZoomFraction(double zoomFactor)
	{
		int powerOfTwo = MathUtils.nextPowerOfTwo((int)Math.ceil(1/zoomFactor));
		powerOfTwo >>= 1;
		return 1/(float)powerOfTwo;
	}
	
	public LocalDateTime getDateTime()
	{
		return localDateTime;
	}

	public void setLocalDateTime(LocalDateTime currentDateTime)
	{
		localDateTime = currentDateTime;
	}	
	
	public int getTextureID()
	{
		return textureID;
	}
	
	public void setOpenGLTextureId(int _textureID)
	{
		textureID = _textureID;
	}

	public void setTextureScaleFactor(float xScale, float yScale)
	{
		xTextureScale = xScale;
		yTextureScale = yScale;
	}
}
