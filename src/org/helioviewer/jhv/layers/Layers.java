package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.Comparator;

import javax.annotation.Nullable;

import org.helioviewer.jhv.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Layers
{
	private static ArrayList<LayerListener> layerListeners = new ArrayList<LayerListener>();
	private static ArrayList<AbstractLayer> layers = new ArrayList<AbstractLayer>();
	private static int activeLayerIndex = -1;

	private static final Comparator<AbstractLayer> COMPARATOR = new Comparator<AbstractLayer>()
	{
		@Override
		public int compare(AbstractLayer o1, AbstractLayer o2)
		{
			if (!o1.isImageLayer() && o2.isImageLayer())
				return 1;
			else if (o1.isImageLayer() && !o2.isImageLayer())
				return -1;
			else
				return 0;
		}
	};
	
	private static void updateOpacity(AbstractImageLayer imageLayer, boolean remove)
	{
		int counter = 0;
		for (AbstractLayer tmpLayer : layers)
			if (tmpLayer.isImageLayer())
				counter++;
		
		for (AbstractLayer tmpLayer : layers)
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

	public static void addLayer(AbstractLayer _newLayer)
	{
		layers.add(_newLayer);
		layers.sort(COMPARATOR);
		
		if (_newLayer.isImageLayer())
			updateOpacity((AbstractImageLayer)_newLayer, false);
		
		for (LayerListener renderListener : layerListeners)
			renderListener.layerAdded();
		
		for(int i=0;i<layers.size();i++)
			if(layers.get(i)==_newLayer)
				setActiveLayer(i);
	}

	public static AbstractLayer getLayer(int idx)
	{
		if (idx >= 0)
			return layers.get(idx);
		return null;
	}

	public static void removeLayer(int idx)
	{
		if (layers.isEmpty())
			return;
		
		if (!layers.get(idx).isImageLayer())
			return;
		
		AbstractImageLayer.newRenderPassStarted();
		
		updateOpacity((AbstractImageLayer)layers.get(idx), true);
		
		layers.get(idx).dispose();
		layers.remove(idx);
		
		if (layers.isEmpty() || activeLayerIndex==idx)
			setActiveLayer(-1);
		else if(activeLayerIndex>idx)
			setActiveLayer(activeLayerIndex-1);
		
		for (LayerListener renderListener : layerListeners)
			renderListener.layersRemoved();
	}

	public static void addLayerListener(LayerListener renderListener)
	{
		layerListeners.add(renderListener);
	}

	public static int getLayerCount()
	{
		return layers.size();
	}

	public static int getActiveLayerIndex()
	{
		return activeLayerIndex;
	}

	@Nullable
	public static AbstractLayer getActiveLayer()
	{
		if (activeLayerIndex >= 0)
			return layers.get(activeLayerIndex);
		
		return null;
	}

	public static void setActiveLayer(int _newActiveLayer)
	{
		activeLayerIndex = _newActiveLayer;
		
		AbstractLayer l=getLayer(activeLayerIndex);
		for (LayerListener renderListener : layerListeners)
			renderListener.activeLayerChanged(l);
	}

	public static ArrayList<AbstractLayer> getLayers()
	{
		return layers;
	}

	public static void toggleCoronaVisibility()
	{
		AbstractImageLayer il = getActiveImageLayer();
		if(il != null)
		{
			il.toggleCoronaVisibility();
			MainFrame.MAIN_PANEL.repaint();
		}
	}

	public static void removeAllImageLayers()
	{
		for (AbstractLayer layer : layers)
			if (layer.isImageLayer())
				layer.dispose();
	
		AbstractImageLayer.newRenderPassStarted();
		
		layers.clear();
		
		activeLayerIndex = 0;
		for (LayerListener renderListener : layerListeners)
			renderListener.layersRemoved();
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
			try
			{
				JSONObject jsonLayer = jsonLayers.getJSONObject(i);
				KakaduLayer layer = KakaduLayer.createFromStateFile(jsonLayer);
				if (layer != null)
				{
					Layers.addLayer(layer);
					layer.readStateFile(jsonLayer);
				}
			}
			catch (JSONException | UnsuitableMetaDataException e)
			{
				Telemetry.trackException(e);
			}
	}

	@Nullable
	public static AbstractImageLayer getActiveImageLayer()
	{
		if (activeLayerIndex == -1 || !(layers.get(activeLayerIndex) instanceof AbstractImageLayer))
			return null;
		
		return (AbstractImageLayer) layers.get(activeLayerIndex);
	}
}
