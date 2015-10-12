package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

import org.helioviewer.jhv.Globals;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.TimeLine;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

public class Texture
{
	private Object source;
	private LocalDateTime dateTime;
	protected ImageRegion imageRegion;
	public final int openGLTextureId;
	public int width;
	public int height;
	
	public float textureScaleX=1;
	public float textureScaleY=1;
	
	private int internalFormat=GL.GL_LUMINANCE8;

	public Texture()
	{
		GL2 gl = GLContext.getCurrentGL().getGL2();
		int tmp[] = new int[1];
		gl.glGenTextures(1, tmp, 0);
		
		openGLTextureId = tmp[0];
		width=0;
		height=0;
	}
	
	public void upload(final BufferedImage bufferedImage)
	{
		upload(bufferedImage, bufferedImage.getWidth(), bufferedImage.getHeight());
	}
	
	public void upload(final BufferedImage bufferedImage, final int _width, final int _height)
	{
		upload(bufferedImage,0,0,_width,_height);
	}
	
	public void upload(final BufferedImage bufferedImage, int _destX, int _destY, final int _width, final int _height)
	{
		int width2 = MathUtils.nextPowerOfTwo(_width);
		int height2 = MathUtils.nextPowerOfTwo(_height);

		if (height != height2 || width != width2 || internalFormat!=GL.GL_RGBA8)
			allocateTexture(width2, height2, GL.GL_RGBA8);
		
		height = height2;
		width = width2;
		textureScaleX = bufferedImage.getWidth() / (float)width2;
		textureScaleY = bufferedImage.getHeight() / (float)height2;

		boolean alpha = false;
		boolean switchChannel = false;
		int inputFormat = GL2.GL_RGB;
		int inputType = GL2.GL_UNSIGNED_BYTE;
		switch (bufferedImage.getType())
		{
			case BufferedImage.TYPE_4BYTE_ABGR:
				switchChannel = true;
				inputFormat = GL2.GL_RGBA;
				alpha = true;
				break;
			case BufferedImage.TYPE_INT_ARGB:
				inputFormat = GL2.GL_RGBA;
				alpha = true;
				break;
				
			case BufferedImage.TYPE_3BYTE_BGR:
			case BufferedImage.TYPE_INT_BGR:			
				switchChannel = true;
				inputFormat = GL2.GL_RGB;
				break;
			case BufferedImage.TYPE_INT_RGB:
				inputFormat = GL2.GL_RGB;
				break;
	
			default:
				throw new RuntimeException("Unsupported image format: "+bufferedImage.getType());
		}
		
		GL2 gl = GLContext.getCurrentGL().getGL2();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, openGLTextureId);
		
		ByteBuffer buffer = readPixels(bufferedImage, alpha, switchChannel);
		
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 8 >> 3);

		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, _destX, _destY, bufferedImage.getWidth(), bufferedImage.getHeight(), inputFormat, inputType, buffer);
	}
	
	private static ByteBuffer readPixels(BufferedImage image, boolean storeAlphaChannel, boolean switchRandBChannel)
	{
		int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        int factor = storeAlphaChannel ? 4 : 3;
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * factor); //4 for RGBA, 3 for RGB
        
        for(int y = 0; y < image.getHeight(); y++)
            for(int x = 0; x < image.getWidth(); x++)
            {
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> (switchRandBChannel?16:0)) & 0xFF));     
                buffer.put((byte) ((pixel >> 8) & 0xFF));      
                buffer.put((byte) ((pixel >> (switchRandBChannel?0:16)) & 0xFF));
                if (storeAlphaChannel)
                	buffer.put((byte) ((pixel >> 24) & 0xFF));
            }

        buffer.flip(); 
	    return buffer;
	}

	public void upload(Object _source, LocalDateTime _dateTime, ImageRegion _imageRegion, final ByteBuffer _image, int _imageWidth, int _imageHeight)
	{
		source = _source;
		dateTime = _dateTime;
		imageRegion = _imageRegion;
		
		int width2 = MathUtils.nextPowerOfTwo(_imageWidth);
		int height2 = MathUtils.nextPowerOfTwo(_imageHeight);
		
		if (height < height2 || width < width2 || internalFormat!=GL.GL_LUMINANCE8)
		{
			System.out.println("Recreating texture "+width+"x"+height+" --> "+width2+"x"+height2);
			allocateTexture(width2, height2, GL.GL_LUMINANCE8);
		}

		GL2 gl = GLContext.getCurrentGL().getGL2();
		
		gl.glBindTexture(GL2.GL_TEXTURE_2D, openGLTextureId);
		
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 8 >> 3);

		ByteBuffer b=ByteBuffer.allocateDirect(width2*height2);
		for(int i=0;i<width2*height2;i++)
			b.put((byte)255);
		b.flip();
		
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, width2, height2, GL2.GL_RED, GL2.GL_UNSIGNED_BYTE, _image);
		
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, _imageWidth, _imageHeight, GL2.GL_ABGR_EXT, GL2.GL_UNSIGNED_BYTE, _image);
		
		textureScaleX=width / (float)_imageWidth;
		textureScaleY=height / (float)_imageHeight;
	}
	
	public boolean contains(Object _source, ImageRegion _imageRegion, LocalDateTime _localDateTime)
	{
		if(!Globals.isReleaseVersion())
			if(_source!=null && source!=null && _source.getClass()!=source.getClass())
				throw new RuntimeException("Comparing two different SOURCE classes.");
		
		return imageRegion != null
				&& source == _source
				&& imageRegion.texels.contains(_imageRegion.texels)
				&& imageRegion.decodeZoomFactor >= _imageRegion.decodeZoomFactor
				&& dateTime.isEqual(_localDateTime);
	}

	public boolean compareTexture(int _sourceId, LocalDateTime _ldt)
	{
		return imageRegion != null && _ldt.equals(dateTime);
	}

	public ImageRegion getImageRegion()
	{
		return imageRegion;
	}

	public void invalidate()
	{
		if(imageRegion==null)
			return;
		
		imageRegion=null;
		
		if (TimeLine.SINGLETON.getCurrentDateTime().equals(dateTime))
			MainFrame.MAIN_PANEL.repaint();
	}
	
	
	public void allocateTexture(int _width, int _height, int _internalFormat)
	{
		width = _width;
		height = _height;
		internalFormat = _internalFormat;
		
		GL2 gl = GLContext.getCurrentGL().getGL2();
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, openGLTextureId);
		
		ByteBuffer b = ByteBuffer.allocate(width * height);
		b.limit(width * height);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width, height, 0, GL2.GL_LUMINANCE, GL2.GL_UNSIGNED_BYTE, b);

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
	}
}