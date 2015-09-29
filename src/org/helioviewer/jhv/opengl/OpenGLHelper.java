package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.gui.MainPanel;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

//FIXME: merge with Texture class
public class OpenGLHelper
{
	private int textureID;
	private int textureWidth = 0;
	private int textureHeight = 0;
	
	private float scaleFactorHeight;
	private float scaleFactorWidth;

	public int createTextureID()
	{
		GL2 gl = GLContext.getCurrentGL().getGL2();
        int tmp[] = new int[1];
		gl.glGenTextures(1, tmp, 0);
		this.textureID = tmp[0];
		return tmp[0];
	}
	
	public void bindBufferedImageToGLTexture(final BufferedImage bufferedImage)
	{
		int width2 = MathUtils.nextPowerOfTwo(bufferedImage.getWidth());
		int height2 = MathUtils.nextPowerOfTwo(bufferedImage.getHeight());
		
		if (textureHeight != height2 && textureWidth != width2)
			createTexture(bufferedImage, width2, height2);
		
		textureHeight = height2;
		textureWidth = width2;

		updateTexture(bufferedImage,0,0);
	}
	
	public float getScaleFactorWidth(){
		return scaleFactorWidth;
	}
	
	public float getScaleFactorHeight(){
		return scaleFactorHeight;
	}
	
	public void bindBufferedImageToGLTexture(final BufferedImage bufferedImage, final int width, final int height){
				int width2 = MathUtils.nextPowerOfTwo(width);
				int height2 = MathUtils.nextPowerOfTwo(height);
				
				if (textureHeight != height2 && textureWidth != width2){
					createTexture(bufferedImage, width2, height2);
				}

				this.textureHeight = height2;
				this.textureWidth = width2;

				updateTexture(bufferedImage, 0, 0);		
	}
	
	public void bindBufferedImageToGLTexture(final BufferedImage bufferedImage, int xOffset, int yOffset, final int width, final int height){
		int width2 = MathUtils.nextPowerOfTwo(width);
		int height2 = MathUtils.nextPowerOfTwo(height);

		if (textureHeight != height2 && textureWidth != width2)
			createTexture(bufferedImage, width2, height2);

		this.textureHeight = height2;
		this.textureWidth = width2;
		this.scaleFactorWidth = bufferedImage.getWidth() / (float)width2;
		this.scaleFactorHeight = bufferedImage.getHeight() / (float)height2;
		updateTexture(bufferedImage, xOffset, yOffset);		
	}	
	

	private void createTexture(BufferedImage bufferedImage, int width, int height)
	{
		GL2 gl = GLContext.getCurrentGL().getGL2();
		
		int internalFormat = GL2.GL_RGB;
		int inputFormat = GL2.GL_RGB;
		int inputType = GL2.GL_UNSIGNED_BYTE;
		int bpp = 3;
		switch (bufferedImage.getType())
		{
			case BufferedImage.TYPE_INT_ARGB:
			case BufferedImage.TYPE_4BYTE_ABGR:
				inputFormat = GL2.GL_RGBA;
				internalFormat = GL2.GL_RGBA;
				bpp = 4;
				break;
				
			case BufferedImage.TYPE_3BYTE_BGR:
			case BufferedImage.TYPE_INT_BGR:			
				inputFormat = GL2.GL_RGB;
				internalFormat = GL2.GL_RGB;
				break;
			case BufferedImage.TYPE_INT_RGB:
				inputFormat = GL2.GL_RGB;
				internalFormat = GL2.GL_RGB;
				break;
	
			default:
				throw new RuntimeException("Unsupported image format: "+bufferedImage.getType());
		}
				
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textureID);
		
		ByteBuffer b = ByteBuffer.allocate(width*height*bpp);
		b.limit(width*height*bpp);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width,
				height, 0, inputFormat, inputType, b);
	}
	
	private void updateTexture(BufferedImage bufferedImage, int xOffset, int yOffset)
	{
		GL2 gl = GLContext.getCurrentGL().getGL2();
		boolean alpha = false;
		boolean switchChannel = false;
		int inputFormat = GL2.GL_RGB;
		int inputType = GL2.GL_UNSIGNED_BYTE;
		switch (bufferedImage.getType())
		{
			case BufferedImage.TYPE_4BYTE_ABGR:
				switchChannel = true;
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
		
		ByteBuffer buffer = readPixels(bufferedImage, alpha, switchChannel);
		
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, this.textureID);
		
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, xOffset, yOffset, bufferedImage.getWidth(), bufferedImage.getHeight(),
				inputFormat, inputType, buffer);

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
				GL2.GL_CLAMP);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
				GL2.GL_CLAMP);
	}
	
	private static ByteBuffer readPixels(BufferedImage image, boolean storeAlphaChannel, boolean switchRandBChannel) {
		int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        int factor = storeAlphaChannel ? 4 : 3;
        ByteBuffer buffer = ByteBuffer.allocateDirect(image.getWidth() * image.getHeight() * factor); //4 for RGBA, 3 for RGB
        
        for(int y = 0; y < image.getHeight(); y++){
            for(int x = 0; x < image.getWidth(); x++){
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> (switchRandBChannel?16:0)) & 0xFF));     
                buffer.put((byte) ((pixel >> 8) & 0xFF));      
                buffer.put((byte) ((pixel >> (switchRandBChannel?0:16)) & 0xFF));
                if (storeAlphaChannel){
                	buffer.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
        }
        buffer.flip(); 
        
	    return buffer;
	}
	
	@Nullable
	public static String loadShaderFromFile(String shaderName)
	{
		StringBuilder shaderCode = new StringBuilder();
		String line = null;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				MainPanel.class.getResourceAsStream(shaderName), StandardCharsets.UTF_8)))
		{
			while ((line = br.readLine()) != null)
				shaderCode.append(line + "\n");
			
			return shaderCode.toString();
		}
		catch (IOException e)
		{
			Telemetry.trackException(e);
		}
		return null;
	}
}
