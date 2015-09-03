package org.helioviewer.jhv.plugins.pfssplugin;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressed;
import org.helioviewer.jhv.plugins.pfssplugin.data.managers.FrameManager;
import org.helioviewer.jhv.plugins.plugin.AbstractPlugin;
import org.helioviewer.jhv.plugins.plugin.Plugins;
import org.json.JSONException;
import org.json.JSONObject;

import com.jogamp.opengl.GL2;

public class PfssPlugin extends AbstractPlugin {
	private static final String JSON_NAME = "pfss";
	private static final String JSON_OPEN = "pfssOpen";
	private static final String JSON_VISIBLE = "pfssVisible";

	private static final String NAME = "PFSS plugin";
	private static final String PLUGIN_NAME = "PFSS";
	private static int threadNumber = 0;
	public static final ExecutorService pool = Executors.newFixedThreadPool(2,
			new ThreadFactory() {
				@Override
				public Thread newThread(Runnable _r) {
					Thread t = Executors.defaultThreadFactory().newThread(_r);
					t.setName("PFSS-" + (threadNumber++));
					t.setDaemon(true);
					return t;
				}
			});

	private FrameManager manager;
	private boolean isVisible = false;
	private PfssPluginPanel pfssPluginPanel;

	public PfssPlugin() {
		super(PLUGIN_NAME);
		renderMode = RENDER_MODE.MAIN_PANEL;
		manager = new FrameManager(this);
		pfssPluginPanel = new PfssPluginPanel(this);
	}

	@Override
	public void render(GL2 gl) {
		if (isVisible) {
			LocalDateTime localDateTime = Plugins.SINGLETON.getCurrentDateTime();
			PfssDecompressed frame = manager.getFrame(gl, localDateTime);
			if (frame != null)
				frame.display(gl, localDateTime);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * null because this is an internal plugin
	 */
	public String getAboutLicenseText() {
		String description = "";
		description += "<p>"
				+ "The plugin uses the <a href=\"http://heasarc.gsfc.nasa.gov/docs/heasarc/fits/java/v1.0/\">Fits in Java</a> Library, licensed under a <a href=\"https://www.gnu.org/licenses/old-licenses/gpl-1.0-standalone.html\">GPL License</a>.";
		description += "<p>"
				+ "The plugin uses the <a href=\"http://www.bzip.org\">Bzip2</a> Library, licensed under the <a href=\"http://opensource.org/licenses/bsd-license.php\">BSD License</a>.";

		return description;
	}

	public static URL getResourceUrl(String name) {
		return PfssPlugin.class.getResource(name);
	}

	/**
	 * sets the dates which the renderer should display
	 * 
	 * @param start
	 *            first date inclusive
	 * @param end
	 *            last date inclusive
	 * @throws IOException
	 *             if the dates are not present+
	 */
	public void setDisplayRange(LocalDateTime start, LocalDateTime end) {
		manager.setDateRange(start, end);
	}

	public void setVisible(boolean visible) {
		isVisible = visible;

		if (visible)
			manager.showErrorMessages();
	}

	public boolean isVisible() {
		return isVisible;
	}

	@Override
	public void loadStateFile(JSONObject jsonObject) {
		if (jsonObject.has(JSON_NAME)) {
			try {
				JSONObject jsonPfss = jsonObject.getJSONObject(JSON_NAME);
				boolean visible = jsonPfss.getBoolean(JSON_VISIBLE);
				boolean open = jsonPfss.getBoolean(JSON_OPEN);
				Plugins.expandPanel(pfssPluginPanel, open);
				pfssPluginPanel.setVisibleBtn(visible);
			} catch (JSONException e) {
				
				e.printStackTrace();
			}

		}
	}

	@Override
	public void writeStateFile(JSONObject jsonObject) {
		try {
			JSONObject jsonPfss = new JSONObject();
			jsonPfss.put(JSON_OPEN, pfssPluginPanel.isVisible());
			jsonPfss.put(JSON_VISIBLE, isVisible());
			jsonObject.put(JSON_NAME, jsonPfss);
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
	}

	@Override
	public void load() {
		Plugins.addPluginLayer(this, PLUGIN_NAME);
	}

	@Override
	public void remove() {
		Plugins.removePanelOnLeftControllPanel(pfssPluginPanel);
	}

	@Override
	public boolean checkBadRequests(LocalDateTime start, LocalDateTime end) {
		if (start == null || end == null) return false;
		else if (manager.getStartDate() == null || manager.getEndDate() == null) return true;
		return manager.getStartDate().isAfter(start) && manager.getEndDate().isBefore(end);
	}

	@Override
	public int getBadRequestCount() {
		return badRequests.size() > 0 ? badRequests.size() : 1;
	}

	@Override
	public void retryBadReqeuest() {
		badRequests.clear();
		if (manager.getStartDate() == null || manager.getEndDate() == null) pfssPluginPanel.reload();
		else manager.retryBadReqeuest();
		Plugins.repaintLayerPanel();
	}
}