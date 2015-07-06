package org.helioviewer.jhv.plugins.pfssplugin;

import java.io.IOException;
import java.util.Date;

import org.helioviewer.jhv.plugins.pfssplugin.data.PfssDecompressed;
import org.helioviewer.jhv.plugins.pfssplugin.data.managers.FrameManager;
import org.helioviewer.jhv.viewmodel.renderer.physical.GLPhysicalRenderGraphics;
import org.helioviewer.jhv.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;

public class PfssPlugin3dRenderer extends PhysicalRenderer3d {
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
	public void render(GLPhysicalRenderGraphics g)
	{
		JHVJPXView masterView = LinkedMovieManager.getActiveInstance().getMasterMovie();;
		if (isVisible && masterView != null && masterView.getCurrentFrameDateTime() != null)
		{
			Date date = masterView.getCurrentFrameDateTime().getTime();
			PfssDecompressed frame = manager.getFrame(g.gl,date);
			if(frame != null)
				frame.display(g.gl, date);			
		}
	}
	
	/**
	 * sets the dates which the renderer should display
	 * @param start first date inclusive
	 * @param end last date inclusive
	 * @throws IOException if the dates are not present+
	 */
	public void setDisplayRange(Date start, Date end)
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

	public void viewChanged(View view)
	{
	}
}
