package org.helioviewer.jhv.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;

import org.helioviewer.jhv.base.Settings.StringKey;
import org.helioviewer.jhv.base.math.MathUtils;
import org.helioviewer.jhv.base.math.Quaternion;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.camera.CameraMode;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StateParser
{
	public static void loadStateFile(File selectedFile) throws IOException, JSONException
	{
		JSONObject json = new JSONObject(new String(Files.readAllBytes(selectedFile.toPath()), StandardCharsets.UTF_8));
		
		JSONArray layers = json.getJSONArray("layers");
		switch(json.getInt("version"))
		{
			case 1:
				//upgrade from v1 save states to v2
				json.put("timeMS", MathUtils.fromLDT(LocalDateTime.parse(json.getString("time"))));
				
				for(int i=0;i<layers.length();i++)
				{
					JSONObject layer = layers.getJSONObject(i);
					if(layer.has("startDateTime"))
						layer.put("startTimeMS", MathUtils.fromLDT(LocalDateTime.parse(layer.getString("startDateTime"))));
					if(layer.has("endDateTime"))
						layer.put("endTimeMS", MathUtils.fromLDT(LocalDateTime.parse(layer.getString("endDateTime"))));
					if(layer.has("cadence"))
						layer.put("cadenceMS", layer.getLong("cadence")*1000);
				}
				
			case 2:
				break;
			default:
				throw new JSONException("Unsupported version ("+json.getInt("version")+")");
		}
		
		Layers.removeAllImageLayers();
		Layers.loadStatefile(layers);
		
		JSONObject jsonCamera = json.getJSONObject("camera");
		JSONArray jsonTranslation = jsonCamera.getJSONArray("translation");
		Vector3d translation = new Vector3d(jsonTranslation.getDouble(0), jsonTranslation.getDouble(1), jsonTranslation.getDouble(2));
		JSONArray jsonRotation = jsonCamera.getJSONArray("rotation");
		Quaternion rotation = Quaternion.createRotation(jsonRotation.getDouble(0), new Vector3d(jsonRotation.getDouble(1), jsonRotation.getDouble(2), jsonRotation.getDouble(3)));
		MainFrame.SINGLETON.MAIN_PANEL.abortAllAnimations();
		
		MainFrame.SINGLETON.MAIN_PANEL.setRotationEnd(rotation);
		MainFrame.SINGLETON.MAIN_PANEL.setRotationCurrent(rotation);
		MainFrame.SINGLETON.MAIN_PANEL.setTranslationEnd(translation);
		MainFrame.SINGLETON.MAIN_PANEL.setTranslationCurrent(translation);
		
		switch(jsonCamera.getString("mode"))
		{
			case "2D":
				CameraMode.set2DMode();
				break;
			case "3D":
				CameraMode.set3DMode();
				break;
			default:
				throw new RuntimeException("Mode: "+CameraMode.mode);
		}
		
		MainFrame.SINGLETON.MAIN_PANEL.setCameraTrackingEnabled(jsonCamera.getBoolean("tracking"));
		
		//TODO: save and restore playback speed
		
		Layers.setActiveLayer(json.getInt("activeLayer"));
		TimeLine.SINGLETON.setCurrentTimeMS(json.getLong("timeMS"));

		MainFrame.SINGLETON.FILTER_PANEL.update();
		MainFrame.SINGLETON.repaint();
	}

	public static void saveStateFile(File selectedFile) throws JSONException, IOException
	{
		Settings.setString(StringKey.STATE_DIRECTORY, selectedFile.getParent());
		String fileName = selectedFile.toString();
		fileName = fileName.endsWith(PredefinedFileFilter.JHV.getDefaultExtension()) ? fileName
				: fileName + PredefinedFileFilter.JHV.getDefaultExtension();
		JSONObject json = new JSONObject();
		json.put("version", 2);

		JSONArray jsonLayers = new JSONArray();
		Layers.writeStatefile(jsonLayers);
		json.put("layers", jsonLayers);

		JSONObject jsonCamera = new JSONObject();

		JSONArray jsonTranslation = new JSONArray();
		Vector3d translation = MainFrame.SINGLETON.MAIN_PANEL.getTranslationCurrent();
		jsonTranslation.put(translation.x);
		jsonTranslation.put(translation.y);
		jsonTranslation.put(translation.z);
		jsonCamera.put("translation", jsonTranslation);

		JSONArray jsonRotation = new JSONArray();
		Quaternion rotation = MainFrame.SINGLETON.MAIN_PANEL.getRotationCurrent();
		jsonRotation.put(rotation.getAngle());
		Vector3d axis = rotation.getRotationAxis();
		jsonRotation.put(axis.x);
		jsonRotation.put(axis.y);
		jsonRotation.put(axis.z);
		jsonCamera.put("rotation", jsonRotation);
		switch(CameraMode.mode)
		{
			case MODE_2D:
				jsonCamera.put("mode", "2D");
				break;
			case MODE_3D:
				jsonCamera.put("mode", "3D");
				break;
			default:
				throw new RuntimeException("Mode: "+CameraMode.mode);
		}
		
		jsonCamera.put("tracking", MainFrame.SINGLETON.MAIN_PANEL.isCameraTrackingEnabled());

		json.put("camera", jsonCamera);
		json.put("activeLayer", Layers.getActiveLayerIndex());
		json.put("timeMS", TimeLine.SINGLETON.getCurrentTimeMS());

		try(Writer w = new OutputStreamWriter(new FileOutputStream(fileName),StandardCharsets.UTF_8))
		{
			w.write(json.toString());
		}
	}
}
