package org.helioviewer.jhv.layers;

import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewRender;

public class Layers {
	CopyOnWriteArrayList<LayerInterface> layers;
	CopyOnWriteArrayList<NewLayerListener> renderListeners;
	private int activeLayer = 0;
	
	private NewRender renderer = new NewRender();
	private NewCache newCache = new NewCache();
	
	public Layers() {
		layers = new CopyOnWriteArrayList<LayerInterface>();
		renderListeners = new CopyOnWriteArrayList<NewLayerListener>();
	}
	
	public void addLayer(LayerInterface layer){
		layers.add(layer);
		for (NewLayerListener renderListener : renderListeners){
			renderListener.newlayerAdded();
		}
	}
	
	public LayerInterface addLayer(int id){
		NewLayer layer = new NewLayer(id, renderer, newCache);
		layers.add(layer);
		return layer;
	}
	
	public LayerInterface getLayer(int idx){
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
