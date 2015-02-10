package org.helioviewer.jhv.opengl;

import java.awt.image.BufferedImage;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.jhv.layers.Layer;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imageformat.ImageFormat;
import org.helioviewer.jhv.viewmodel.imagetransport.Byte8ImageTransport;
import org.helioviewer.jhv.viewmodel.imagetransport.Int32ImageTransport;
import org.helioviewer.jhv.viewmodel.imagetransport.Short16ImageTransport;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.opengl.GLTextureHelper;

public class OpenGLHelper {

	
	private ByteBuffer readPixels(BufferedImage image, boolean storeAlphaChannel) {
		int[] pixels = new int[image.getWidth() * image.getHeight()];
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());

        ByteBuffer buffer = ByteBuffer.allocate(image.getWidth() * image.getHeight() * 3); //4 for RGBA, 3 for RGB
        
        for(int y = 0; y < image.getHeight(); y++){
            for(int x = 0; x < image.getWidth(); x++){
                int pixel = pixels[y * image.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF));     
                buffer.put((byte) ((pixel >> 8) & 0xFF));      
                buffer.put((byte) (pixel & 0xFF));              
            }
        }

        buffer.flip(); 
        
	    return buffer;
	}

	private void createTexture(GL2 gl, Layer layer){
		ImageData imageData = layer.getJhvjpxView().getImageData();
		Region region = layer.getJhvjpxView().getMetaData().getPhysicalRegion();
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

		OpenGLHelper.checkGLErrors(gl, this + ".afterPixelStore");
		ImageFormat imageFormat = imageData.getImageFormat();
		int internalFormat = GLTextureHelper.mapImageFormatToInternalGLFormat(imageFormat);
		int inputFormat = GLTextureHelper.mapImageFormatToInputGLFormat(imageFormat);
		int width = imageData.getWidth();
		int height = imageData.getHeight();
		int inputType = GLTextureHelper.mapBitsPerPixelToGLType(bitsPerPixel);
		
		gl.glBindTexture(GL2.GL_TEXTURE_2D, layer.texture);
		
		int width2 = nextPowerOfTwo(width);
		int height2 = nextPowerOfTwo(height);

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

		ByteBuffer b = ByteBuffer.allocate(width2 * height2 * bpp);
		b.limit(width2 * height2 * bpp);

		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, internalFormat, width2,
				height2, 0, inputFormat, inputType, b);

		OpenGLHelper.checkGLErrors(gl, this + ".glTexImage2d");
		if (buffer != null) {
			gl.glTexSubImage2D(GL.GL_TEXTURE_2D, 0, 0, 0, width, height,
					inputFormat, inputType, buffer);
		}
		OpenGLHelper.checkGLErrors(gl, this + ".glTexSubImage2d");

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
				GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
				GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S,
				GL2.GL_CLAMP_TO_BORDER);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T,
				GL2.GL_CLAMP_TO_BORDER);		
	}
	
	public int nextPowerOfTwo(int input) {
		int output = 1;
		while (output < input) {
			output <<= 1;
		}
		return output;
	}
	
	public static boolean checkGLErrors(GL gl, String message) {
		if (gl == null) {
			System.out.println("OpenGL not yet Initialised!");
			return true;
		}
		int glErrorCode = gl.glGetError();

		if (glErrorCode != GL.GL_NO_ERROR) {
			GLU glu = new GLU();
			System.err.println("GL Error (" + glErrorCode + "): "
            + glu.gluErrorString(glErrorCode) + " - @" + message);
			if (glErrorCode == GL.GL_INVALID_OPERATION) {
				// Find the error position
				int[] err = new int[1];
				gl.glGetIntegerv(GL2.GL_PROGRAM_ERROR_POSITION_ARB, err, 0);
				if (err[0] >= 0) {
					String error = gl
							.glGetString(GL2.GL_PROGRAM_ERROR_STRING_ARB);
					System.err.println("GL error at " + err[0] + ":\n" + error);
				}
			}
			return true;
		} else {
			return false;
		}
	}
}
