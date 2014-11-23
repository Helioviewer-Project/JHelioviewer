package org.helioviewer.jhv.plugins.pfssplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.helioviewer.jhv.plugins.viewmodelplugin.controller.PluginManager;
import org.helioviewer.jhv.plugins.viewmodelplugin.controller.PluginSettings;
import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayPlugin;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin extends OverlayPlugin implements Plugin {

  /**
	 * Reference to the eventPlugin
	 */
	private PfssPluginContainer eventPlugin;

	/**
	 * Default constructor.
	 */
	public PfssPlugin() {
		try {
			this.pluginLocation = new URI(PfssSettings.PLUGIN_LOCATION);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		eventPlugin = new PfssPluginContainer();
		addOverlayContainer(eventPlugin);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overrides the default method because the internal provided filters are
	 * activated by default.
	 */
	public void installPlugin() {
		for (OverlayContainer overlay : overlayContainerList) {
			overlay.setActive(PluginSettings.getSingeltonInstance()
					.isOverlayInPluginActivated(pluginLocation,
							overlay.getOverlayClass(), true));
			overlay.setPosition(PluginSettings.getSingeltonInstance()
					.getOverlayPosition(pluginLocation,
							overlay.getOverlayClass()));
			PluginManager.getSingeltonInstance().addOverlayContainer(overlay);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "Pfss Overlay Plugin";
	}


	/**
	 * {@inheritDoc}
	 * 
	 * null because this is an internal plugin
	 */
	public String getAboutLicenseText() {
		String description = "";
		description += "<p>"
				+ "This software uses the <a href=\"http://heasarc.gsfc.nasa.gov/docs/heasarc/fits/java/v1.0/\">Fits in Java</a> Library, licensed under a <a href=\"https://www.gnu.org/licenses/old-licenses/gpl-1.0-standalone.html\">GPL License</a>.";
		description += "<p>"
				+ "This software uses the <a href=\"http://www.bzip.org\">Bzip2</a> Library, licensed under the <a href=\"http://opensource.org/licenses/bsd-license.php\">BSD License</a>.";

		return description;
	}

	public static URL getResourceUrl(String name) {
		return PfssPlugin.class.getResource(name);
	}

	/**
	 * {@inheritDoc} In this case, does nothing.
	 */
	public void setState(String state) {
		// TODO Implement setState for PfssPlugin
	}

	/**
	 * {@inheritDoc} In this case, does nothing.
	 */
	public String getState() {
		// TODO Implement getState for PfssPlugin
		return "";
	}
}