package org.helioviewer.jhv.base;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.helioviewer.jhv.JHVGlobals;
import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.actions.filefilters.ExtensionFileFilter;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.plugins.plugin.UltimatePluginInterface;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
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

	private static final String TIME = "time";
	private static final String ACTIVE_LAYER = "activeLayer";

	private static final String PLUGINS = "plugins";

	private static class JHVStateFilter extends ExtensionFileFilter {

		private static final String DESCRIPTION = "JHelioviewer State files (\"*.jhv\")";
		private static final String EXTENSION = ".jhv";

		/**
		 * Default Constructor.
		 */
		public JHVStateFilter() {
			extensions = new String[] { EXTENSION };
		}

		/**
		 * {@inheritDoc}
		 */
		public String getDescription() {
			return DESCRIPTION;
		}

		public static ExtensionFilter getExtensionFilter() {
			return new ExtensionFilter(DESCRIPTION, "*" + EXTENSION);
		}
	}

	private static void openLoadFileChooserFX() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Open state file");
				fileChooser.setInitialDirectory(new File(Settings
						.getProperty(LOAD_PATH_SETTINGS)));

				ExtensionFilter extensionFilter1 = new ExtensionFilter(
						"All Files", "*.*");
				fileChooser.getExtensionFilters().addAll(
						StateParser.JHVStateFilter.getExtensionFilter(),
						extensionFilter1);
				final File selectedFile = fileChooser
						.showOpenDialog(new Stage());

				if (selectedFile != null && selectedFile.exists()
						&& selectedFile.isFile()) {

					// remember the current directory for future
					Settings.setProperty(LOAD_PATH_SETTINGS,
							selectedFile.getParent());
					try {
						startLoadingStateFile(selectedFile);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

		});
	}

	public static void loadStateFile() throws IOException, JSONException {
		String lastPath = Settings.getProperty(LOAD_PATH_SETTINGS);
		if (JHVGlobals.isFXAvailable()) {
			openLoadFileChooserFX();
		} else {
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Open state file");
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
					startLoadingStateFile(selectedFile);
				}

			}
		}
	}

	private static void startLoadingStateFile(File selectedFile)
			throws IOException, JSONException {
		Layers.removeAllImageLayers();
		byte[] data = Files.readAllBytes(selectedFile.toPath());
		String content = new String(data, StandardCharsets.UTF_8);
		JSONObject jsonObject = new JSONObject(content);
		JSONArray layers = jsonObject.getJSONArray(LAYERS);
		Layers.readStatefile(layers);

		JSONObject jsonCamera = jsonObject.getJSONObject(CAMERA);
		JSONArray jsonTranslation = jsonCamera.getJSONArray(CAMERA_TRANSLATION);
		Vector3d translation = new Vector3d(jsonTranslation.getDouble(0),
				jsonTranslation.getDouble(1), jsonTranslation.getDouble(2));
		JSONArray jsonRotation = jsonCamera.getJSONArray(CAMERA_ROTATION);
		double angle = jsonRotation.getDouble(0);
		Vector3d axis = new Vector3d(jsonRotation.getDouble(1),
				jsonRotation.getDouble(2), jsonRotation.getDouble(3));
		Quaternion3d rotation = new Quaternion3d(angle, axis);
		MainFrame.MAIN_PANEL.setTransformation(rotation, translation);

		Layers.setActiveLayer(jsonObject.getInt(ACTIVE_LAYER));
		LocalDateTime currentDateTime = LocalDateTime.parse(jsonObject
				.getString(TIME));
		TimeLine.SINGLETON.setCurrentDate(currentDateTime);

		JSONObject jsonPlugin = jsonObject.getJSONObject(PLUGINS);
		UltimatePluginInterface.SINGLETON.loadStateFile(jsonPlugin);
	}

	private static void startSavingStateFile(File selectedFile)
			throws JSONException, IOException {
		Settings.setProperty(SAVE_PATH_SETTINGS, selectedFile.getParent());
		String fileName = selectedFile.toString();
		JHVStateFilter fileFilter = new JHVStateFilter();
		fileName = fileName.endsWith(fileFilter.getDefaultExtension()) ? fileName
				: fileName + fileFilter.getDefaultExtension();
		JSONObject jsonObject = new JSONObject();

		JSONArray jsonLayers = new JSONArray();
		Layers.writeStatefile(jsonLayers);
		jsonObject.put(LAYERS, jsonLayers);

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
		jsonObject.put(ACTIVE_LAYER, Layers.getActiveLayerNumber());
		jsonObject.put(TIME, TimeLine.SINGLETON.getCurrentDateTime());

		JSONObject jsonPlugins = new JSONObject();
		UltimatePluginInterface.SINGLETON.writeStateFile(jsonPlugins);
		jsonObject.put(PLUGINS, jsonPlugins);

		FileWriter file = new FileWriter(fileName);
		file.write(jsonObject.toString());
		file.flush();
		file.close();
	}

	private static void openSaveFileChooserFX() {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {

				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save state file");

				String val = Settings.getProperty(SAVE_PATH_SETTINGS);
				File file = new File(val);
				if (val != null && !(val.length() == 0) && file.exists()) {
					fileChooser.setInitialDirectory(file);
				}

				fileChooser.getExtensionFilters().addAll(
						StateParser.JHVStateFilter.getExtensionFilter());

				final File selectedFile = fileChooser
						.showSaveDialog(new Stage());

				if (selectedFile != null) {
					try {
						startSavingStateFile(selectedFile);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
	}

	public static void writeStateFile() throws JSONException, IOException {
		if (JHVGlobals.isFXAvailable()) {
			openSaveFileChooserFX();
		} else {
			String lastPath = Settings.getProperty(SAVE_PATH_SETTINGS);
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setDialogTitle("Save state file");
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
				startSavingStateFile(fileChooser.getSelectedFile());
			}
		}
	}
}
