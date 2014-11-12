package org.helioviewer.jhv.viewmodel.view.opengl;

import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.NonConstantMetaDataChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.RegionUpdatedReason;
import org.helioviewer.jhv.viewmodel.changeevent.SubImageDataChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.LayerChangedReason.LayerChangeType;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.region.RegionAdapter;
import org.helioviewer.jhv.viewmodel.region.StaticRegion;
import org.helioviewer.jhv.viewmodel.view.AbstractView;
import org.helioviewer.jhv.viewmodel.view.LayeredView;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.SubimageDataView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
import org.helioviewer.jhv.viewmodel.view.ViewListener;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJP2View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLFragmentShaderView;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLMinimalFragmentShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLMinimalVertexShaderProgram;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLShaderBuilder;
import org.helioviewer.jhv.viewmodel.view.opengl.shader.GLVertexShaderView;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;
import org.helioviewer.jhv.viewmodel.viewportimagesize.ViewportImageSize;

/**
 * The GL3DLayeredView makes sure to add all required sub-views to a new layer.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DLayeredView extends AbstractView implements LayeredView, RegionView, ViewportView, MetaDataView, ViewListener, GLFragmentShaderView, GLVertexShaderView, GL3DView {
    // public class GL3DLayeredView extends AbstractLayeredView implements
    // GL3DView, LayeredView {

    private Lock renderLock = new ReentrantLock();
    private GLTextureHelper textureHelper = new GLTextureHelper();

	protected CopyOnWriteArrayList<View> layers = new CopyOnWriteArrayList<View>();
    protected ReentrantLock layerLock = new ReentrantLock();
    protected HashMap<View, Layer> viewLookup = new HashMap<View, Layer>();
    protected Viewport viewport;
    protected Region region;
    protected MetaData metaData;
    protected ViewportImageSize viewportImageSize;
    protected double minimalRegionSize;

    /**
     * Buffer for precomputed values for each layer.
     * 
     * <p>
     * This container saves some values per layer, such as its visibility and
     * precomputed view adapters.
     * 
     */
    public class Layer {
        public View view;

        public RegionView regionView;
        public ViewportView viewportView;
        public MetaDataView metaDataView;

        public boolean visibility = true;

        /**
         * Default constructor.
         * 
         * Computes view adapters for this layer.
         * 
         * @param base
         *            layer to save
         */
        public Layer(View base) {
            view = base;
            update();
        }

        /**
         * Recalculates the view adapters, in case the view chain has changed.
         */
        public void update() {
            if (view != null) {
                regionView = view.getAdapter(RegionView.class);
                viewportView = view.getAdapter(ViewportView.class);
                metaDataView = view.getAdapter(MetaDataView.class);
            }
        }
    }
    
    public GL3DLayeredView() {
    }

    public void addLayer(View newLayer, int newIndex) {
        if (newLayer == null)
            return;

        // This code snippet is copied from the superclass, because the
        // imageTextureView should be the first
        // view in the layer sub-chain
        if (newLayer.getAdapter(GLScalePowerOfTwoView.class) == null) {
            GLScalePowerOfTwoView scaleView = new GLScalePowerOfTwoView();
            scaleView.setView(newLayer);
            newLayer = scaleView;
        }

        if (newLayer.getAdapter(GL3DCoordinateSystemView.class) == null) {
            GL3DCoordinateSystemView coordinateSystemView = new GL3DCoordinateSystemView();
            coordinateSystemView.setView(newLayer);
            newLayer = coordinateSystemView;
        }

        if (newLayer.getAdapter(GL3DImageRegionView.class) == null) {
            GL3DImageRegionView imageRegionView = new GL3DImageRegionView();
            imageRegionView.setView(newLayer);
            newLayer = imageRegionView;
        }

        if (newLayer.getAdapter(GL3DImageTextureView.class) == null) {
            GL3DImageTextureView imageToTextureView = new GL3DImageTextureView();
            imageToTextureView.setView(newLayer);
            newLayer = imageToTextureView;
        }

        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        ChangeEvent changeEvent = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_ADDED, newLayer));

            layers.add(newIndex, newLayer);
            newLayer.addViewListener(this);

            viewLookup.put(newLayer, new Layer(newLayer));

            recalculateMetaData();
            
            region = metaData.getPhysicalRegion();
            if (viewport != null)
                region = ViewHelper.expandRegionToViewportAspectRatio(viewport, region, metaData);
            if (region != null)
                region = new RegionAdapter(new StaticRegion(-0.5 * region.getWidth(), -0.5 * region.getHeight(), region.getSize()));
            recalculateRegionsAndViewports(new ChangeEvent());
        redrawBuffer(changeEvent);        
    }

    public void render3D(GL3DState state) {
        renderLock.lock();
        for (int i = 0; i < this.getNumLayers(); i++) {
            View layerView = this.getLayer(i);
            if (layerView instanceof GL3DView) {
                ((GL3DView) layerView).render3D(state);
            } else if (layerView instanceof GLView) {
                ((GLView) layerView).renderGL(state.gl, true);
            }
        }
        renderLock.unlock();
    }

    public void deactivate(GL3DState state) {
        for (int i = 0; i < getNumLayers(); i++) {
            if (getLayer(i).getAdapter(GL3DView.class) != null) {
                JHVJPXView JHVJPXView = getLayer(i).getAdapter(JHVJPXView.class);
                if (JHVJPXView != null) {
                    JHVJPXView.pauseMovie();
                }
                getLayer(i).getAdapter(GL3DView.class).deactivate(state);
            }
        }
    }

    /**
     * Recalculates the regions and viewports of all layers.
     * 
     * <p>
     * Sets the regions and viewports of all layers according to the region and
     * viewport of the LayeredView. Also, calculates the offset of the layers to
     * each other.
     * 
     * @param event
     *            ChangeEvent to collect history of all following changes
     * @return true, if at least one region or viewport changed
     */
    protected boolean recalculateRegionsAndViewports(ChangeEvent event) {
        
        renderLock.lock();

        Log.debug("GL3DLayeredView: recalculateRegionsAndViewports: " + this.region + " " + this.viewport);
        boolean changed = false;

        if (viewport != null) {
            for (Layer layer : viewLookup.values()) {
                changed |= layer.viewportView.setViewport(getViewport(), event);
            }
        }
        if (changed) {
            notifyViewListeners(event);
        }
        renderLock.unlock();

        return changed;
    }

    /**
     * Recalculates the regions and viewports of all layers.
     * 
     * <p>
     * Sets the regions and viewports of all layers according to the region and
     * viewport of the LayeredView. Also, calculates the offset of the layers to
     * each other.
     * 
     * @param event
     *            ChangeEvent to collect history of all following changes
     * @return true, if at least one region or viewport changed
     */
    protected boolean recalculateRegionsAndViewports(ChangeEvent event, boolean includePixelBasedImages) {
        return this.recalculateRegionsAndViewports(event);
    }

    
    protected void redrawBufferImpl() {
    }
    
    /**
     * {@inheritDoc}
     */
    public void renderGL(GL2 gl, boolean nextView) {

        layerLock.lock();

        try {
            gl.glPushMatrix();

            for (View v : layers) {
                Layer layer = viewLookup.get(v);
                if (layer != null){
                	// If invisible, skip layer
	                if (!layer.visibility) {
	                    continue;
	                }
	
	                gl.glColor3f(1.0f, 1.0f, 1.0f);
	                // if layer is GLView, go on, otherwise render now
	                if (v instanceof GLView) {
	                    ((GLView) v).renderGL(gl, true);
	                } else {
	                    textureHelper.renderImageDataToScreen(gl, layer.regionView.getRegion(), v.getAdapter(SubimageDataView.class).getSubimageData());
	                }
                }
            }

            gl.glPopMatrix();

        } finally {
            layerLock.unlock();
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * In this case, creates a new shader for every layer and initializes it
     * with the least necessary commands.
     */
    public GLShaderBuilder buildFragmentShader(GLShaderBuilder shaderBuilder) {

        layerLock.lock();

        try {
            for (View v : layers) {

                GLFragmentShaderView fragmentView = v.getAdapter(GLFragmentShaderView.class);
                if (fragmentView != null) {
                    // create new shader builder
                    GLShaderBuilder newShaderBuilder = new GLShaderBuilder(shaderBuilder.getGL(), GL2.GL_FRAGMENT_PROGRAM_ARB);

                    // fill with standard values
                    GLMinimalFragmentShaderProgram minimalProgram = new GLMinimalFragmentShaderProgram();
                    minimalProgram.build(newShaderBuilder);

                    // fill with other filters and compile
                    fragmentView.buildFragmentShader(newShaderBuilder).compile();
                }
            }
        } finally {
            layerLock.unlock();
        }

        return shaderBuilder;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * In this case, creates a new shader for every layer and initializes it
     * with the least necessary commands.
     */
    public GLShaderBuilder buildVertexShader(GLShaderBuilder shaderBuilder) {

        layerLock.lock();

        try {
            for (View v : layers) {
                GLVertexShaderView vertexView = v.getAdapter(GLVertexShaderView.class);
                if (vertexView != null) {
                    // create new shader builder
                    GLShaderBuilder newShaderBuilder = new GLShaderBuilder(shaderBuilder.getGL(), GL2.GL_VERTEX_PROGRAM_ARB);

                    // fill with standard values
                    GLMinimalVertexShaderProgram minimalProgram = new GLMinimalVertexShaderProgram();
                    minimalProgram.build(newShaderBuilder);

                    // fill with other filters and compile
                    vertexView.buildVertexShader(newShaderBuilder).compile();
                }
            }
        } finally {
            layerLock.unlock();
        }

        return shaderBuilder;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isVisible(View _view) {
        if (viewLookup.get(_view) != null)
            return viewLookup.get(_view).visibility;
        else
            return false;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumberOfVisibleLayer() {
        int result = 0;
        layerLock.lock();
        try {
            Collection<Layer> values = viewLookup.values();
            for (Layer l : values) {
                if (l.visibility)
                    result++;
            }
        } finally {
            layerLock.unlock();
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void toggleVisibility(View view) {
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();

        Layer layer = viewLookup.get(view);

        if (layer != null) {
            layer.visibility = !layer.visibility;

            redrawBuffer(new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_VISIBILITY, view)));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addLayer(View newLayer) {
        addLayer(newLayer, layers.size());
    }

    /**
     * {@inheritDoc}
     */
    public View getLayer(int index) {
    	if (index < layers.size())
    		return layers.get(index);
    	return null;
    }

    /**
     * {@inheritDoc}
     */
    public int getNumLayers() {
        return layers.size();
    }

    /**
     * {@inheritDoc}
     */
    public int getLayerLevel(View view) {
        return layers.indexOf(view);
    }

    /**
     * {@inheritDoc}
     */
    public void removeLayer(View view) {

        if (view == null) {
            return;
        }

        int index = layers.indexOf(view);
        if (index == -1) {
            return;
        }
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();
        layerLock.lock();
        try {
            layers.remove(view);
            viewLookup.remove(view);
            view.removeViewListener(this);
        } finally {
            layerLock.unlock();
        }
        if (view.getAdapter(JHVJP2View.class) != null) {
            view.getAdapter(JHVJP2View.class).abolish();
        }

        ChangeEvent event = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_REMOVED, view, index));

        recalculateMetaData();
        if (metaData != null) {
            Region bound = metaData.getPhysicalRegion();
            double lowerLeftX = Math.max(bound.getCornerX(), region.getCornerX());
            double lowerLeftY = Math.max(bound.getCornerY(), region.getCornerY());
            Region newRegion = ViewHelper.cropInnerRegionToOuterRegion(StaticRegion.createAdaptedRegion(lowerLeftX, lowerLeftY, region.getSize()), bound);
            setRegion(newRegion, event);
        } else {
            recalculateRegionsAndViewports(event);
        }
        redrawBuffer(event);
    }

    /**
     * {@inheritDoc}
     */
    public void removeLayer(int index) {
        removeLayer(layers.get(index));
    }

    /**
     * {@inheritDoc}
     */
    public void removeAllLayers() {

        ChangeEvent event = new ChangeEvent();
        LinkedMovieManager.getActiveInstance().pauseLinkedMovies();
        layerLock.lock();
        try {
            for (View view : layers) {
                int index = layers.indexOf(view);
                view.removeViewListener(this);
                if (view.getAdapter(JHVJP2View.class) != null) {
                    view.getAdapter(JHVJP2View.class).abolish();
                }
                event.addReason(new LayerChangedReason(this, LayerChangeType.LAYER_REMOVED, view, index));
            }
            layers.clear();
            viewLookup.clear();
        } finally {
            layerLock.unlock();
        }
        recalculateMetaData();
        recalculateRegionsAndViewports(event);
        redrawBuffer(event);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings(value = { "unchecked" })
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
    public Region getRegion() {
        return region;
    }

    /**
     * {@inheritDoc}
     */
    public boolean setRegion(Region r, ChangeEvent event) {

        if (event == null) {
            event = new ChangeEvent(new RegionUpdatedReason(this, r));
        } else {
            event.addReason(new RegionUpdatedReason(this, r));
        }

        // check if region is valid
        if (region == null || r == null || r.getWidth() < minimalRegionSize || r.getHeight() < minimalRegionSize) {
            notifyViewListeners(event);
            return false;
        }
        // check if region to small or viewport
        r = ViewHelper.expandRegionToViewportAspectRatio(viewport, r, metaData);

        // check if region has changed
        if (region.getCornerX() == r.getCornerX() && region.getCornerY() == r.getCornerY() && region.getWidth() == r.getWidth() && region.getHeight() == r.getHeight()) {
            notifyViewListeners(event);
            return false;
        }

        event.addReason(new RegionChangedReason(this, r));

        region = r;
        recalculateRegionsAndViewports(event);
        redrawBuffer(event);
        return true;
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

        // check if viewport has changed
        if (viewport != null && v != null && viewport.getWidth() == v.getWidth() && viewport.getHeight() == v.getHeight())
            return false;

        viewport = v;

        if (!setRegion(region, event)) {
            recalculateRegionsAndViewports(event);
            redrawBuffer(event);
        }
        return true;
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
    public void viewChanged(View sender, ChangeEvent aEvent) {
        if (aEvent.reasonOccurred(ViewChainChangedReason.class)) {
            for (Layer layer : viewLookup.values()) {
                layer.update();
            }
        }
        if (aEvent.reasonOccurred(NonConstantMetaDataChangedReason.class)) {
            recalculateMetaData();
            recalculateRegionsAndViewports(new ChangeEvent(aEvent));
        }

        if ((aEvent.reasonOccurred(RegionChangedReason.class) || aEvent.reasonOccurred(SubImageDataChangedReason.class) || aEvent.reasonOccurred(ViewChainChangedReason.class)) && sender != null) {
            redrawBuffer(aEvent);
        } else {
            notifyViewListeners(aEvent);
        }
    }

    /**
     * Recalculates the meta data of the LayeredView.
     * 
     * The region of the LayeredView is set to the bounding box of all layers.
     */
    protected void recalculateMetaData() {
        recalculateMetaData(true);
    }

    /**
     * Recalculates the meta data of the LayeredView.
     * 
     * The region of the LayeredView is set to the bounding box of all layers.
     */
    protected void recalculateMetaData(boolean includePixelBasedImages) {
        if (layers.size() == 0) {
            metaData = null;
            return;
        }

        minimalRegionSize = 0.0f;
        layerLock.lock();
        try {
            for (Layer layer : viewLookup.values()) {
                if (layer.metaDataView != null) {
                    //if (includePixelBasedImages || !(layer.metaDataView.getMetaData() instanceof PixelBasedMetaData)) {
                         metaData = layer.metaDataView.getMetaData();

                        
                        double unitsPerPixel = layer.metaDataView.getMetaData().getUnitsPerPixel();
                        if (unitsPerPixel > minimalRegionSize) {
                            minimalRegionSize = unitsPerPixel;
                        }
                    //}
                }
            }
        } finally {
            layerLock.unlock();
        }

        minimalRegionSize *= 2.0f;
    }

    /**
     * Redraws the scene.
     * 
     * Calls the implementation specific function redrawBufferImpl(). A
     * SubImageDataChangedReason will be appended to the given ChangeEvent.
     * 
     * @param aEvent
     *            ChangeEvent to collect history
     */
    protected void redrawBuffer(ChangeEvent aEvent) {
        redrawBufferImpl();

        // add reason to change event
        if (aEvent == null)
            aEvent = new ChangeEvent();

        aEvent.addReason(new SubImageDataChangedReason(this));

        notifyViewListeners(aEvent);
    }


    /**
     * {@inheritDoc}
     */
    public void moveView(View view, int newLevel) {
        ChangeEvent changeEvent = new ChangeEvent(new LayerChangedReason(this, LayerChangeType.LAYER_MOVED, view, newLevel));
        layerLock.lock();
        try {
            if (layers.contains(view)) {
                layers.remove(view);
                layers.add(newLevel, view);
                redrawBuffer(changeEvent);
            }
        } finally {
            layerLock.unlock();
        }
    }
}
