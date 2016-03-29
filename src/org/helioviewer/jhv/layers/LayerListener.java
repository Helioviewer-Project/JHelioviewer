package org.helioviewer.jhv.layers;

import javax.annotation.Nullable;

public interface LayerListener
{
	void layerAdded();
	void layersRemoved();
	void activeLayerChanged(@Nullable Layer _newLayer);
}
