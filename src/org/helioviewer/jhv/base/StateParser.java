package org.helioviewer.jhv.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.JHVException.LocalFileException;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;
import org.helioviewer.jhv.plugins.plugin.UltimatePluginInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.helpers.DefaultHandler;

public class StateParser extends DefaultHandler {

	private static final String LOAD_PATH_SETTINGS = "statefile.load.path";
	private static final String SAVE_PATH_SETTINGS = "statefile.save.path";

	private static final String LAYERS = "layers";
	private static final String LOCAL_PATH = "localPath";
	private static final String ID = "id";
	private static final String CADENCE = "cadence";
	private static final String START_DATE_TIME = "startDateTime";
	private static final String END_DATE_TIME = "endDateTime";

	private static final String OPACITY = "opacity";
	private static final String SHARPEN = "sharpen";
	private static final String GAMMA = "gamma";
	private static final String CONTRAST = "contrast";
	private static final String LUT = "lut";
	private static final String RED_CHANNEL = "redChannel";
	private static final String GREEN_CHANNEL = "greenChannel";
	private static final String BLUE_CHANNEL = "blueChannel";

	private static final String VISIBILITY = "visibility";
	private static final String INVERTED_LUT = "invertedLut";
	private static final String CORONA_VISIBILITY = "coronaVisiblity";

	private static final String CAMERA = "camera";
	private static final String CAMERA_TRANSLATION = "translation";
	private static final String CAMERA_ROTATION = "rotation";

	private static final String PLUGINS = "plugins";

	private static class JHVStateFilter extends ExtensionFileFilter {

		/**
		 * Default Constructor.
		 */
		public JHVStateFilter() {
			extensions = new String[] { ".jhv" };
		}

		/**
		 * {@inheritDoc}
		 */
		public String getDescription() {
			return "JHelioviewer State files (\".jhv\")";
		}
	}

	public static void loadStateFile() throws IOException, JSONException {
		String lastPath = Settings.getProperty(LOAD_PATH_SETTINGS);
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Download imagedata");
		if (lastPath != null) {
			fileChooser.setCurrentDirectory(new File(lastPath));
		}
		fileChooser.setFileFilter(new StateParser.JHVStateFilter());
		int retVal = fileChooser.showOpenDialog(MainFrame.SINGLETON);
		if (retVal == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			Settings.setProperty(LOAD_PATH_SETTINGS, fileChooser
					.getCurrentDirectory().getAbsolutePath());
			if (selectedFile.exists() && selectedFile.isFile()) {
				Layers.removeAllLayers();
				byte[] data = Files.readAllBytes(selectedFile.toPath());
				String content = new String(data, StandardCharsets.UTF_8);
				JSONObject jsonObject = new JSONObject(content);
				JSONArray layers = jsonObject.getJSONArray(LAYERS);
				for (int i = 0; i < layers.length(); i++) {
					LayerInterface layer;
					JSONObject jsonLayer = layers.getJSONObject(i);
					if (jsonLayer.has(LOCAL_PATH)) {
						layer = Layers
								.addLayer(jsonLayer.getString(LOCAL_PATH));
					} else {
						int id = jsonLayer.getInt(ID);
						LocalDateTime start = LocalDateTime.parse(jsonLayer
								.getString(START_DATE_TIME));
						LocalDateTime end = LocalDateTime.parse(jsonLayer
								.getString(END_DATE_TIME));
						int cadence = jsonLayer.getInt(CADENCE);
						layer = Layers.addLayer(id, start, end, cadence);
					}
					layer.setOpacity(jsonLayer.getDouble(OPACITY));
					layer.setSharpen(jsonLayer.getDouble(SHARPEN));
					layer.setGamma(jsonLayer.getDouble(GAMMA));
					layer.setContrast(jsonLayer.getDouble(CONTRAST));
					layer.setLut(LUT_ENTRY.values()[jsonLayer.getInt(LUT)]);
					layer.setRedChannel(jsonLayer.getBoolean(RED_CHANNEL));
					layer.setGreenChannel(jsonLayer.getBoolean(GREEN_CHANNEL));
					layer.setBlueChannel(jsonLayer.getBoolean(BLUE_CHANNEL));

					layer.setVisible(jsonLayer.getBoolean(VISIBILITY));
					layer.setLutInverted(jsonLayer.getBoolean(INVERTED_LUT));
					layer.setCoronaVisibility(jsonLayer
							.getBoolean(CORONA_VISIBILITY));
				}

				JSONObject jsonCamera = jsonObject.getJSONObject(CAMERA);
				JSONArray jsonTranslation = jsonCamera
						.getJSONArray(CAMERA_TRANSLATION);
				Vector3d translation = new Vector3d(
						jsonTranslation.getDouble(0),
						jsonTranslation.getDouble(1),
						jsonTranslation.getDouble(2));
				JSONArray jsonRotation = jsonCamera
						.getJSONArray(CAMERA_ROTATION);
				double angle = jsonRotation.getDouble(0);
				Vector3d axis = new Vector3d(
						jsonRotation.getDouble(1),
						jsonRotation.getDouble(2),
						jsonRotation.getDouble(3));
				Quaternion3d rotation = new Quaternion3d(angle, axis);
				MainFrame.MAIN_PANEL.setTransformation(rotation, translation);
				
				JSONObject jsonPlugin = jsonObject.getJSONObject(PLUGINS);
				UltimatePluginInterface.SINGLETON.loadStateFile(jsonPlugin);
				
			}

		}
	}

	public static void writeStateFile() throws JSONException, IOException {
		String lastPath = Settings.getProperty(SAVE_PATH_SETTINGS);
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Download imagedata");
		fileChooser.setMultiSelectionEnabled(false);

		if (lastPath != null) {
			fileChooser.setCurrentDirectory(new File(lastPath));
		}
		fileChooser.setFileFilter(new StateParser.JHVStateFilter());
		int retVal = fileChooser.showSaveDialog(MainFrame.SINGLETON);
		
		if (fileChooser.getSelectedFile().exists()) {
			// ask if the user wants to overwrite
			int response = JOptionPane.showConfirmDialog(null,
					"Overwrite existing file?", "Confirm Overwrite",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE);

			// if the user doesn't want to overwrite, simply return null
			if (response == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}
		
		if (retVal == JFileChooser.APPROVE_OPTION) {
			Settings.setProperty(SAVE_PATH_SETTINGS, fileChooser
					.getCurrentDirectory().getAbsolutePath());
			String fileName = fileChooser.getSelectedFile().toString();
			JHVStateFilter fileFilter = (JHVStateFilter)fileChooser.getFileFilter();
			fileName = fileName.endsWith(fileFilter.getDefaultExtension()) ? fileName : fileName + fileFilter.getDefaultExtension();
			JSONObject jsonObject = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			for (LayerInterface layer : Layers.getLayers()) {
				JSONObject jsonLayer = new JSONObject();
				jsonArray.put(jsonLayer);
				try {
					jsonLayer.put(LOCAL_PATH, layer.getLocalFilePath());
				} catch (LocalFileException e) {
					System.out.println("Statefile include no local file");
				}

				try {
					jsonLayer.put(ID, layer.getID());
					jsonLayer.put(CADENCE, layer.getCadence());
					jsonLayer.put(START_DATE_TIME, layer.getStartDateTime());
					jsonLayer.put(END_DATE_TIME, layer.getEndDateTime());
				} catch (LocalFileException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				jsonLayer.put(OPACITY, layer.getOpacity());
				jsonLayer.put(SHARPEN, layer.getSharpen());
				jsonLayer.put(GAMMA, layer.getGamma());
				jsonLayer.put(CONTRAST, layer.getContrast());
				jsonLayer.put(LUT, layer.getLut().ordinal());
				jsonLayer.put(RED_CHANNEL, layer.isRedChannelActive());
				jsonLayer.put(GREEN_CHANNEL, layer.isGreenChannelActive());
				jsonLayer.put(BLUE_CHANNEL, layer.isBlueChannelActive());

				jsonLayer.put(VISIBILITY, layer.isVisible());
				jsonLayer.put(INVERTED_LUT, layer.isLutInverted());
				jsonLayer.put(CORONA_VISIBILITY, layer.isCoronaVisible());

			}
			jsonObject.put(LAYERS, jsonArray);

			JSONObject jsonCamera = new JSONObject();

			JSONArray jsonTranslation = new JSONArray();
			Vector3d translation = MainFrame.MAIN_PANEL.getTranslation();
			jsonTranslation.put(translation.x);
			jsonTranslation.put(translation.y);
			jsonTranslation.put(translation.z);
			jsonCamera.put(CAMERA_TRANSLATION, jsonTranslation);

			JSONArray jsonRotation = new JSONArray();
			Quaternion3d rotation = MainFrame.MAIN_PANEL.getRotation();
			jsonRotation.put(rotation.getAngle());
			Vector3d axis = rotation.getRotationAxis();
			jsonRotation.put(axis.x);
			jsonRotation.put(axis.y);
			jsonRotation.put(axis.z);
			jsonCamera.put(CAMERA_ROTATION, jsonRotation);

			jsonObject.put(CAMERA, jsonCamera);

			JSONObject jsonPlugins = new JSONObject();
			UltimatePluginInterface.SINGLETON.writeStateFile(jsonPlugins);
			jsonObject.put(PLUGINS, jsonPlugins);

			FileWriter file = new FileWriter(fileName);
			file.write(jsonObject.toString());
			file.flush();
			file.close();
		}
	}
}
