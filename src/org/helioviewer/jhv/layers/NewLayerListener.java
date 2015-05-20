package org.helioviewer.jhv.layers;

public interface NewLayerListener {
	void newlayerAdded();
	void newlayerRemoved(int idx);
	void activeLayerChanged(LayerInterface layer);
}
