package org.helioviewer.jhv.layers;

public interface LayerListener {
	void newLayerAdded();
	void newlayerRemoved(int idx);
	void activeLayerChanged(AbstractLayer layer);
}
