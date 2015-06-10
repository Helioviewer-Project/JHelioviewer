package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewCache;
import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.NewRender;

public class Layers {
	private CopyOnWriteArrayList<LayerInterface> layers;
	private CopyOnWriteArrayList<NewLayerListener> layerListeners;
	private int activeLayer = 0;
	
	private NewRender renderer = new NewRender();
	private NewCache newCache = NewCache.singelton;
	private boolean coronaVisibility = true;
	
	public static final Layers LAYERS = new Layers();
	
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
	
	public NewLayer addLayer(int id, LocalDateTime start, LocalDateTime end, int cadence){
		NewLayer layer = new NewLayer(id, renderer, newCache, start, end, cadence);
		layers.add(layer);
		for (NewLayerListener renderListener : layerListeners){
			renderListener.newlayerAdded();
		}
		if (layers.size() == 1){
			this.layerChanged();
		}
		return layer;
	}
	
	private LayerInterface getLayer(int idx){
		return layers.get(idx);
	}
	
	public void removeLayer(int idx){
		layers.remove(idx);
		activeLayer = 0;
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

	private void layerChanged() {
		for (NewLayerListener renderListener : layerListeners){
			renderListener.activeLayerChanged(this.getLayer(activeLayer));
		}
	}
	
	public int getActiveLayerNumber(){
		return activeLayer;
	}
	
	public LayerInterface getActiveLayer(){
		if (layers.size() <= 0) return null;
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

	public void toggleCoronaVisibility() {
		this.coronaVisibility = !this.coronaVisibility;
	}
	
	public boolean getCoronaVisibility(){
		return this.coronaVisibility;
	}
}
