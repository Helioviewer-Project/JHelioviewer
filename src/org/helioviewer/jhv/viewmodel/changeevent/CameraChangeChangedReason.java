package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * ChangedReason when the active {@link GL3DCamera} has changed.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class CameraChangeChangedReason implements ChangedReason {

    private View sender;

    private GL3DCamera camera;

    public CameraChangeChangedReason(View sender, GL3DCamera newCamera) {
        this.sender = sender;
        this.camera = newCamera;
    }

    public View getView() {
        return sender;
    }

    public GL3DCamera getCamera() {
        return this.camera;
    }

}
