package org.helioviewer.jhv.viewmodel.view.opengl;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.jhv.viewmodel.view.SubimageDataView;

/**
 * Extension of StandardSolarRotationTrackingView to enable tracking in OpenGL
 * mode.
 * 
 * @author Markus Langenberg
 */
public class GLSolarRotationTrackingView extends StandardSolarRotationTrackingView implements GLView {

    protected final static GLTextureHelper textureHelper = new GLTextureHelper();

    /**
     * {@inheritDoc}
     */
    public void renderGL(GL2 gl, boolean nextView) {
        if (view instanceof GLView) {
            ((GLView) view).renderGL(gl, true);
        } else {
            textureHelper.renderImageDataToScreen(gl, view.getAdapter(RegionView.class).getRegion(), view.getAdapter(SubimageDataView.class).getSubimageData());
        }
    }
}
