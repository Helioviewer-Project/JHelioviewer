package org.helioviewer.jhv.viewmodel.view.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.view.AbstractBasicView;
import org.helioviewer.jhv.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.SubimageDataView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;

/**
 * Abstract base class implementing GLView, providing some common OpenGL
 * functions.
 * 
 * <p>
 * This class provides some functions common or useful for all OpenGL views,
 * including the capability to render its successor and handle the case, it the
 * successor is not a GLView.
 * 
 * @author Markus Langenberg
 * 
 */
public abstract class AbstractGLView extends AbstractBasicView implements GLView, ModifiableInnerViewView, ViewListener {

    protected final static GLTextureHelper TEXTURE_HELPER = new GLTextureHelper();

    /**
     * {@inheritDoc}
     */
    public void viewChanged(View sender, ChangeEvent aEvent) {
        notifyViewListeners(aEvent);
    }

    /**
     * Renders the successor (or child) or this view.
     * 
     * This is a service function for all GLViews, so that they do not have to
     * take care whether their child is a GLView itself or not. The child is
     * always rendered in the correct way, so other GLViews may call this
     * function during their {@link #renderGL(GL)} function.
     * 
     * @param gl
     *            Valid reference to the current gl object
     */
    protected void renderChild(GL2 gl) {
        if (view instanceof GLView) {
        	((GLView) view).renderGL(gl, true);
        	this.checkGLErrors(gl,view+".afterRenderGL");            
        } else {
            TEXTURE_HELPER.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getLastDecodedRegion(), view.getAdapter(SubimageDataView.class).getImageData());
        }
    }

	public boolean checkGLErrors(GL2 gl, String message) {
		if (gl == null) {
			Log.warn("OpenGL not yet Initialised!");
			return true;
		}
		int glErrorCode = gl.glGetError();

		if (glErrorCode != GL.GL_NO_ERROR) {
			GLU glu = new GLU();
			Log.error("GL Error (" + glErrorCode + "): "
					+ glu.gluErrorString(glErrorCode) + " - @" + message);
			if (glErrorCode == GL.GL_INVALID_OPERATION) {
				// Find the error position
				int[] err = new int[1];
				gl.glGetIntegerv(GL2.GL_PROGRAM_ERROR_POSITION_ARB, err, 0);
				if (err[0] >= 0) {
					String error = gl
							.glGetString(GL2.GL_PROGRAM_ERROR_STRING_ARB);
					Log.error("GL error at " + err[0] + ":\n" + error);
				}
			}
			return true;
		} else {
			return false;
		}
	}
}
