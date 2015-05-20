package org.helioviewer.jhv.plugins.pfssplugin;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.helioviewer.jhv.plugins.viewmodelplugin.interfaces.Plugin;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayPlugin;

public class PfssPlugin extends OverlayPlugin implements Plugin
{
    private static int threadNumber=0;
    public static final ExecutorService pool = Executors.newFixedThreadPool(8,new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable _r)
        {
            Thread t=Executors.defaultThreadFactory().newThread(_r);
            t.setName("PFSS-"+(threadNumber++));
            t.setDaemon(true);
            return t;
        }
    });
    
	public PfssPlugin()
	{
		try
		{
			this.pluginLocation = new URI(PfssSettings.PLUGIN_LOCATION);
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}

		addOverlayContainer(new PfssPluginContainer());
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overrides the default method because the internal provided filters are
	 * activated by default.
	 */
	public void installPlugin() {
		for (OverlayContainer overlay : overlayContainerList) {
			/*overlay.setActive(PluginSettings.getSingeltonInstance()
					.isOverlayInPluginActivated(pluginLocation,
							overlay.getOverlayClass(), true));
			overlay.setPosition(PluginSettings.getSingeltonInstance()
					.getOverlayPosition(pluginLocation,
							overlay.getOverlayClass()));
			PluginManager.getSingeltonInstance().addOverlayContainer(overlay);
			*/
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A description is not needed here because this plug-in is activated always
	 * and will not be visible in the corresponding dialogs.
	 */
	public String getDescription() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getName()
	{
		return "PFSS plugin";
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

	public static URL getResourceUrl(String name)
	{
		return PfssPlugin.class.getResource(name);
	}

	/**
	 * {@inheritDoc} In this case, does nothing.
	 */
	public void setState(String state)
	{
	}

	/**
	 * {@inheritDoc} In this case, does nothing.
	 */
	public String getState()
	{
		return "";
	}
}