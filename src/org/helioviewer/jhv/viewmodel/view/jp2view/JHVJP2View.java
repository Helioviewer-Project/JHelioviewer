package org.helioviewer.jhv.viewmodel.view.jp2view;

import java.awt.Dimension;
import java.net.URI;

import kdu_jni.Jp2_palette;
import kdu_jni.KduException;

import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.ChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.jhv.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.ViewportChangedReason;
import org.helioviewer.jhv.viewmodel.imagedata.ImageData;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.metadata.MetaDataFactory;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.AbstractView;
import org.helioviewer.jhv.viewmodel.view.ImageInfoView;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.SubimageDataView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.view.jp2view.J2KRender.RenderReasons;
import org.helioviewer.jhv.viewmodel.view.jp2view.concurrency.BooleanSignal;
import org.helioviewer.jhv.viewmodel.view.jp2view.concurrency.ReasonSignal;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.JP2ImageParameter;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;
import org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage;
import org.helioviewer.jhv.viewmodel.viewport.StaticViewport;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;
import org.helioviewer.jhv.viewmodel.viewportimagesize.StaticViewportImageSize;
import org.helioviewer.jhv.viewmodel.viewportimagesize.ViewportImageSize;
import org.helioviewer.jhv.viewmodel.viewportimagesize.ViewportImageSizeAdapter;

/**
 * Implementation of ImageInfoView for JPG2000 images.
 * 
 * <p>
 * This class represents the gateway to the heart of the helioviewer project. It
 * is responsible for reading and decoding JPG2000 images. Therefore, it manages
 * two Threads: One Thread for communicating with the JPIP server, the other one
 * for decoding the images.
 * 
 * <p>
 * For decoding the images, the kakadu library is used. Unfortunately, kakaku is
 * not threadsafe, so be careful! Although kakadu is a and highly optimized
 * library, the decoding process is the bottleneck for speeding up the
 * application.
 * 
 */
public class JHVJP2View extends AbstractView implements JP2View, ViewportView, RegionView, MetaDataView, SubimageDataView, ImageInfoView {

    public enum ReaderMode {
        NEVERFIRE, ONLYFIREONCOMPLETE, ALWAYSFIREONNEWDATA, SIGNAL_RENDER_ONCE
    };

    // Member related to the view chain
    protected Viewport viewport;
    protected Region region, lastDecodedRegion;
    protected ImageData imageData;
    //protected MetaData metaData;
    protected MetaData metaData;
    protected CircularSubImageBuffer subImageBuffer = new CircularSubImageBuffer();
    protected volatile ChangeEvent event = new ChangeEvent();

    // Member related to JP2
    protected boolean isMainView;
    protected boolean isPersistent;
    protected JP2Image jp2Image;
    protected volatile JP2ImageParameter imageViewParams;

    // Reader
    protected J2KReader reader;
    protected ReaderMode readerMode = ReaderMode.ALWAYSFIREONNEWDATA;
    final BooleanSignal readerSignal = new BooleanSignal(false);

    // Renderer
    protected J2KRender render;
    final ReasonSignal<RenderReasons> renderRequestedSignal = new ReasonSignal<RenderReasons>();

    // Renderer-ThreadGroup - This group is necessary to identify all renderer
    // threads
    public static final ThreadGroup THREAD_GROUP = new ThreadGroup("J2KRenderGroup");

    /**
     * Default constructor.
     * 
     * <p>
     * When the view is not marked as a main view, it is assumed, that the view
     * will only serve one single image and will not have to perform any kind of
     * update any more. The effect of this assumption is, that the view will not
     * try to reconnect to the JPIP server when the connection breaks and that
     * there will be no other timestamps used than the first one.
     * 
     * @param isMainView
     *            Whether the view is a main view or not
     */
    public JHVJP2View(boolean isMainView) {
        this.isMainView = isMainView;
        isPersistent = isMainView;
    }

    /**
     * Returns the JPG2000 image managed by this class.
     * 
     * @return JPG2000 image
     */
    public JP2Image getJP2Image() {
        return jp2Image;
    }

    /**
     * Sets the JPG2000 image used by this class.
     * 
     * This functions sets up the whole infrastructure needed for using the
     * image, including the two threads.
     * 
     * <p>
     * Thus, this functions also works as a constructor.
     * 
     * @param newJP2Image
     */
    public void setJP2Image(JP2Image newJP2Image) {
        if (jp2Image != null && reader != null) {
            abolish();
        }

        //metaData = MetaDataConstructor.getMetaData(newJP2Image);
        metaData = MetaDataFactory.getMetaData(newJP2Image);
        
        region = metaData.getPhysicalRegion();
        viewport = StaticViewport.createAdaptedViewport(256, 256);

        //if (metaData instanceof ObserverMetaData) {
            event.addReason(new TimestampChangedReason(this, metaData.getDateTime()));
        //}

        jp2Image = newJP2Image;

        imageViewParams = calculateParameter(newJP2Image.getQualityLayerRange().end, 0);

        if (isMainView) {
            jp2Image.setParentView(this);
        }

        jp2Image.addReference();

        try {
            reader = new J2KReader(this);
            render = new J2KRender(this);
            startDecoding();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //}
    }

    /**
     * Sets the reader mode.
     * 
     * <p>
     * The options are:
     * <ul>
     * <li>NEVERFIRE: The reader basically is disabled and never fires a
     * ChangeEvent.</li>
     * <li>ONLYFIREONCOMPLETE: The reader only fires a ChangeEvent, when the
     * current frame is loaded completely.</li>
     * <li>ALWAYSFIREONNEWDATA: Whenever new data is received, the reader fires
     * a ChangeEvent. This is the default value.</li>
     * </ul>
     * 
     * @param readerMode
     * @see #getReaderMode()
     */
    public void setReaderMode(ReaderMode readerMode) {
        this.readerMode = readerMode;
    }

    /**
     * Returns the reader mode.
     * 
     * @return Current reader mode.
     * @see #setReaderMode(ReaderMode)
     */
    public ReaderMode getReaderMode() {
        return readerMode;
    }

    /**
     * Sets, whether this view is persistent.
     * 
     * This value only has effect, when the image is a remote image. A
     * persistent view will close its socket after receiving the first frame. By
     * default, main views are not persistent.
     * 
     * @param isPersistent
     *            True, if this view is persistent
     * @see #isPersistent
     */
    public void setPersistent(boolean isPersistent) {
        this.isPersistent = isPersistent;
    }

    /**
     * Returns the built-in color lookup table.
     * 
     */
    public int[] getBuiltInLUT() {
        try {
            jp2Image.getLock().lock();
            Jp2_palette palette = jp2Image.getJpxSource().Access_codestream(0).Access_palette();

            if (palette.Get_num_luts() == 0)
                return null;

            int[] lut = new int[palette.Get_num_entries()];

            float[] red = new float[palette.Get_num_entries()];
            float[] green = new float[palette.Get_num_entries()];
            float[] blue = new float[palette.Get_num_entries()];

            palette.Get_lut(0, red);
            palette.Get_lut(1, green);
            palette.Get_lut(2, blue);

            for (int i = 0; i < lut.length; i++) {
                lut[i] = 0xFF000000 | ((int) ((red[i] + 0.5f) * 0xFF) << 16) | ((int) ((green[i] + 0.5f) * 0xFF) << 8) | ((int) ((blue[i] + 0.5f) * 0xFF));
            }

            return lut;

        } catch (KduException e) {
            e.printStackTrace();
        } finally {
            jp2Image.getLock().unlock();
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Viewport getViewport() {
        return viewport;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setViewport(Viewport v, ChangeEvent event) {

        boolean viewportChanged = (viewport == null ? v == null : !viewport.equals(v));
        viewport = v;
        if (setImageViewParams(calculateParameter())) {
            // sub image data will change because resolution level changed
            // -> memorize change event till sub image data has changed

            this.event.copyFrom(event);

            this.event.addReason(new ViewportChangedReason(this, v));

            return true;

        } else if (viewportChanged && imageViewParams.resolution.getZoomLevel() == jp2Image.getResolutionSet().getMaxResolutionLevels()) {

            this.event.copyFrom(event);

            this.event.addReason(new ViewportChangedReason(this, v));

            renderRequestedSignal.signal(RenderReasons.OTHER);

            return true;
        }

        return viewportChanged;
    }

    /**
     * {@inheritDoc}
     */
    public ImageData getImageData() {
        return imageData;
    }

    /**
     * {@inheritDoc}
     */
    public MetaData getMetaData() {
        return metaData;
    }

    /**
     * {@inheritDoc}
     */
    public int getCurrentNumQualityLayers() {
        return imageViewParams.qualityLayers;
    }

    /**
     * {@inheritDoc}
     */
    public int getMaximumNumQualityLayers() {
        return jp2Image.getQualityLayerRange().end;
    }

    /**
     * {@inheritDoc}
     */
    public void setNumQualityLayers(int newNumQualityLayers) {
        if (newNumQualityLayers >= 1 && newNumQualityLayers <= getMaximumNumQualityLayers()) {
            setImageViewParams(null, null, newNumQualityLayers, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Region getLastDecodedRegion() {
        return lastDecodedRegion;
    }

    /**
     * @return newest region, even if no new data has been retrieved, yet
     */
    public Region getNewestRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setRegion(Region r, ChangeEvent event) {

        boolean changed = region == null ? r == null : !region.equals(r);
        region = r;
        changed |= setImageViewParams(calculateParameter());
        this.event.copyFrom(event);
        return changed;
    }

    /**
     * {@inheritDoc}
     */
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
     */
    public String getName() {
        return metaData.getFullName();
    }

    /**
     * {@inheritDoc}
     */
    public URI getUri() {
        return jp2Image.getURI();
    }

    /**
     * {@inheritDoc}
     */
    public URI getDownloadURI() {
        return jp2Image.getDownloadURI();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRemote() {
        return jp2Image.isRemote();
    }

    /**
     * Returns whether the reader is connected to a JPIP server or not.
     * 
     * @return True, if connected to a JPIP server, false otherwise
     */
    public boolean isConnectedToJPIP() {
        if (reader != null)
            return reader.isConnected();

        return false;
    }

    /**
     * Fires a ChangeEvent into the view chain.
     * 
     * @param aEvent
     *            ChangeEvent to fire
     */
    public void fireChangeEvent(ChangeEvent aEvent) {
        super.notifyViewListeners(aEvent);
    }

    /**
     * Destroy the resources associated with this object.
     */
    public void abolish() {
        if (reader != null) {
            reader.abolish();
            reader = null;
        }
        if (render != null) {
            render.abolish();
            render = null;
        }
        jp2Image.abolish();
    }

    /**
     * Starts the J2KReader/J2KRender threads.
     */
    protected void startDecoding() {
        render.start();
        reader.start();
        readerSignal.signal();
    }

    /**
     * Recalculates the image parameters.
     * 
     * <p>
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     * 
     * <p>
     * To achieve this, calls {@link #calculateParameter(int, int)} with the
     * currently used number of quality layers and the first frame.
     * 
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter() {
        return calculateParameter(getCurrentNumQualityLayers(), 0);
    }

    /**
     * Recalculates the image parameters.
     * 
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     * 
     * <p>
     * To achieve this, calls
     * {@link #calculateParameter(Viewport, Region, int, int)} with the current
     * region and viewport and the given number of quality layers and frame
     * number.
     * 
     * @param numQualityLayers
     *            Number of quality layers to use
     * @param frameNumber
     *            Frame number to show (has to be 0 for single images)
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter(int numQualityLayers, int frameNumber) {
        return calculateParameter(viewport, region, numQualityLayers, frameNumber);
    }

    /**
     * Recalculates the image parameters.
     * 
     * This function maps between the set of parameters used within the view
     * chain and the set of parameters used within the jp2-package.
     * 
     * <p>
     * To achieve this, calculates the set of parameters used within the
     * jp2-package according to the given requirements from the view chain.
     * 
     * @param v
     *            Viewport the image will be displayed in
     * @param r
     *            Physical region
     * @param numQualityLayers
     *            Number of quality layers to use
     * @param frameNumber
     *            Frame number to show (has to be 0 for single images)
     * @return Set of parameters used within the jp2-package
     */
    protected JP2ImageParameter calculateParameter(Viewport v, Region r, int numQualityLayers, int frameNumber) {
        ViewportImageSize imageViewportDimension = ViewHelper.calculateViewportImageSize(v, r);
        MetaData metaData = getMetaData();

        // calculate total resolution of the image necessary to
        // have the requested resolution in the subimage
        int totalWidth = (int) Math.round(imageViewportDimension.getWidth() * metaData.getPhysicalImageWidth() / r.getWidth());
        int totalHeight = (int) Math.round(imageViewportDimension.getHeight() * metaData.getPhysicalImageHeight() / r.getHeight());

        // get corresponding resolution level
        ResolutionLevel res = jp2Image.getResolutionSet().getNextResolutionLevel(new Dimension(totalWidth, totalHeight));

        double imageMeterPerPixel = metaData.getPhysicalImageWidth() / res.getResolutionBounds().getWidth();
        int imageWidth = (int) Math.round(r.getWidth() / imageMeterPerPixel);
        int imageHeight = (int) Math.round(r.getHeight() / imageMeterPerPixel);

        Vector2i imagePostion = ViewHelper.calculateInnerViewportOffset(r, metaData.getPhysicalRegion(), new ViewportImageSizeAdapter(new StaticViewportImageSize(res.getResolutionBounds().width, res.getResolutionBounds().height)));

        SubImage subImage = new SubImage(imagePostion.getX(), imagePostion.getY(), imageWidth, imageHeight);
        subImageBuffer.putSubImage(subImage, r);

        return new JP2ImageParameter(subImage, res, numQualityLayers, frameNumber);

    }

    /**
     * Sets the current ImageViewParams to the ones specified. Any parameter
     * that should remain unchanged should be specified null. (Isn't
     * auto-unboxing just convenient as hell sometimes?)
     * 
     * @param _roi
     *            Pixel region to display
     * @param _resolution
     *            Resolution level to use
     * @param _qualityLayers
     *            Number of quality layers to use
     * @param _compositionLayer
     *            Frame number to use
     * @param _doReload
     *            If true, the image is reloaded after updating the parameters
     * @return true, if the parameters actually has changed, false otherwise
     */
    protected boolean setImageViewParams(SubImage _roi, ResolutionLevel _resolution, Integer _qualityLayers, Integer _compositionLayer, boolean _doReload) {
        return setImageViewParams(new JP2ImageParameter((_roi == null ? imageViewParams.subImage : _roi), (_resolution == null ? imageViewParams.resolution : _resolution), (_qualityLayers == null ? imageViewParams.qualityLayers : _qualityLayers), (_compositionLayer == null ? imageViewParams.compositionFrame : _compositionLayer)), _doReload);
    }

    /**
     * Method calls setImageViewParams(SubImage, ResolutionLevel, Integer,
     * Integer, boolean) with the boolean set to true.
     * 
     * @param _roi
     *            Pixel region to display
     * @param _resolution
     *            Resolution level to use
     * @param _qualityLayers
     *            Number of quality layers to use
     * @param _compositionLayer
     *            Frame number to use
     * @return true, if the parameters actually has changed, false otherwise
     */
    protected boolean setImageViewParams(SubImage _roi, ResolutionLevel _resolution, Integer _qualityLayers, Integer _compositionLayer) {
        return setImageViewParams(_roi, _resolution, _qualityLayers, _compositionLayer, true);
    }

    /**
     * Calls {@link #setImageViewParams(JP2ImageParameter, boolean)} with the
     * boolean set to true.
     * 
     * @param newParams
     *            New set of parameters to use
     * @return true, if the parameters actually has changed, false otherwise
     */
    protected boolean setImageViewParams(JP2ImageParameter newParams) {
        return setImageViewParams(newParams, true);
    }

    /**
     * Sets the image parameters, if the given ones are valid.
     * 
     * Also, triggers an update of the image using the new set of parameters, if
     * desired.
     * 
     * @param newParams
     *            New set of parameters to use
     * @param reload
     *            if true, triggers an update of the image
     * @return true, if the parameters actually has changed, false otherwise
     */
    protected boolean setImageViewParams(JP2ImageParameter newParams, boolean reload) {
    	// TO DO: Rewrite this, it's just a hack
    	 if (imageViewParams.equals(newParams) && region.getWidth() == lastDecodedRegion.getWidth() && region.getHeight() == lastDecodedRegion.getHeight()) {
    		 return false;
    	 }
    	 
    	if (newParams.subImage.width == 0 || newParams.subImage.height == 0) {
            if (imageData == null) {
                return false;
            }
            setSubimageData(null, null, 0);
            return true;
        }
        imageViewParams = newParams;

        if (reload) {
            readerSignal.signal();
        }
        return true;
    }

    /*
     * NOTE: The following section is for communications with the two threads,
     * J2KReader and J2KRender. Thus, the visibility is set to "default" (also
     * known as "package"). These functions should not be used by any other
     * class.
     */

    /**
     * Returns the current set of parameters.
     * 
     * @return Current set of parameters
     */
    JP2ImageParameter getImageViewParams() {
        return imageViewParams;
    }

    /**
     * Sets the new image data for the given region.
     * 
     * <p>
     * This function is used as a callback function which is called by
     * {@link J2KRender} when it has finished decoding an image.
     * 
     * @param newImageData
     *            New image data
     * @param roi
     *            Area the image contains, to find the corresponding
     * @param compositionLayer
     *            Composition Layer rendered, to update meta data
     *            {@link org.helioviewer.jhv.viewmodel.region.Region}
     */
    void setSubimageData(ImageData newImageData, SubImage roi, int compositionLayer) {
        imageData = newImageData;
        Region lastRegionSaved = lastDecodedRegion;
        subImageBuffer.setLastRegion(roi);
        this.event.addReason(new RegionUpdatedReason(this, lastDecodedRegion));

        if (!lastDecodedRegion.equals(lastRegionSaved)) {
            this.event.addReason(new RegionChangedReason(this, lastDecodedRegion));
        }

        event.addReason(new SubImageDataChangedReason(this));

        ChangeEvent fireEvent = null;
        synchronized (event) {
            fireEvent = event.clone();
            event.reinitialize();
        }
        notifyViewListeners(fireEvent);
        // Just a hack (by stefan meier), because notifyViewListener sometime doesn't work correctly
        GuiState3DWCS.overViewPanel.subImageDataChanged();
        GuiState3DWCS.mainComponentView.subImageDataChanged();
    }

    /**
     * Returns whether this view is used as a main view.
     * 
     * @return Whether this view is used as a main view
     */
    boolean isMainView() {
        return isMainView;
    }

    /**
     * Returns, whether this view is persistent.
     * 
     * @return True, if this view is persistent, false otherwise.
     * @see #setPersistent(boolean)
     */
    boolean isPersistent() {
        return isPersistent;
    }

    /**
     * Recalculate the image parameters.
     * 
     * This might be useful, if some assumption have changed, such as the
     * resolution set.
     */
    void updateParameter() {
        setImageViewParams(calculateParameter());
    }

    /**
     * Adds a ChangedReason to the current event.
     * 
     * The event will be fired during the next call of
     * {@link #setSubimageData(ImageData, SubImage, int)}.
     * 
     * @param reason
     *            The ChangedReason to add
     */
    void addChangedReason(ChangedReason reason) {
        event.addReason(reason);
    }

    /**
     * Private class for remembering the
     * {@link org.helioviewer.jhv.viewmodel.region.Region} corresponding to
     * {@link org.helioviewer.jhv.viewmodel.view.jp2view.image.SubImage}.
     * 
     * <p>
     * To ensure, that the size of the buffer does not grow into infinity, this
     * buffer is organized in circle.
     */
    private class CircularSubImageBuffer {

        private static final int BUFFER_SIZE = 16;
        private SubImageRegion[] buffer = new SubImageRegion[BUFFER_SIZE];
        private int nextPos = 0;

        /**
         * Puts a new pair of Region and SubImage into the buffer.
         * 
         * @param subImage
         * @param subImageRegion
         */
        public void putSubImage(SubImage subImage, Region subImageRegion) {
            SubImageRegion newEntry = new SubImageRegion();
            
            newEntry.subImage = subImage;
            newEntry.region = subImageRegion;

            buffer[(++nextPos) & (BUFFER_SIZE - 1)] = newEntry;
        }

        /**
         * Sets the parents Region to the one corresponding to subImage.
         * 
         * @param subImage
         *            Search Region for this SubImage
         */
        public void setLastRegion(SubImage subImage) {
            int searchPos = nextPos;
            SubImageRegion searchEntry;

            for (int i = 0; i < BUFFER_SIZE; i++) {
                searchEntry = buffer[(searchPos--) & (BUFFER_SIZE - 1)];
                if (searchEntry != null && searchEntry.subImage == subImage) {
                    lastDecodedRegion = searchEntry.region;
                    return;
                }
            }
        }

        /**
         * Pair of SubImage and Region.
         */
        private class SubImageRegion {
            public SubImage subImage;
            public Region region;
        }
    }
}
