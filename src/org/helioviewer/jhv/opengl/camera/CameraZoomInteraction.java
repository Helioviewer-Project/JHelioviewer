package org.helioviewer.jhv.opengl.camera;

import java.awt.Component;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;


public class CameraZoomInteraction extends CameraInteraction{
    private static final double ZOOM_WHEEL_FACTOR = 1.0 / 20;
    
    public CameraZoomInteraction(MainPanel mainPanel, Camera camera) {
    	super(mainPanel, camera);
	}
	
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double zoomDistance = (e.getUnitsToScroll()) * mainPanel.getTranslation().z * ZOOM_WHEEL_FACTOR;
        camera.setZTranslation(camera.getTranslation().z + zoomDistance);
        //camera.addCameraAnimation(new CameraZoomAnimation(zoomDistance));
    }


}

