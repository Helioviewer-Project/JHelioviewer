package org.helioviewer.jhv.layers;

import java.util.concurrent.CopyOnWriteArrayList;

public class Layers {
	CopyOnWriteArrayList<Layer> layers;
	CopyOnWriteArrayList<NewLayerListener> renderListeners;
	private int activeLayer = 0;
	
	public Layers() {
		layers = new CopyOnWriteArrayList<Layer>();
		renderListeners = new CopyOnWriteArrayList<NewLayerListener>();
	}
	
	public void addLayer(Layer layer){
		layers.add(layer);
		for (NewLayerListener renderListener : renderListeners){
			renderListener.newlayerAdded();
		}
	}
	
	public Layer getLayer(int idx){
		return layers.get(idx);
	}
	
	public void removeLayer(int idx){
		layers.remove(idx);
		for (NewLayerListener renderListener : renderListeners){
			renderListener.newlayerRemoved(idx);
		}
	}
	
	public void addNewLayerListener(NewLayerListener renderListener){
		this.renderListeners.add(renderListener);
	}

	public int getLayerCount() {
		return layers.size();
	}

	public void layerChanged() {
		System.out.println("layer changed : " + activeLayer);
		for (NewLayerListener renderListener : renderListeners){
			renderListener.activeLayerChanged(this.getLayer(activeLayer));
		}
	}
	
	public int getActiveLayerNumber(){
		return activeLayer;
	}
	
	public Layer getActiveLayer(){
		return layers.get(activeLayer);
	}
	
	public void setActiveLayer(int activeLayer){
		if (this.activeLayer != activeLayer){
			this.activeLayer = activeLayer;
			this.layerChanged();
		}
	}
	
	public CopyOnWriteArrayList<Layer> getLayers(){
		return layers;
	}
}
