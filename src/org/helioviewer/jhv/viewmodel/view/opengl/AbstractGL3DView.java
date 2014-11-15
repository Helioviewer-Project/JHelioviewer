package org.helioviewer.jhv.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.opengl.scenegraph.GL3DState;

/**
 * Default super class for all {@link GL3DView}s. Provides default behavior like
 * view chain traversal.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class AbstractGL3DView extends AbstractGLView implements GL3DView {

    public void renderGL(GL2 gl, boolean nextView) {
        render3D(GL3DState.get());
		GL3DState.get().checkGLErrors(this+".afterRender3D");
    }
}
