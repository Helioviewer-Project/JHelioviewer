package org.helioviewer.jhv.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;

import org.helioviewer.jhv.base.Globals.DialogType;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.PredefinedFileFilter;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.Plugins;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.helpers.DefaultHandler;

public class StateParser extends DefaultHandler
{
	private static final String LOAD_PATH_SETTINGS = "statefile.load.path";
	private static final String SAVE_PATH_SETTINGS = "statefile.save.path";
	
	public static void loadStateFile() throws IOException, JSONException
	{
		File selectedFile = Globals.showFileDialog(
				DialogType.OPEN_FILE,
				"Open State File",
				Settings.getProperty(LOAD_PATH_SETTINGS),
				true,
				null,
				PredefinedFileFilter.JHV
			);
		
		if (selectedFile!=null)
		{
			Settings.setProperty(LOAD_PATH_SETTINGS, selectedFile.getParentFile().getAbsolutePath());
			loadStateFile(selectedFile);
		}
	}

	public static void loadStateFile(File selectedFile) throws IOException, JSONException
	{
		Layers.removeAllImageLayers();
		byte[] data = Files.readAllBytes(selectedFile.toPath());
		String content = new String(data, StandardCharsets.UTF_8);
		JSONObject jsonObject = new JSONObject(content);
		JSONArray layers = jsonObject.getJSONArray("layers");
		Layers.readStatefile(layers);

		JSONObject jsonCamera = jsonObject.getJSONObject("camera");
		JSONArray jsonTranslation = jsonCamera.getJSONArray("translation");
		Vector3d translation = new Vector3d(jsonTranslation.getDouble(0), jsonTranslation.getDouble(1), jsonTranslation.getDouble(2));
		JSONArray jsonRotation = jsonCamera.getJSONArray("rotation");
		double angle = jsonRotation.getDouble(0);
		Vector3d axis = new Vector3d(jsonRotation.getDouble(1), jsonRotation.getDouble(2), jsonRotation.getDouble(3));
		Quaternion3d rotation = new Quaternion3d(angle, axis);
		MainFrame.SINGLETON.MAIN_PANEL.stopAllAnimations();
		MainFrame.SINGLETON.MAIN_PANEL.setRotationEnd(rotation);
		MainFrame.SINGLETON.MAIN_PANEL.setRotationCurrent(rotation);
		MainFrame.SINGLETON.MAIN_PANEL.setTranslationEnd(translation);
		MainFrame.SINGLETON.MAIN_PANEL.setTranslationCurrent(translation);
		
		Layers.setActiveLayer(jsonObject.getInt("activeLayer"));
		LocalDateTime currentDateTime = LocalDateTime.parse(jsonObject.getString("time"));
		TimeLine.SINGLETON.setCurrentDate(currentDateTime);

		JSONObject jsonPlugin = jsonObject.getJSONObject("plugins");
		Plugins.SINGLETON.restoreConfiguration(jsonPlugin);
	}

	private static void startSavingStateFile(File selectedFile) throws JSONException, IOException
	{
		Settings.setProperty(SAVE_PATH_SETTINGS, selectedFile.getParent());
		String fileName = selectedFile.toString();
		fileName = fileName.endsWith(PredefinedFileFilter.JHV.getDefaultExtension()) ? fileName
				: fileName + PredefinedFileFilter.JHV.getDefaultExtension();
		JSONObject jsonObject = new JSONObject();

		JSONArray jsonLayers = new JSONArray();
		Layers.writeStatefile(jsonLayers);
		jsonObject.put("layers", jsonLayers);

		JSONObject jsonCamera = new JSONObject();

		JSONArray jsonTranslation = new JSONArray();
		Vector3d translation = MainFrame.SINGLETON.MAIN_PANEL.getTranslationCurrent();
		jsonTranslation.put(translation.x);
		jsonTranslation.put(translation.y);
		jsonTranslation.put(translation.z);
		jsonCamera.put("translation", jsonTranslation);

		JSONArray jsonRotation = new JSONArray();
		Quaternion3d rotation = MainFrame.SINGLETON.MAIN_PANEL.getRotationCurrent();
		jsonRotation.put(rotation.getAngle());
		Vector3d axis = rotation.getRotationAxis();
		jsonRotation.put(axis.x);
		jsonRotation.put(axis.y);
		jsonRotation.put(axis.z);
		jsonCamera.put("rotation", jsonRotation);

		jsonObject.put("camera", jsonCamera);
		jsonObject.put("activeLayer", Layers.getActiveLayerIndex());
		jsonObject.put("time", TimeLine.SINGLETON.getCurrentDateTime());

		JSONObject jsonPlugins = new JSONObject();
		Plugins.SINGLETON.storeConfiguration(jsonPlugins);
		jsonObject.put("plugins", jsonPlugins);
		
		try(Writer w = new OutputStreamWriter(new FileOutputStream(fileName),StandardCharsets.UTF_8))
		{
			w.write(jsonObject.toString());
		}
	}

	public static void writeStateFile() throws JSONException, IOException
	{
		File selectedFile = Globals.showFileDialog(DialogType.SAVE_FILE,
				"Save state file",
				Settings.getProperty(SAVE_PATH_SETTINGS),
				true,
				null,
				PredefinedFileFilter.JHV);
		
		if(selectedFile!=null)
			startSavingStateFile(selectedFile);
	}
}
