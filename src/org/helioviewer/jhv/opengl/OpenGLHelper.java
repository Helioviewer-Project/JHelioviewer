package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imageformat.ImageFormat;
import org.helioviewer.jhv.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.jhv.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.jhv.viewmodel.imagetransport.Short16ImageTransport;
import org.helioviewer.jhv.viewmodel.view.opengl.GLTextureHelper;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class OpenGLHelper {
	public static GLContext glContext;
	public int textureID;
	public int textureWidth = 0;
	public int textureHeight = 0;
	
	public static int nextPowerOfTwo(int input) {
		int output = 1;
		while (output < input) {
			output <<= 1;
		}
		return output;
	}
	
	public int createTextureID(){
		glContext.makeCurrent();
		GL2 gl = glContext.getGL().getGL2();
		int tmp[] = new int[1];
		gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, tmp, 0);
		gl.glGenTextures(1, tmp, 0);
		this.textureID = tmp[0];
		return tmp[0];
	}
	
	public void bindBufferedImageToGLTexture(BufferedImage bufferedImage){
		int width2 = nextPowerOfTwo(bufferedImage.getWidth());
		int height2 = nextPowerOfTwo(bufferedImage.getHeight());
		
		if (this.textureHeight != height2 && this.textureWidth != width2){
			createTexture(bufferedImage, width2, height2);
		}

		updateTexture(bufferedImage);
	}
	
	public void bindBufferedImageToGLTexture(BufferedImage bufferedImage, int width, int height){
		glContext.makeCurrent();
		int width2 = nextPowerOfTwo(width);
		int height2 = nextPowerOfTwo(height);
		
		if (this.textureHeight != height2 && this.textureWidth != width2){
			createTexture(bufferedImage, width2, height2);
		}

		updateTexture(bufferedImage);
	}
	

	private void createTexture(BufferedImage bufferedImage, int width, int height){
		GL2 gl = glContext.getGL().getGL2();
		
		int internalFormat = GL2.GL_RGB;
		int inputFormat = GL2.GL_RGB;
		int inputType = GL2.GL_UNSIGNED_BYTE;
		int bpp = 3;
		switch (bufferedImage.getType()) {
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
			new NotImplementedException();
			break;
		}
				
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, textureID);
		
		ByteBuffer b = ByteBuffer.allocate(width*height*bpp);
		b.limit(width*height*bpp);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width,
				height, 0, inputFormat, inputType, b);

	}
	
	private void updateTexture(BufferedImage bufferedImage){
		GL2 gl = glContext.getGL().getGL2();
		boolean alpha = false;
		boolean switchChannel = false;
		int inputFormat = GL2.GL_RGB;
		int inputType = GL2.GL_UNSIGNED_BYTE;
		switch (bufferedImage.getType()) {
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
			new NotImplementedException();
			break;
		}
		
		ByteBuffer buffer = readPixels(bufferedImage, alpha, switchChannel);
		
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, this.textureID);
		
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
				inputFormat, inputType, buffer);

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
				GL2.GL_CLAMP);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
				GL2.GL_CLAMP);
	}
	
	
	public void bindLayerToGLTexture(Layer layer){
		glContext.makeCurrent();
		ImageData imageData = layer.getJhvjpxView().getImageData();
		int width2 = nextPowerOfTwo(imageData.getWidth());
		int height2 = nextPowerOfTwo(imageData.getHeight());
		if (this.textureHeight != height2 || this.textureWidth != width2){
			this.createTexture(layer, width2, height2);
		}
		updateTexture(layer);
	}
	
	private void createTexture(Layer layer, int width, int height){
		GL2 gl = glContext.getGL().getGL2();
		ImageData imageData = layer.getJhvjpxView().getImageData();
		int bitsPerPixel = imageData.getImageTransport().getNumBitsPerPixel();

		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, bitsPerPixel >> 3);

		ImageFormat imageFormat = imageData.getImageFormat();
		int internalFormat = GLTextureHelper.mapImageFormatToInternalGLFormat(imageFormat);
		int inputFormat = GLTextureHelper.mapImageFormatToInputGLFormat(imageFormat);
		int inputType = GLTextureHelper.mapBitsPerPixelToGLType(bitsPerPixel);
		
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, layer.getTexture());
		
		int bpp = OpenGLHelper.getBitsPerPixel(inputFormat, inputType);
		ByteBuffer b = ByteBuffer.allocate(width * height * bpp);
		b.limit(width * height * bpp);

		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width,
				height, 0, inputFormat, inputType, b);


		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
				GL2.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
				GL2.GL_CLAMP_TO_BORDER);	
	}
	
	private void updateTexture(Layer layer){
		GL2 gl = OpenGLHelper.glContext.getGL().getGL2();
		ImageData imageData = layer.getJhvjpxView().getImageData();
		int bitsPerPixel = imageData.getImageTransport().getNumBitsPerPixel();
		Buffer buffer;

		switch (bitsPerPixel) {
		case 8:
			buffer = ByteBuffer.wrap(((Byte8ImageTransport) imageData
					.getImageTransport()).getByte8PixelData());
			break;
		case 16:
			buffer = ShortBuffer.wrap(((Short16ImageTransport) imageData
					.getImageTransport()).getShort16PixelData());
			break;
		case 32:
			buffer = IntBuffer.wrap(((Int32ImageTransport) imageData
					.getImageTransport()).getInt32PixelData());
			break;
		default:
			buffer = null;
		}
				
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ROW_LENGTH, 0);
		gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, bitsPerPixel >> 3);

		ImageFormat imageFormat = imageData.getImageFormat();
		int inputFormat = GLTextureHelper.mapImageFormatToInputGLFormat(imageFormat);
		int width = imageData.getWidth();
		int height = imageData.getHeight();
		int inputType = GLTextureHelper.mapBitsPerPixelToGLType(bitsPerPixel);
		
		gl.glBindTexture(GL2.GL_TEXTURE_2D, layer.getTexture());
		
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, width, height,
					inputFormat, inputType, buffer);

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
				GL2.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
				GL2.GL_CLAMP_TO_BORDER);
	}

	public static int getBitsPerPixel(int inputFormat, int inputType){
		int bpp = 3;

		switch (inputFormat) {
		case GL2.GL_LUMINANCE:
		case GL2.GL_ALPHA:
			bpp = 1;
			break;
		case GL2.GL_LUMINANCE_ALPHA:
			bpp = 2;
			break;
		case GL2.GL_RGB:
			bpp = 3;
			break;
		case GL2.GL_RGBA:
		case GL2.GL_BGRA:
			bpp = 4;
			break;

		default:
			throw new RuntimeException("" + inputFormat);
		}

		switch (inputType) {
		case GL2.GL_UNSIGNED_BYTE:
			bpp *= 1;
			break;
		case GL2.GL_UNSIGNED_SHORT:
		case GL2.GL_UNSIGNED_SHORT_5_6_5:
		case GL2.GL_UNSIGNED_SHORT_4_4_4_4:
		case GL2.GL_UNSIGNED_SHORT_5_5_5_1:
			bpp *= 2;
			break;
		case GL2.GL_UNSIGNED_INT_8_8_8_8_REV:
			bpp *= 4;
			break;
		default:
			throw new RuntimeException("" + inputType);
		}

	return bpp;
	}

	public static ByteBuffer readPixels(BufferedImage image, boolean storeAlphaChannel, boolean switchRandBChannel) {
		int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        int factor = storeAlphaChannel ? 4 : 3;
        ByteBuffer buffer = ByteBuffer.allocate(image.getWidth() * image.getHeight() * factor); //4 for RGBA, 3 for RGB
        
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
}
