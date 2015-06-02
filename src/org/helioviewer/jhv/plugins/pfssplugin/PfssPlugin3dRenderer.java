package org.helioviewer.jhv.plugins.pfssplugin;

import java.io.IOException;
import java.time.LocalDateTime;

import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressed;
import org.helioviewer.jhv.plugins.pfssplugin.data.managers.FrameManager;
import org.helioviewer.jhv.plugins.plugin.PluginRenderer;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

public class PfssPlugin3dRenderer implements PluginRenderer{
	private FrameManager manager;
	private boolean isVisible = false;
	/**
	 * Default constructor.
	 */
	public PfssPlugin3dRenderer()
	{
		manager = new FrameManager(this);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Draws all available and visible solar events with there associated icon.
	 */
	public void render(GL2 gl)
	{
		//gl.glDisable(GL.GL_DEPTH_TEST);
			LocalDateTime localDateTime = TimeLine.SINGLETON.getCurrentDateTime();
			PfssDecompressed frame = manager.getFrame(gl,localDateTime);
			if(frame != null)
				frame.display(gl, localDateTime);			
			gl.glDepthMask(false);
			gl.glEnable(GL.GL_DEPTH_TEST);
		
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

	@Override
	public LocalDateTime getDateTime() {
		return null;
	}

}
