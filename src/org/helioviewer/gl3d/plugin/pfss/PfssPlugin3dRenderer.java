package org.helioviewer.gl3d.plugin.pfss;

import java.io.IOException;
import java.util.Date;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.gl3d.plugin.pfss.data.PfssFrame;
import org.helioviewer.gl3d.plugin.pfss.data.managers.FrameManager;
import org.helioviewer.gl3d.plugin.pfss.olddata.PfssCache;
import org.helioviewer.gl3d.plugin.pfss.olddata.PfssDataOld;
import org.helioviewer.gl3d.plugin.pfss.olddata.PfssFitsFile;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin3dRenderer extends PhysicalRenderer3d {
	private FrameManager manager;
	private GL lastGl = null;
	private boolean isVisible = false;
	/**
	 * Default constructor.
	 */
	public PfssPlugin3dRenderer() {
		this.manager = new FrameManager();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Draws all available and visible solar events with there associated icon.
	 */
	public void render(PhysicalRenderGraphics g) {
		
		TimedMovieView masterView = LinkedMovieManager.getActiveInstance().getMasterMovie();;
		if (this.isVisible) {
			GL2 gl = g.getGL().getGL2();
			
			manager.preInitFrames(gl);
			Date date = masterView.getCurrentFrameDateTime().getTime();
			PfssFrame frame = manager.getFrame(date);
			if(frame != null)
				frame.display(gl);
			
			GL3DState.get().checkGLErrors("PfssPlugin3dRenderer.afterRender");
		}
	}
	
	public void render(Date d) {
		manager.preInitFrames(null);
		Date date = d;
		PfssFrame frame = manager.getFrame(date);
		if(frame != null)
			frame.display(null);
	}
	
	public void setDisplayRange(Date start, Date end) throws IOException {
		manager.setDateRange(start, end);
	}

	public void setVisible(boolean visible) {
		isVisible = visible;
	}
	
	public boolean isVisible() {
		return isVisible;
	}

	public void viewChanged(View view) {

	}

}
