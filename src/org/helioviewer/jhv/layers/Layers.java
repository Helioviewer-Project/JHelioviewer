package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;

import org.helioviewer.jhv.viewmodel.view.jp2view.newjpx.KakaduRender;

public class Layers {
	private static CopyOnWriteArrayList<LayerInterface> layers;
	private static CopyOnWriteArrayList<LayerListener> layerListeners;
	private static int activeLayer = 0;
	
	private static KakaduRender renderer = new KakaduRender();
	private static boolean coronaVisibility = true;
	
	static {
		layers = new CopyOnWriteArrayList<LayerInterface>();
		layerListeners = new CopyOnWriteArrayList<LayerListener>();
	}
		
	public static LayerInterface addLayer(String uri){
		ImageLayer layer = new ImageLayer(uri, renderer);
		layers.add(layer);
		for (LayerListener renderListener : layerListeners){
			renderListener.newlayerAdded();
		}
		if (layers.size() == 1){
			layerChanged();
		}
		return layer;
	}
	
	public static ImageLayer addLayer(int id, LocalDateTime start, LocalDateTime end, int cadence){
		ImageLayer layer = new ImageLayer(id, renderer, start, end, cadence);
		layers.add(layer);
		for (LayerListener renderListener : layerListeners){
			renderListener.newlayerAdded();
		}
		if (layers.size() == 1){
			layerChanged();
		}
		return layer;
	}
	
	private static LayerInterface getLayer(int idx){
		return layers.get(idx);
	}
	
	public static void removeLayer(int idx){
		layers.get(idx).cancelDownload();
		layers.remove(idx);
		activeLayer = 0;
		for (LayerListener renderListener : layerListeners){
			renderListener.newlayerRemoved(idx);
		}
	}
		
	public static void addNewLayerListener(LayerListener renderListener){
		layerListeners.add(renderListener);
	}

	public static int getLayerCount() {
		return layers.size();
	}

	private static void layerChanged() {
		for (LayerListener renderListener : layerListeners){
			renderListener.activeLayerChanged(getLayer(activeLayer));
		}
	}
	
	public static int getActiveLayerNumber(){
		return activeLayer;
	}
	
	public static LayerInterface getActiveLayer(){
		if (layers.size() <= 0) return null;
		return layers.get(activeLayer);
	}
	
	public static void setActiveLayer(int activeLayer){
		if (Layers.activeLayer != activeLayer && getLayerCount() > 0){
			Layers.activeLayer = activeLayer;
			Layers.layerChanged();
		}
	}
	
	public static CopyOnWriteArrayList<LayerInterface> getLayers(){
		return layers;
	}

	public static void toggleCoronaVisibility() {
		coronaVisibility = !coronaVisibility;
	}
	
	public static boolean getCoronaVisibility(){
		return coronaVisibility;
	}

	public static void removeAllLayers() {
		layers.clear();
	}
}
