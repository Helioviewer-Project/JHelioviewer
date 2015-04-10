package org.helioviewer.jhv.viewmodel.imagedata;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;

import org.helioviewer.jhv.viewmodel.imageformat.ARGB32ImageFormat;
import org.helioviewer.jhv.viewmodel.imageformat.ImageFormat;
import org.helioviewer.jhv.viewmodel.imagetransport.ImageTransport;
import org.helioviewer.jhv.viewmodel.imagetransport.Int32ImageTransport;

/**
 * Representation of image data in ARGB32 format.
 * 
 * <p>
 * The image data contains four channels (alpha, red, green, blue), each channel
 * has eight bits per pixel.
 * 
 * @author Ludwig Schmidt
 * @author Markus Langenberg
 * 
 */
public class ARGBInt32ImageData extends AbstractImageData {

    private static final ImageFormat FORMAT = new ARGB32ImageFormat();
    private Int32ImageTransport imageTransport;

    /**
     * Constructor, given an array as data source.
     * 
     * <p>
     * This constructor receives the raw data as a data source. If the caller
     * handles raw data as well, the use of this constructor is recommended.
     * <p>
     * The pixel data has to be given as a one-dimensional array containing the
     * pixel data line by line. Each array element represents one pixel.
     * 
     * @param newWidth
     *            width of the image
     * @param newHeight
     *            height of the image
     * @param newPixelData
     *            pixel data
     * @param newColorMask
     *            color mask of the image
     */
    public ARGBInt32ImageData(int newWidth, int newHeight, int[] newPixelData, ColorMask newColorMask) {
        super(newWidth, newHeight, newColorMask);
        imageTransport = new Int32ImageTransport(newPixelData);
    }

    /**
     * Constructor, given an array as data source.
     * 
     * <p>
     * This constructor receives the raw data as a data source. If the caller
     * handles raw data as well, the use of this constructor is recommended.
     * <p>
     * The pixel data has to be given as a one-dimensional array containing the
     * pixel data line by line. Each array element represents one pixel.
     * 
     * @param base
     *            original ImageData-object
     * @param newPixelData
     *            pixel data
     */
    public ARGBInt32ImageData(ImageData base, int[] newPixelData) {
        super(base);
        imageTransport = new Int32ImageTransport(newPixelData);
    }

    /**
     * Constructor, given an BufferedImage as data source.
     * 
     * <p>
     * This constructor receives a BufferedImage as data source. If the caller
     * operates on BufferedImages as well, the use of this constructor is
     * recommended.
     * 
     * @param newImage
     *            pixel data
     * @param newColorMask
     *            color mask of the image
     */
    public ARGBInt32ImageData(BufferedImage newImage, ColorMask newColorMask) {
        super(newImage.getWidth(), newImage.getHeight(), newColorMask);
        readImageTransportFromBufferedImage(newImage);
    }

    /**
     * Constructor, given an BufferedImage as data source.
     * 
     * <p>
     * This constructor receives a BufferedImage as data source. If the caller
     * operates on BufferedImages as well, the use of this constructor is
     * recommended.
     * 
     * @param base
     *            original ImageData-object
     * @param newImage
     *            pixel data
     */
    public ARGBInt32ImageData(ImageData base, BufferedImage newImage) {
        super(base);
        readImageTransportFromBufferedImage(newImage);
    }

    /**
     * Internal function to extract the ImageTransport-Object from the given
     * BufferedImage.
     * 
     * @param newImage
     *            source image
     */
    private void readImageTransportFromBufferedImage(BufferedImage newImage) {
        image = newImage;

        DataBuffer dataBuffer = newImage.getRaster().getDataBuffer();

        if (dataBuffer instanceof DataBufferInt) {
            imageTransport = new Int32ImageTransport(((DataBufferInt) newImage.getRaster().getDataBuffer()).getData());

        } else if (dataBuffer instanceof DataBufferByte) {
            byte[] inputData = ((DataBufferByte) dataBuffer).getData();
            int[] outputData = new int[width * height];

            int bytesPerPixel = inputData.length / (width * height);

            for (int i = 0; i < width * height; i++) {

                outputData[i] = (inputData[i * bytesPerPixel] & 0xFF);

                for (int j = 1; j < bytesPerPixel; j++) {
                    outputData[i] |= (inputData[i * bytesPerPixel + j] & 0xFF) << (j * 8);
                }

                if (bytesPerPixel < 4) {
                    outputData[i] |= 0xFF000000;
                }
            }

            imageTransport = new Int32ImageTransport(outputData);

        } else {
            System.err.println("Unknown DataBuffer: " + dataBuffer);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ImageFormat getImageFormat() {
        return FORMAT;
    }

    /**
     * {@inheritDoc}
     */
    public ImageTransport getImageTransport() {
        return imageTransport;
    }

    /**
     * {@inheritDoc}
     */
    protected BufferedImage createBufferedImageFromImageTransport() {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        newImage.setRGB(0, 0, width, height, imageTransport.getInt32PixelData(), 0, width);
        return newImage;
    }
}
