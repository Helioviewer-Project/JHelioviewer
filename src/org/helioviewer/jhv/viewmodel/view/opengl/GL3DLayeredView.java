package org.helioviewer.jhv.viewmodel.view.opengl;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.view.LayeredView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;

/**
 * The GL3DLayeredView makes sure to add all required sub-views to a new layer.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DLayeredView extends GLLayeredView implements GL3DView, LayeredView, RegionView, ViewportView {
    // public class GL3DLayeredView extends AbstractLayeredView implements
    // GL3DView, LayeredView {

    private Lock renderLock = new ReentrantLock();
    
    public GL3DLayeredView() {
    }

    public void addLayer(View newLayer, int newIndex) {
        if (newLayer == null)
            return;

        // This code snippet is copied from the superclass, because the
        // imageTextureView should be the first
        // view in the layer sub-chain
        if (!GLTextureHelper.textureNonPowerOfTwoAvailable() && newLayer.getAdapter(GLScalePowerOfTwoView.class) == null) {
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


        
        // Call to GLLayeredView.addLayer
        super.addLayer(newLayer, newIndex);
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

    protected boolean recalculateRegionsAndViewports(ChangeEvent event, boolean includePixelBasedImages) {
        return this.recalculateRegionsAndViewports(event);
    }

    
    protected void redrawBufferImpl() {
    }
}
