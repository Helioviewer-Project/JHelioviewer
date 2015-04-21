package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseWheelEvent;


public class CameraZoomInteraction extends CameraInteraction{
    private static final double ZOOM_WHEEL_FACTOR = 1.0 / 20;

	public CameraZoomInteraction(Camera camera) {
		super(camera);
		// TODO Auto-generated constructor stub
	}
	
    public void mouseWheelMoved(MouseWheelEvent e) {
        double zoomDistance = (e.getUnitsToScroll()) * camera.getTranslation().z * ZOOM_WHEEL_FACTOR;
        camera.setZTranslation(camera.getTranslation().z + zoomDistance);
        //camera.addCameraAnimation(new CameraZoomAnimation(zoomDistance));
        camera.fireCameraMoved();
    }
}

