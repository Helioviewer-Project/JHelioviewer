package org.helioviewer.jhv.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewportView;

/**
 * This class sets the GL viewport to the size of the underlying viewport, which
 * in effect will be the entire size of the canvas because the size of the
 * underlying viewport is in turn adjusted by the {@link GL3DComponentView}s
 * updateImagePanelSize() method.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DViewportView extends AbstractGL3DView implements GL3DView {

    private ViewportView viewportView;

    public void render3D(GL3DState state) {

        GL2 gl = state.gl;
        if (viewportView != null) {
            //gl.glViewport(0, 0, viewportView.getViewport().getWidth(), viewportView.getViewport().getHeight());
            // Log.debug("GL3DViewportView.viewport (width="+viewportView.getViewport().getWidth()+", height="+viewportView.getViewport().getHeight()+")");
        }
		GL3DState.get().checkGLErrors("GL3DViewportView.afterRender3D");
        if (this.getView() != null) {
            this.renderChild(gl);
        }
    }

    protected void setViewSpecificImplementation(View newView, ChangeEvent changeEvent) {
        viewportView = newView.getAdapter(ViewportView.class);
    }

}
