package org.helioviewer.jhv.opengl.camera;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.helioviewer.jhv.opengl.camera.newCamera.CameraZoomAnimation;


public class CameraZoomInteraction extends CameraInteraction{
    private static final double ZOOM_WHEEL_FACTOR = 1.0 / 20;

	public CameraZoomInteraction(Camera camera) {
		super(camera);
		// TODO Auto-generated constructor stub
	}
	
    public void mouseWheelMoved(MouseWheelEvent e) {
        double zoomDistance = (e.getUnitsToScroll()) * camera.getTranslation().z * ZOOM_WHEEL_FACTOR;
        camera.setZTranslation(camera.getTranslation().z + zoomDistance);
        System.out.println(camera.getTranslation().z);
        //camera.addCameraAnimation(new CameraZoomAnimation(zoomDistance));
        camera.fireCameraMoved();
    }
}

