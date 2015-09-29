package org.helioviewer.jhv.opengl;

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

	public Texture()
	{
		GL2 gl = GLContext.getCurrentGL().getGL2();
		int tmp[] = new int[1];
		gl.glGenTextures(1, tmp, 0);
		
		openGLTextureId = tmp[0];
		width=0;
		height=0;
	}

	public void upload(Object _source, LocalDateTime _dateTime, ImageRegion _imageRegion, final ByteBuffer _image, int _imageWidth, int _imageHeight)
	{
		source = _source;
		dateTime = _dateTime;
		imageRegion = _imageRegion;
		
		int width2 = MathUtils.nextPowerOfTwo(_imageWidth);
		int height2 = MathUtils.nextPowerOfTwo(_imageHeight);
		
		if (height < height2 || width < width2)
		{
			System.out.println("Recreating texture "+width+"x"+height+" --> "+width2+"x"+height2);
			allocateTexture(width2, height2);
		}

		GL2 gl = GLContext.getCurrentGL().getGL2();
		
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 8 >> 3);

		gl.glBindTexture(GL2.GL_TEXTURE_2D, openGLTextureId);
		
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
				&& imageRegion.contains(_imageRegion)
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
	
	
	public void allocateTexture(int _width, int _height)
	{
		width = _width;
		height = _height;
		
		GL2 gl = GLContext.getCurrentGL().getGL2();
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, openGLTextureId);
		
		ByteBuffer b = ByteBuffer.allocate(width * height);
		b.limit(width * height);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE8, width, height, 0, GL2.GL_LUMINANCE, GL2.GL_UNSIGNED_BYTE, b);

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		
		//doesn't help anyways, because the image won't always fill the POT texture
		//gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
		//gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
	}
}