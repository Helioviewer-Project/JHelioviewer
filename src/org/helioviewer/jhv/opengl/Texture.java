package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Globals;
import org.helioviewer.jhv.base.ImageRegion;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.TimeLine;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

public class Texture
{
	private @Nullable Object source;
	private @Nullable LocalDateTime dateTime;
	protected @Nullable ImageRegion imageRegion;
	public final int openGLTextureId;
	public int width;
	public int height;
	
	public float textureWidth=1; //always <=1
	public float textureHeight=1; //always <=1
	
	private int internalFormat=GL.GL_LUMINANCE8;

	/*
	private JFrame debug=new JFrame();
	private JLabel debugImage=new JLabel();
	*/
	
	public Texture()
	{
		GL2 gl = GLContext.getCurrentGL().getGL2();
		int tmp[] = new int[1];
		gl.glGenTextures(1, tmp, 0);
		
		openGLTextureId = tmp[0];
		width=0;
		height=0;
		
		/*
		debug.setTitle(openGLTextureId+"");
		debug.setSize(200, 200);
		debug.setVisible(true);
		debug.getContentPane().add(debugImage);
		*/
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
		
		textureWidth = bufferedImage.getWidth() / (float)width;
		textureHeight = bufferedImage.getHeight() / (float)height;
		
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
		
		//updateDebugImage();
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
		
		if (width < width2 || height < height2 || internalFormat!=GL.GL_LUMINANCE8)
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

		/*ByteBuffer b=ByteBuffer.allocateDirect(width*height*4);
		for(int i=0;i<width*height;i++)
		{
			b.put((byte)255);
			b.put((byte)(i % 2==0 ? 255 : 0));
			b.put((byte)((i/2) % 2==0 ? 255 : 0));
			b.put((byte)((i/4) % 2==0 ? 255 : 0));
		}
		b.flip();
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, width, height, GL2.GL_ABGR_EXT, GL2.GL_UNSIGNED_BYTE, b);*/
		
		//FIXME: crashes with LASCO C2
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, _imageWidth, _imageHeight, /*GL2.GL_ABGR_EXT*/ GL2.GL_RED, GL2.GL_UNSIGNED_BYTE, _image);
		
		textureWidth=_imageWidth / (float)width;
		textureHeight=_imageHeight / (float)height;
		
		//updateDebugImage();
	}
	
	/*private void updateDebugImage()
	{
		GL2 gl = GLContext.getCurrentGL().getGL2();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, openGLTextureId);
		
		ByteBuffer buf=ByteBuffer.allocateDirect(width*height*4);
		gl.glGetTexImage(GL2.GL_TEXTURE_2D, 0, GL2.GL_ABGR_EXT, GL2.GL_UNSIGNED_BYTE, buf);
		
		BufferedImage bi=new BufferedImage(width/2, height/2, BufferedImage.TYPE_4BYTE_ABGR);
		int i=0;
		for(int y=0;y<height/2;y++)
		{
			buf.position(y*2*width*4);
			for(int x=0;x<width/2;x++)
			{
				bi.getRaster().getDataBuffer().setElem(i++, buf.get());
				bi.getRaster().getDataBuffer().setElem(i++, buf.get());
				bi.getRaster().getDataBuffer().setElem(i++, buf.get());
				bi.getRaster().getDataBuffer().setElem(i++, buf.get());
				buf.get();buf.get();buf.get();buf.get();
			}
		}
		
		
		Graphics g=bi.getGraphics();
		g.setColor(Color.GREEN);
		g.fillOval((int)(textureWidth*width/2)-3, (int)(textureHeight*height/2)-3, 5, 5);
		
		if(imageRegion!=null)
			g.drawRect(
					(int)(imageRegion.areaOfSourceImage.getX()*width/2), 
					(int)(imageRegion.areaOfSourceImage.getY()*height/2),
					(int)(imageRegion.areaOfSourceImage.getWidth()*width/2),
					(int)(imageRegion.areaOfSourceImage.getHeight()*height/2)
					);
		
		debug.setTitle(openGLTextureId+" "+System.currentTimeMillis());
		
		debugImage.setIcon(new ImageIcon(bi));
		debug.setSize(Math.max(width/2+20,500), height/2+50);
	}*/

	@SuppressWarnings("null")
	public boolean contains(Object _source, ImageRegion _imageRegion, LocalDateTime _localDateTime)
	{
		if(!Globals.isReleaseVersion())
			if(_source!=null && source!=null && _source.getClass()!=source.getClass())
				throw new RuntimeException("Comparing two different SOURCE classes.");
		
		return imageRegion != null
				&& source == _source
				&& imageRegion.areaOfSourceImage.contains(_imageRegion.areaOfSourceImage)
				&& imageRegion.decodeZoomFactor >= _imageRegion.decodeZoomFactor
				&& dateTime.isEqual(_localDateTime);
	}

	public boolean compareTexture(int _sourceId, LocalDateTime _ldt)
	{
		return imageRegion != null && _ldt.equals(dateTime);
	}

	public @Nullable ImageRegion getImageRegion()
	{
		return imageRegion;
	}

	public void invalidate()
	{
		if(imageRegion==null)
			return;
		
		imageRegion=null;
		
		if (TimeLine.SINGLETON.getCurrentDateTime().equals(dateTime))
			MainFrame.SINGLETON.MAIN_PANEL.repaint();
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