package org.helioviewer.layers;

import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.view.View;

public interface LayerListener {

    /**
     * Gets fired if a new layer has been added.
     * 
     * @param idx
     *            - index of the new layer
     */
    public void layerAdded(int idx);
    public void layerAdded(Layer layer);

    /**
     * Gets fired if a layer has been removed.
     * 
     * @param oldIdx
     *            - (old) index of the layer that has been removed
     */
    public void layerRemoved(Layer layer);

    /**
     * Gets fired if a layer has changed.
     * 
     * @param idx
     *            - index of the layer that changed
     */
    public void layerChanged(int idx);

    /**
     * Gets fired if the active layer has changed (meaning, a new layer has
     * become the new active layer).
     * 
     * @param idx
     *            - index of the new active layer
     */
    public void activeLayerChanged(Layer layer);

    /**
     * Gets fired if the viewport geometry changed (which might e.g. be
     * interesting for updating Zoomlevel information etc.).
     */
    public void viewportGeometryChanged();

    /**
     * Gets fired if the timestamp changed.
     * 
     * @param idx
     *            - index of the new active layer
     */
    public void timestampChanged(int idx);

    /**
     * Gets fired if any image data changed.
     */
    public void subImageDataChanged();

    /**
     * Gets fired if a remote jp2 image or movie was downloaded and now becomes
     * a local image
     * 
     * @param idx
     */
    public void layerDownloaded(Layer layer);


}
