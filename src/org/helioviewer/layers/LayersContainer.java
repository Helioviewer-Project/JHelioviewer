package org.helioviewer.layers;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;


public class LayersContainer {
	
	CopyOnWriteArrayList<Layer> layers = new CopyOnWriteArrayList<Layer>();
	CopyOnWriteArrayList<LayerListener> layerListeners = new CopyOnWriteArrayList<LayerListener>();
	
	private static LayersContainer layersContainer = null;
	private Layer activeLayer = null;
	private LayersContainer() {
		// TODO Auto-generated constructor stub
	}
	
	public static LayersContainer getSigentlon(){
		if (layersContainer == null){
			layersContainer = new LayersContainer();
		}
		return layersContainer;
	}

	public void addLayer(Layer layer){
		if (activeLayer == null) selectLayer(layer);
		layers.add(layer);
		fireLayerAdded(layer);
	}
	
	public void addLayer(Layer layer, int idx){
		if (activeLayer == null) selectLayer(layer);
		layers.add(idx, layer);
		fireLayerAdded(layer);
	}
	
	public boolean removeLayer(Layer layer){
		fireLayerRemoved(layer);
		return layers.remove(layer);
	}
	
	public void selectLayer(Layer layer){
		if (activeLayer != layer){
			activeLayer = layer;
			fireActiveLayerChanged();
		}
	}
	
	private void fireActiveLayerChanged(){
		Iterator<LayerListener> iterator = this.layerListeners.iterator();
		while(iterator.hasNext()){
			LayerListener layerListener = iterator.next();
			layerListener.activeLayerChanged(activeLayer);
		}
	}
	
	private void fireLayerAdded(Layer layer){
		Iterator<LayerListener> iterator = this.layerListeners.iterator();
		while(iterator.hasNext()){
			LayerListener layerListener = iterator.next();
			layerListener.layerAdded(layer);
		}		
	}
	
	private void fireLayerRemoved(Layer layer){
		Iterator<LayerListener> iterator = this.layerListeners.iterator();
		while(iterator.hasNext()){
			LayerListener layerListener = iterator.next();
			layerListener.layerRemoved(layer);
		}		
	}
	
	public void addLayerListeners(LayerListener layerListener){
		this.layerListeners.add(layerListener);
	}
	
}
