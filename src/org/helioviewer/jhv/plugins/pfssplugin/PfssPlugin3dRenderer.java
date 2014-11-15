package org.helioviewer.jhv.plugins.pfssplugin;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssCache;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssData;
import org.helioviewer.jhv.plugins.pfssplugin.data.PfssFitsFile;
import org.helioviewer.jhv.viewmodel.renderer.physical.GLPhysicalRenderGraphics;
import org.helioviewer.jhv.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * @author Stefan Meier (stefan.meier@fhnw.ch)
 * */
public class PfssPlugin3dRenderer extends PhysicalRenderer3d {
	private PfssCache pfssCache = null;
	private GL lastGl = null;
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
	public void render(GLPhysicalRenderGraphics g) {
		if (!LinkedMovieManager.getActiveInstance().isPlaying() && pfssCache.isVisible()) {
			GL2 gl = g.gl.getGL2();
			PfssFitsFile fitsToClear = pfssCache.getFitsToDelete();
			if (fitsToClear != null)
				fitsToClear.clear(gl);
			PfssData pfssData = pfssCache.getData();

			if (pfssData != null) {
				if (lastGl != gl) pfssData.setInit(false);
				pfssData.init(gl);
				lastGl = gl;
				if (pfssData.isInit()) {
					pfssData.display(gl);
				}
				
			}
		GL3DState.get().checkGLErrors("PfssPlugin3dRenderer.afterRender");
		}
	}

	public void viewChanged(View view) {

	}

}
