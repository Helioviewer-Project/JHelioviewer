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
	
	public static int nextPowerOfTwo(int input) {
		int output = 1;
		while (output < input) {
			output <<= 1;
		}
		return output;
	}
	
	public static int createTexture(GL2 gl){
		int tmp[] = new int[1];
		gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, tmp, 0);
		gl.glGenTextures(1, tmp, 0);
		return tmp[0];
	}
	
	public static ByteBuffer readPixels(BufferedImage image, boolean storeAlphaChannel, boolean switchRandBChannel) {
		int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = ByteBuffer.allocate(image.getWidth() * image.getHeight() * 3); //4 for RGBA, 3 for RGB
        
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
	
	public static int createTexture(GL2 gl, BufferedImage bufferedImage){
		int tmp[] = new int[1];
		gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, tmp, 0);
		gl.glGenTextures(1, tmp, 0);

		boolean alpha = false;
		boolean switchChannel = false;
		int internalFormat = GL2.GL_RGB;
		int inputFormat = GL2.GL_RGB;
		int inputType = GL2.GL_UNSIGNED_BYTE;
		int bpp = 3;
		switch (bufferedImage.getType()) {
		case BufferedImage.TYPE_INT_ARGB:
			switchChannel = true;
		case BufferedImage.TYPE_4BYTE_ABGR:
			inputFormat = GL2.GL_RGBA;
			internalFormat = GL2.GL_RGBA;
			bpp = 4;
			alpha = true;			
			break;
			
		case BufferedImage.TYPE_3BYTE_BGR:
		case BufferedImage.TYPE_INT_BGR:			
			switchChannel = true;
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
		gl.glBindTexture(GL2.GL_TEXTURE_2D, tmp[0]);

		int width2 = nextPowerOfTwo(bufferedImage.getWidth());
		int height2 = nextPowerOfTwo(bufferedImage.getHeight());
		ByteBuffer b = ByteBuffer.allocate(width2*height2*bpp);
		b.limit(width2*height2*bpp);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width2,
				height2, 0, inputFormat, inputType, b);

		return tmp[0];
	}
	
	public static int createTexture(GL2 gl, BufferedImage bufferedImage, int width, int height){
		int tmp[] = new int[1];
		gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, tmp, 0);
		gl.glGenTextures(1, tmp, 0);

		boolean alpha = false;
		boolean switchChannel = false;
		int internalFormat = GL2.GL_RGB;
		int inputFormat = GL2.GL_RGB;
		int inputType = GL2.GL_UNSIGNED_BYTE;
		int bpp = 3;
		switch (bufferedImage.getType()) {
		case BufferedImage.TYPE_INT_ARGB:
			switchChannel = true;
		case BufferedImage.TYPE_4BYTE_ABGR:
			inputFormat = GL2.GL_RGBA;
			internalFormat = GL2.GL_RGBA;
			bpp = 4;
			alpha = true;			
			break;
			
		case BufferedImage.TYPE_3BYTE_BGR:
		case BufferedImage.TYPE_INT_BGR:			
			switchChannel = true;
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
		gl.glBindTexture(GL2.GL_TEXTURE_2D, tmp[0]);

		ByteBuffer b = ByteBuffer.allocate(width*height*bpp);
		b.limit(width*height*bpp);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width,
				height, 0, inputFormat, inputType, b);

		return tmp[0];
	}
	
	public static void updateTexture(GL2 gl, int texID, BufferedImage bufferedImage){
		boolean alpha = false;
		boolean switchChannel = false;
		int internalFormat = GL2.GL_RGB;
		int inputFormat = GL2.GL_RGB;
		int inputType = GL2.GL_UNSIGNED_BYTE;
		int bpp = 3;
		switch (bufferedImage.getType()) {
		case BufferedImage.TYPE_INT_ARGB:
			switchChannel = true;
		case BufferedImage.TYPE_4BYTE_ABGR:
			inputFormat = GL2.GL_RGBA;
			internalFormat = GL2.GL_RGBA;
			bpp = 4;
			alpha = true;			
			break;
			
		case BufferedImage.TYPE_3BYTE_BGR:
		case BufferedImage.TYPE_INT_BGR:			
			switchChannel = true;
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
		
		ByteBuffer buffer = readPixels(bufferedImage, alpha, switchChannel);
		
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
		
		gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
				inputFormat, inputType, buffer);

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
				GL2.GL_CLAMP);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
				GL2.GL_CLAMP);
	}
	/*
	public static int createTexture(GL2 gl, BufferedImage bufferedImage){
		try {
			int tmp[] = new int[1];
			gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, tmp, 0);
			gl.glGenTextures(1, tmp, 0);

			BufferedImage bufferedImage = ImageIO.read(CompenentView.class.getResourceAsStream(lutImageName));
			
			ByteBuffer buffer = readPixels(bufferedImage, false);

		
			gl.glEnable(GL2.GL_TEXTURE_2D);	
						
			gl.glBindTexture(GL2.GL_TEXTURE_2D, lutTexID);
			
			ByteBuffer b = ByteBuffer.allocate(256*256*3);
			b.limit(256*256*3);
			bufferedImage.getType();
			int internalFormat = GL2.GL_RGB;
			int inputFormat = GL2.GL_RGB;
			int inputType = GL2.GL_UNSIGNED_BYTE;
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, 256,
					256, 0, inputFormat, inputType, b);

			textureHelper.checkGLErrors(gl, this + ".glTexImage2d(LUT)");
			// Log.debug("GLTextureHelper.genTexture2D: Width="+width+", Height="+height+" Width2="+width2+", Height2="+height2);
			
			if (buffer != null) {
				gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
						inputFormat, inputType, buffer);
			}
			textureHelper.checkGLErrors(gl, this + ".glTexSubImage2d");
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
					GL2.GL_NEAREST);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
					GL2.GL_NEAREST);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
					GL2.GL_CLAMP);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
					GL2.GL_CLAMP);
			
			return tmp[0];
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	*/
	
	
	public static void createTexture(GL2 gl, Layer layer){
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
		int internalFormat = GLTextureHelper.mapImageFormatToInternalGLFormat(imageFormat);
		int inputFormat = GLTextureHelper.mapImageFormatToInputGLFormat(imageFormat);
		int width = imageData.getWidth();
		int height = imageData.getHeight();
		int inputType = GLTextureHelper.mapBitsPerPixelToGLType(bitsPerPixel);
		
		gl.glEnable(GL2.GL_TEXTURE_2D);			
		gl.glBindTexture(GL2.GL_TEXTURE_2D, layer.getTexture());
		
		int width2 = nextPowerOfTwo(width);
		int height2 = nextPowerOfTwo(height);
		int bpp = OpenGLHelper.getBitsPerPixel(inputFormat, inputType);
		ByteBuffer b = ByteBuffer.allocate(width2 * height2 * bpp);
		b.limit(width2 * height2 * bpp);

		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width2,
				height2, 0, inputFormat, inputType, b);


		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
				GL2.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
				GL2.GL_CLAMP_TO_BORDER);	
	}
	
	public static void updateTexture(GL2 gl, Layer layer){
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
		int internalFormat = GLTextureHelper.mapImageFormatToInternalGLFormat(imageFormat);
		int inputFormat = GLTextureHelper.mapImageFormatToInputGLFormat(imageFormat);
		int width = imageData.getWidth();
		int height = imageData.getHeight();
		int inputType = GLTextureHelper.mapBitsPerPixelToGLType(bitsPerPixel);
		
		gl.glBindTexture(GL2.GL_TEXTURE_2D, layer.getTexture());
		
		int width2 = nextPowerOfTwo(width);
		int height2 = nextPowerOfTwo(height);
		int bpp = OpenGLHelper.getBitsPerPixel(inputFormat, inputType);
		ByteBuffer b = ByteBuffer.allocate(width2 * height2 * bpp);
		b.limit(width2 * height2 * bpp);

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

	
}
