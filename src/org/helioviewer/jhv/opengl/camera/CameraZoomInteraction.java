package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.RayTrace.Ray;
import org.helioviewer.jhv.opengl.camera.animation.CameraZoomAnimation;


public class CameraZoomInteraction extends CameraInteraction
{
    private static final double ZOOM_WHEEL_FACTOR = 1.0 / 20;
    
    public CameraZoomInteraction(MainPanel mainPanel, Camera camera)
    {
    	super(mainPanel, camera);
	}
	
    @Override
    public void mouseWheelMoved(MouseWheelEvent e, Ray _ray)
    {
        double zoomDistance = (e.getUnitsToScroll()) * mainPanel.getTranslationEnd().z * ZOOM_WHEEL_FACTOR;
        camera.addCameraAnimation(new CameraZoomAnimation(mainPanel, zoomDistance));
    }
}
