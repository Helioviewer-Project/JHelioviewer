package org.helioviewer.jhv.layers;

public interface LayerListener {
	void newlayerAdded();
	void newlayerRemoved(int idx);
	void activeLayerChanged(LayerInterface layer);
}
