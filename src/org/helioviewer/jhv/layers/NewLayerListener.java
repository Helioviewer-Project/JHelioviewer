package org.helioviewer.jhv.layers;

public interface NewLayerListener {
	void newlayerAdded();
	void newlayerRemoved(int idx);
	void newtimestampChanged();
	void activeLayerChanged(Layer layer);
}
