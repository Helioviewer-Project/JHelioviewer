package org.helioviewer.jhv.opengl.camera;

import java.awt.Component;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;


public class CameraZoomInteraction extends CameraInteraction{
    private static final double ZOOM_WHEEL_FACTOR = 1.0 / 20;
    private Component component;

    public CameraZoomInteraction(CompenentView compenentView) {
    	super(compenentView);
    	enable = true;
	}
	
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double zoomDistance = (e.getUnitsToScroll()) * compenentView.getTranslation().z * ZOOM_WHEEL_FACTOR;
        compenentView.setZTranslation(compenentView.getTranslation().z + zoomDistance);
        //camera.addCameraAnimation(new CameraZoomAnimation(zoomDistance));
    }


}

