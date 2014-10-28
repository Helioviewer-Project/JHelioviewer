package org.helioviewer.gl3d.plugin.pfss;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.gl3d.plugin.pfss.olddata.PfssCache;
import org.helioviewer.gl3d.plugin.pfss.olddata.PfssDataOld;
import org.helioviewer.gl3d.plugin.pfss.olddata.PfssFitsFile;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderGraphics;
import org.helioviewer.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.View;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin3dRenderer extends PhysicalRenderer3d {
	private PfssCache pfssCache = null;
	private GL lastGl = null;
	private boolean isVisible = false;
	/**
	 * Default constructor.
	 */
	public PfssPlugin3dRenderer(PfssCache pfssCache) {
		this.pfssCache = pfssCache;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Draws all available and visible solar events with there associated icon.
	 */
	public void render(PhysicalRenderGraphics g) {
		if (!LinkedMovieManager.getActiveInstance().isPlaying() && this.isVisible) {
			GL2 gl = g.getGL().getGL2();
			PfssFitsFile fitsToClear = pfssCache.getFitsToDelete();
			if (fitsToClear != null)
				fitsToClear.clear(gl);
			PfssDataOld pfssData = pfssCache.getData();

			if (pfssData != null) {
				if (lastGl != gl) isVisible = false;
				pfssData.init(gl);
				lastGl = gl;
				if (pfssData.isInit()) {
					pfssData.display(gl);
				}
				
			}
		GL3DState.get().checkGLErrors("PfssPlugin3dRenderer.afterRender");
		}
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
