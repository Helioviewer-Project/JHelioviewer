package org.helioviewer.jhv.layers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;

import org.helioviewer.jhv.gui.MainFrame;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Layers
{
	private static ArrayList<LayerListener> layerListeners;
	private static ArrayList<AbstractLayer> layers;
	private static int activeLayer = -1;
	private static int activeImageLayer = -1;

	private static final Comparator<AbstractLayer> COMPARATOR = new Comparator<AbstractLayer>()
	{
		@Override
		public int compare(AbstractLayer o1, AbstractLayer o2)
		{
			if (!o1.isImageLayer && o2.isImageLayer)
				return 1;
			else if (o1.isImageLayer && !o2.isImageLayer)
				return -1;
			else
				return 0;
		}
	};

	
	static
	{
		layers = new ArrayList<AbstractLayer>();
		layerListeners = new ArrayList<LayerListener>();
	}

	public static AbstractLayer addLayer(String uri)
	{
		ImageLayer layer = new ImageLayer(uri);
		layers.add(layer);
		layers.sort(COMPARATOR);
		updateOpacity(layer, false);
		if (layers.size() == 1 || activeImageLayer < 0) setActiveLayer(0);
		for (LayerListener renderListener : layerListeners)
			renderListener.newLayerAdded();
		
		if (layers.size() == 1)
			layerChanged();
		
		return layer;
	}
	
	private static void updateOpacity(AbstractImageLayer imageLayer, boolean remove){
		int counter = 0;
		for (AbstractLayer tmpLayer : layers)
			if (tmpLayer.isImageLayer())
				counter++;
		
		for (AbstractLayer tmpLayer : layers)
		{
			if (tmpLayer.isImageLayer())
			{
				AbstractImageLayer tmpImageLayer = (AbstractImageLayer) tmpLayer;
				if (tmpImageLayer == imageLayer)
					tmpImageLayer.opacity = 1d/counter;
				else
				{
					if (remove)
						tmpImageLayer.opacity /= ((counter-1d) / counter);
					else
						tmpImageLayer.opacity *= ((counter-1d) / counter);
				}
			}
		}
	}

	public static void addLayer(AbstractLayer layer)
	{
		layers.add(layer);
		layers.sort(COMPARATOR);
		if (layers.size() == 1 || activeImageLayer < 0) setActiveLayer(0);
		if (layer.isImageLayer()) updateOpacity((AbstractImageLayer)layer, false);
		for (LayerListener renderListener : layerListeners)
			renderListener.newLayerAdded();
		
		if (layers.size() == 1)
			layerChanged();
	}

	public static ImageLayer addLayer(int id, LocalDateTime start, LocalDateTime end, int cadence, String name)
	{
		ImageLayer layer = new ImageLayer(id, start, end, cadence, name);
		layers.add(layer);
		layers.sort(COMPARATOR);
		updateOpacity(layer, false);
		
		if (layers.size() == 1 || activeImageLayer < 0) setActiveLayer(0);
		for (LayerListener renderListener : layerListeners) {
			renderListener.newLayerAdded();
		}
		if (layers.size() == 1) {
			layerChanged();
		}
		return layer;
	}

	public static AbstractLayer getLayer(int idx) {
		if (idx >= 0 && idx < layers.size())
			return layers.get(idx);
		return null;
	}

	public static void removeLayer(int idx) {
		if (!layers.isEmpty()) {
			if (layers.get(idx).isImageLayer()){
				updateOpacity((AbstractImageLayer)layers.get(idx), true);				
			}
			layers.get(idx).remove();
			layers.remove(idx);
			if (layers.isEmpty())
				activeLayer = -1;
			int counter = 0;
			for (AbstractLayer layer : layers){
				if (layer.isImageLayer()){
					activeImageLayer = counter;					
					break;
				}
				counter++;
			}
			if (counter != activeImageLayer){
				activeImageLayer = -1;
				layerChanged();
			}
			for (LayerListener renderListener : layerListeners) {
				renderListener.newlayerRemoved(idx);
			}
		}
	}

	public static void addNewLayerListener(LayerListener renderListener) {
		layerListeners.add(renderListener);
	}

	public static int getLayerCount() {
		return layers.size();
	}

	private static void layerChanged() {
		if (activeLayer >= 0) {
			for (LayerListener renderListener : layerListeners) {
				renderListener.activeLayerChanged(getLayer(activeLayer));
			}
		}
	}

	public static int getActiveLayerNumber() {
		return activeLayer;
	}

	public static AbstractLayer getActiveLayer() {
		if (layers.size() > 0 && activeLayer >= 0)
			return layers.get(activeLayer);
		return null;
	}

	public static void setActiveLayer(int activeLayer) {
		if ((Layers.activeLayer != activeLayer || Layers.activeImageLayer < 0) && getLayerCount() > 0) {
			Layers.activeLayer = activeLayer;
			if (getActiveLayer() != null && getActiveLayer().isImageLayer())
				Layers.activeImageLayer = activeLayer;
			Layers.layerChanged();
		}
	}

	public static ArrayList<AbstractLayer> getLayers() {
		return layers;
	}

	public static void toggleCoronaVisibility()
	{
		AbstractLayer il = getActiveLayer();
		if(il instanceof AbstractImageLayer)
		{
			((AbstractImageLayer)il).toggleCoronaVisibility();
			MainFrame.MAIN_PANEL.repaint();
		}
	}

	public static void removeAllImageLayers()
	{
		for (AbstractLayer layer : layers)
		{
			if (layer.isImageLayer)
			{
				layer.remove();
				layers.remove(layer);
			}
		}
		activeLayer = 0;
		for (LayerListener renderListener : layerListeners)
			renderListener.newlayerRemoved(0);
	}

	public static void writeStatefile(JSONArray jsonLayers)
	{
		for (AbstractLayer layer : layers)
		{
			JSONObject jsonLayer = new JSONObject();
			layer.writeStateFile(jsonLayer);
			jsonLayers.put(jsonLayer);
		}
	}

	public static void readStatefile(JSONArray jsonLayers)
	{
		for (int i = 0; i < jsonLayers.length(); i++)
		{
			try
			{
				JSONObject jsonLayer = jsonLayers.getJSONObject(i);
				AbstractImageLayer layer = ImageLayer.createFromStateFile(jsonLayer);
				if (layer != null)
				{
					Layers.addLayer(layer);
					layer.readStateFile(jsonLayer);
				}
			}
			catch (JSONException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static AbstractImageLayer getActiveImageLayer() {
		if (activeImageLayer >= 0)
			return (AbstractImageLayer) layers.get(activeImageLayer);
		return null;
	}
}
