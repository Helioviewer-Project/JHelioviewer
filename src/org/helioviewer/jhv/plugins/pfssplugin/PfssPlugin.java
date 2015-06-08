package org.helioviewer.jhv.plugins.pfssplugin;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressed;
import org.helioviewer.jhv.plugins.pfssplugin.data.managers.FrameManager;
import org.helioviewer.jhv.plugins.plugin.NewPlugin;
import org.helioviewer.jhv.plugins.plugin.UltimatePluginInterface;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class PfssPlugin extends NewPlugin
{
	private static final String NAME = "PFSS plugin";
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

    
	private FrameManager manager;
	private boolean isVisible = false;

	public PfssPlugin()
	{
		manager = new FrameManager(this);
		UltimatePluginInterface.addPanelToLeftControllPanel(NAME, new PfssPluginPanel(this), false);
	}
	
	@Override
	public void render(GL2 gl) {
		if (isVisible){
			LocalDateTime localDateTime = UltimatePluginInterface.SIGLETON.getCurrentDateTime();
			PfssDecompressed frame = manager.getFrame(gl,localDateTime);
			if(frame != null)
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
	
	public static URL getResourceUrl(String name)
	{
		return PfssPlugin.class.getResource(name);
	}
	
	/**
	 * sets the dates which the renderer should display
	 * @param start first date inclusive
	 * @param end last date inclusive
	 * @throws IOException if the dates are not present+
	 */
	public void setDisplayRange(LocalDateTime start, LocalDateTime end)
	{
		manager.setDateRange(start, end);
	}

	public void setVisible(boolean visible)
	{
		isVisible = visible;
		
		if(visible)
		    manager.showErrorMessages();
	}
	
	public boolean isVisible()
	{
		return isVisible;
	}

}