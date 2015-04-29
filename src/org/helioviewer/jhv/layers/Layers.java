package org.helioviewer.jhv.layers;

import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewRender;

public class Layers {
	CopyOnWriteArrayList<LayerInterface> layers;
	CopyOnWriteArrayList<NewLayerListener> layerListeners;
	private int activeLayer = 0;
	
	private NewRender renderer = new NewRender();
	private NewCache newCache = new NewCache();
	
	public Layers() {
		layers = new CopyOnWriteArrayList<LayerInterface>();
		layerListeners = new CopyOnWriteArrayList<NewLayerListener>();
	}
		
	public void addLayer(String uri){
		NewLayer layer = new NewLayer(uri, renderer);
		layers.add(layer);
		for (NewLayerListener renderListener : layerListeners){
			renderListener.newlayerAdded();
		}
		if (layers.size() == 1){
			this.layerChanged();
		}
	}
	
	public NewLayer addLayer(int id){
		NewLayer layer = new NewLayer(id, renderer, newCache);
		layers.add(layer);
		for (NewLayerListener renderListener : layerListeners){
			renderListener.newlayerAdded();
		}
		if (layers.size() == 1){
			this.layerChanged();
		}
		return layer;
	}
	
	public LayerInterface getLayer(int idx){
		return layers.get(idx);
	}
	
	public void removeLayer(int idx){
		layers.remove(idx);
		for (NewLayerListener renderListener : layerListeners){
			renderListener.newlayerRemoved(idx);
		}
	}
		
	public void addNewLayerListener(NewLayerListener renderListener){
		this.layerListeners.add(renderListener);
	}

	public int getLayerCount() {
		return layers.size();
	}

	public void layerChanged() {
		for (NewLayerListener renderListener : layerListeners){
			renderListener.activeLayerChanged(this.getLayer(activeLayer));
		}
	}
	
	public int getActiveLayerNumber(){
		return activeLayer;
	}
	
	public LayerInterface getActiveLayer(){
		return layers.get(activeLayer);
	}
	
	public void setActiveLayer(int activeLayer){
		if (this.activeLayer != activeLayer){
			this.activeLayer = activeLayer;
			this.layerChanged();
		}
	}
	
	public CopyOnWriteArrayList<LayerInterface> getLayers(){
		return layers;
	}
}
