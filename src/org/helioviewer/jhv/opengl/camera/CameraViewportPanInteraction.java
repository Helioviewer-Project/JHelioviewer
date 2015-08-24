package org.helioviewer.jhv.opengl.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.opengl.camera.CameraMode.MODE;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;

public class CameraViewportPanInteraction extends CameraInteraction {
	private double meterPerPixelWidth;
	private double meterPerPixelHeight;
	private Point lastPosition;
	private boolean dragged;
	
	public CameraViewportPanInteraction(MainPanel mainPanel, Camera camera) {
		super(mainPanel, camera);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		double z;
		dragged = false;
		if (CameraMode.mode == MODE.MODE_3D){
			RayTrace rayTrace = new RayTrace();
			z = mainPanel.getTranslation().z - rayTrace.cast(e.getX(), e.getY(), mainPanel).getHitpoint().z;
		}
		else z = mainPanel.getTranslation().z;
		
		double width = Math.tan(Math.toRadians(MainPanel.FOV/ 2.0)) * z * 2;
		double height = width / mainPanel.getAspect();
		meterPerPixelWidth = width/(double)mainPanel.getWidth();
		meterPerPixelHeight = height/(double)mainPanel.getHeight();
		lastPosition = e.getPoint();
		
	}


	public void mouseDragged(MouseEvent e) {
		if (!e.getPoint().equals(lastPosition)){
			dragged = true;
		double xTranslation = (lastPosition.getX() - e.getX()) * meterPerPixelWidth;
		double yTranslation = (lastPosition.getY() - e.getY()) * meterPerPixelHeight;
		Vector3d translation = camera.getTranslation();
		this.lastPosition = e.getPoint();
		camera.setTranslation(new Vector3d(translation.x - xTranslation, translation.y - yTranslation, translation.z));
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// If never dragged, pan to the point where should be clicked
		if (!dragged){
			double xTranslation = (lastPosition.getX() - mainPanel.getWidth() / 2) * meterPerPixelWidth;
			double yTranslation = (lastPosition.getY() - mainPanel.getHeight() / 2) * meterPerPixelHeight;
			Vector3d translation = mainPanel.getTranslation();
			camera.setTranslation(new Vector3d(translation.x - xTranslation, translation.y - yTranslation, camera.getTranslation().z));
		}
	}
}