package org.helioviewer.jhv.layers;

public interface LayerListener
{
	void layerAdded();
	void layersRemoved();
	void activeLayerChanged(AbstractLayer _newLayer);
}
