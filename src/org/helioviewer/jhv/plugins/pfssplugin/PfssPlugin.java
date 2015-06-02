package org.helioviewer.jhv.plugins.pfssplugin;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.helioviewer.jhv.plugins.plugin.Plugin;

public class PfssPlugin extends Plugin
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
		PfssPlugin3dRenderer pfssPlugin3dRenderer = new PfssPlugin3dRenderer();
		pluginPanel = new PfssPluginPanel(pfssPlugin3dRenderer);
		pluginRenderer = pfssPlugin3dRenderer;
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