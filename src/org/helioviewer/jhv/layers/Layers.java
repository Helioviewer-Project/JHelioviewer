package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.Comparator;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Layers
{
	private static ArrayList<LayerListener> layerListeners = new ArrayList<LayerListener>();
	private static ArrayList<Layer> layers = new ArrayList<Layer>();
	private static int activeLayerIndex = -1;

	private static final Comparator<Layer> COMPARATOR = new Comparator<Layer>()
	{
		@Override
		public int compare(@Nullable Layer o1, @Nullable Layer o2)
		{
			if (o1==null)
				return 1;
			else if (o2==null)
				return -1;
			else if (!(o1 instanceof ImageLayer) && o2 instanceof ImageLayer)
				return 1;
			else if (o1 instanceof ImageLayer && !(o2 instanceof ImageLayer))
				return -1;
			else
				return 0;
		}
	};
	
	private static void updateOpacity(ImageLayer imageLayer, boolean remove)
	{
		int counter = 0;
		for (Layer tmpLayer : layers)
			if (tmpLayer instanceof ImageLayer)
				counter++;
		
		for (Layer tmpLayer : layers)
			if (tmpLayer instanceof ImageLayer)
			{
				ImageLayer tmpImageLayer = (ImageLayer) tmpLayer;
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

	public static void addLayer(Layer _newLayer)
	{
		layers.add(_newLayer);
		layers.sort(COMPARATOR);
		
		Telemetry.trackEvent("Layer added", "Name", _newLayer.getName(), "Full name", _newLayer.getFullName());
		
		if (_newLayer instanceof ImageLayer)
			updateOpacity((ImageLayer)_newLayer, false);
		
		for (LayerListener listener : layerListeners)
			listener.layerAdded();
		
		for(int i=0;i<layers.size();i++)
			if(layers.get(i)==_newLayer)
				setActiveLayer(i);
	}

	public static @Nullable Layer getLayer(int idx)
	{
		if(idx==-1)
			return null;
		
		return layers.get(idx);
	}
	
	public static void removeLayer(Layer _l)
	{
		int index=layers.indexOf(_l);
		if(index==-1)
			return;
		
		removeLayer(index);
	}
	
	public static void removeLayer(int _idx)
	{
		if (layers.isEmpty())
			return;
		
		if (!(layers.get(_idx) instanceof ImageLayer))
			return;
		
		ImageLayer.newRenderPassStarted();
		
		updateOpacity((ImageLayer)layers.get(_idx), true);
		
		layers.get(_idx).dispose();
		layers.remove(_idx);
		
		if (layers.isEmpty() || activeLayerIndex==_idx)
			setActiveLayer(-1);
		else if(activeLayerIndex>_idx)
			setActiveLayer(activeLayerIndex-1);
		
		for (LayerListener renderListener : layerListeners)
			renderListener.layersRemoved();
	}

	public static void addLayerListener(LayerListener renderListener)
	{
		layerListeners.add(renderListener);
	}

	public static boolean anyImageLayers()
	{
		for (Layer tmpLayer : layers)
			if (tmpLayer instanceof ImageLayer)
				return true;
		return false;
	}

	public static boolean anyLayers()
	{
		return !layers.isEmpty();
	}

	public static int getActiveLayerIndex()
	{
		return activeLayerIndex;
	}

	@Nullable
	public static Layer getActiveLayer()
	{
		if (activeLayerIndex >= 0)
			return layers.get(activeLayerIndex);
		
		return null;
	}

	public static void setActiveLayer(int _newActiveLayer)
	{
		activeLayerIndex = _newActiveLayer;
		
		Layer l=getLayer(activeLayerIndex);
		for (LayerListener renderListener : layerListeners)
			renderListener.activeLayerChanged(l);
	}
	
	public static ArrayList<Layer> getLayers()
	{
		return layers;
	}

	public static void toggleCoronaVisibility()
	{
		ImageLayer il = getActiveImageLayer();
		if(il != null)
		{
			il.toggleCoronaVisibility();
			MainFrame.SINGLETON.MAIN_PANEL.repaint();
		}
	}

	public static void removeAllImageLayers()
	{
		for (Layer layer : layers)
			if (layer instanceof ImageLayer)
				layer.dispose();
	
		ImageLayer.newRenderPassStarted();
		
		layers.clear();
		
		activeLayerIndex = 0;
		for (LayerListener renderListener : layerListeners)
			renderListener.layersRemoved();
	}

	public static void writeStatefile(JSONArray jsonLayers)
	{
		for (Layer layer : layers)
		{
			JSONObject jsonLayer = new JSONObject();
			layer.storeConfiguration(jsonLayer);
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
	public static ImageLayer getActiveImageLayer()
	{
		if (activeLayerIndex == -1 || !(layers.get(activeLayerIndex) instanceof ImageLayer))
			return null;
		
		return (ImageLayer) layers.get(activeLayerIndex);
	}
}
