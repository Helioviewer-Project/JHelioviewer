package org.helioviewer.jhv.viewmodel.view.fitsview;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;

import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.jhv.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.jhv.viewmodel.imagedata.ARGBInt32ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.ColorMask;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelByte8ImageData;
import org.helioviewer.jhv.viewmodel.imagedata.SingleChannelShortImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataFactory;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.region.StaticRegion;
import org.helioviewer.jhv.viewmodel.view.AbstractView;
import org.helioviewer.jhv.viewmodel.view.ImageInfoView;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.SubimageDataView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.viewport.StaticViewport;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;
import org.helioviewer.jhv.viewmodel.viewportimagesize.StaticViewportImageSize;
import org.helioviewer.jhv.viewmodel.viewportimagesize.ViewportImageSizeAdapter;

/**
 * Implementation of ImageInfoView for FITS images.
 * 
 * <p>
 * For further informations about the behavior of this view,
 * {@link ImageInfoView} is a good start to get into the concept.
 * 
 * @author Andreas Hoelzl
 * */
public class JHVFITSView extends AbstractView implements ViewportView, RegionView, SubimageDataView, ImageInfoView, MetaDataView {

    protected Viewport viewport;
    protected Region region;
    protected FITSImage fits;
    protected ImageData subImageData;
    protected MetaData m;
    private URI uri;

    /**
     * Constructor which loads a fits image from a given URI.
     * 
     * @param uri
     *            Specifies the location of the FITS file.
     * @throws IOException
     *             when an error occurred during reading the fits file.
     * */
    public JHVFITSView(URI uri) throws IOException {

        this.uri = uri;

        if (!uri.getScheme().equalsIgnoreCase("file"))
            throw new IOException("FITS does not support the " + uri.getScheme() + " protocol");

        try {
            fits = new FITSImage(uri.toURL().toString());
        } catch (Exception e) {
            throw new IOException("FITS image data cannot be accessed.");
        }

        initFITSImageView();
    }

    /**
     * Constructor which uses a given fits image.
     * 
     * @param fits
     *            FITSImage object which contains the image data
     * @param uri
     *            Specifies the location of the FITS file.
     * */
    public JHVFITSView(FITSImage fits, URI uri) {

        this.uri = uri;
        this.fits = fits;

        initFITSImageView();
    }

    /**
     * Initializes global variables.
     */
    private void initFITSImageView() {
    	m = MetaDataFactory.getMetaData(fits);
        //m = MetaDataConstructor.getMetaData(fits);

        BufferedImage bi = fits.getImage(0, 0, fits.getPixelHeight(), fits.getPixelWidth());

        if (bi.getColorModel().getPixelSize() <= 8) {
            subImageData = new SingleChannelByte8ImageData(bi, new ColorMask());
        } else if (bi.getColorModel().getPixelSize() <= 16) {
            subImageData = new SingleChannelShortImageData(bi.getColorModel().getPixelSize(), bi, new ColorMask());
        } else {
            subImageData = new ARGBInt32ImageData(bi, new ColorMask());
        }

        region = StaticRegion.createAdaptedRegion(m.getPhysicalLowerLeft().x, m.getPhysicalLowerLeft().y, m.getPhysicalImageSize().x, m.getPhysicalImageSize().y);

        viewport = StaticViewport.createAdaptedViewport(100, 100);
    }

    /**
     * Updates the sub image depending on the current region.
     * 
     * @param event
     *            Event that belongs to the request.
     * */
    private void updateImageData(ChangeEvent event) {
        Region r = region;

        m = getMetaData();

        double imageMeterPerPixel = m.getPhysicalImageWidth() / fits.getPixelWidth();
        long imageWidth = Math.round(r.getWidth() / imageMeterPerPixel);
        long imageHeight = Math.round(r.getHeight() / imageMeterPerPixel);

        Vector2i imagePostion = ViewHelper.calculateInnerViewportOffset(r, m.getPhysicalRegion(), new ViewportImageSizeAdapter(new StaticViewportImageSize(fits.getPixelWidth(), fits.getPixelHeight())));

        BufferedImage bi = fits.getImage(imagePostion.getX(), imagePostion.getY(), (int) imageHeight, (int) imageWidth);

        if (bi.getColorModel().getPixelSize() <= 8) {
            subImageData = new SingleChannelByte8ImageData(bi, new ColorMask());
        } else if (bi.getColorModel().getPixelSize() <= 16) {
            subImageData = new SingleChannelShortImageData(bi.getColorModel().getPixelSize(), bi, new ColorMask());
        } else {
            subImageData = new ARGBInt32ImageData(bi, new ColorMask());
        }

        event.addReason(new SubImageDataChangedReason(this));
        notifyViewListeners(event);
    }

    /**
     * {@inheritDoc}
     * */
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * {@inheritDoc}
     * */
    public boolean setViewport(Viewport v, ChangeEvent event) {

        // check if viewport has changed
        if (viewport != null && v != null && viewport.getWidth() == v.getWidth() && viewport.getHeight() == v.getHeight())
            return false;

        viewport = v;
        event.addReason(new ViewportChangedReason(this, v));
        notifyViewListeners(event);

        return true;
    }

    /**
     * {@inheritDoc}
     * */
    @SuppressWarnings("unchecked")
    public <T extends View> T getAdapter(Class<T> c) {
        if (c.isInstance(this)) {
            return (T) this;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * */
    public Region getLastDecodedRegion() {

        return region;
    }

    /**
     * {@inheritDoc}
     * */
    public boolean setRegion(Region r, ChangeEvent event) {

        event.addReason(new RegionUpdatedReason(this, r));

        // check if region has changed
        if ((region == r) || (region != null && r != null && region.getCornerX() == r.getCornerX() && region.getCornerY() == r.getCornerY() && region.getWidth() == r.getWidth() && region.getHeight() == r.getHeight()))
            return false;

        region = r;
        event.addReason(new RegionChangedReason(this, r));
        updateImageData(event);

        return true;
    }

    /**
     * Returns the header information as XML string.
     * 
     * @return XML string including all header information.
     * */
    public String getHeaderAsXML() {
        return fits.getHeaderAsXML();
    }

    /**
     * {@inheritDoc}
     * */
    public MetaData getMetaData() {
        return m;
    }

    /**
     * {@inheritDoc}
     * */
    public ImageData getImageData() {
        return subImageData;
    }

    /**
     * Returns the FITS image managed by this class.
     * 
     * @return FITS image.
     */
    public FITSImage getFITSImage() {
        return fits;
    }

    /**
     * {@inheritDoc}
     * */
    public String getName() {
        return m.getFullName();
    }

    /**
     * {@inheritDoc}
     * */
    public URI getUri() {
        return uri;
    }

    /**
     * {@inheritDoc}
     * */
    public boolean isRemote() {
        return false;
    }

    public URI getDownloadURI() {
        return uri;
    }
}
