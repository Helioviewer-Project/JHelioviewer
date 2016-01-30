package org.helioviewer.jhv.layers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.viewmodel.jp2view.newjpx.KakaduLayer;
import org.helioviewer.jhv.viewmodel.metadata.UnsuitableMetaDataException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Layers
{
	private static ArrayList<LayerListener> layerListeners = new ArrayList<>();
	private static ArrayList<Layer> layers = new ArrayList<>();
	private static int activeLayerIndex = -1;

	static void updateOpacities(ImageLayer _layer, boolean remove)
	{
		List<ImageLayer> affected = new ArrayList<ImageLayer>(layers.size());
		
		double rSum=0;
		double gSum=0;
		double bSum=0;
		for (Layer l : layers)
			if(l != _layer)
				if (l instanceof ImageLayer)
				{
					ImageLayer il=(ImageLayer)l;
					if(il.isMetadataInitialized())
						if((il.getGroupForOpacity() & _layer.getGroupForOpacity()) != 0)
						{
							if(il.redChannel)
								rSum += il.lut.getAvgColor().getRed()*il.opacity;
							if(il.greenChannel)
								gSum += il.lut.getAvgColor().getGreen()*il.opacity;
							if(il.blueChannel)
								bSum += il.lut.getAvgColor().getBlue()*il.opacity;
							
							affected.add(il);
						}
				}
		
		if(!remove)
		{
			rSum *= affected.size();
			gSum *= affected.size();
			bSum *= affected.size();
			for(ImageLayer l : affected)
				l.opacity *= affected.size();
			
			System.out.println(_layer.lut);
			System.out.println(_layer.lut.getAvgColor());
			if(_layer.redChannel)
				rSum += _layer.lut.getAvgColor().getRed()*_layer.opacity;
			if(_layer.greenChannel)
				gSum += _layer.lut.getAvgColor().getGreen()*_layer.opacity;
			if(_layer.blueChannel)
				bSum += _layer.lut.getAvgColor().getBlue()*_layer.opacity;
			
			if(rSum==0 && gSum==0 && bSum==0)
				return;
			
			double scale=255d/MathUtils.max(rSum,gSum,bSum);
			if(scale>1)
				scale=1;
			
			for(ImageLayer l : affected)
				l.opacity *= Math.min(1d/affected.size(), scale);
			_layer.opacity *= scale;
		}
		else
		{
			rSum *= affected.size();
			gSum *= affected.size();
			bSum *= affected.size();
			for(ImageLayer l : affected)
				l.opacity *= affected.size();
			
			if(rSum==0 && gSum==0 && bSum==0)
				return;
			
			double scale=1/MathUtils.max(rSum,gSum,bSum);
			if(scale<1)
				scale=1;
			
			for(ImageLayer l : affected)
				l.opacity *= scale;
		}
		
		MainFrame.SINGLETON.FILTER_PANEL.update();
	}

	public static void addLayer(Layer _newLayer)
	{
		layers.add(_newLayer);
		layers.sort(new Comparator<Layer>()
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
		});
		
		Telemetry.trackEvent("Layer added", "Name", _newLayer.getName(), "Full name", _newLayer.getFullName());
		
		/*if (_newLayer instanceof ImageLayer)
			updateOpacities((ImageLayer)_newLayer, false);*/
		
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
		
		updateOpacities((ImageLayer)layers.get(_idx), true);
		
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
