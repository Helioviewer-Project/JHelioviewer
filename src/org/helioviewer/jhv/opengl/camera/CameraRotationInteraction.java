package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class CameraRotationInteraction extends CameraInteraction {

	private Vector3d currentRotationStartPoint;
	private Vector3d currentRotationEndPoint;
	private volatile Quaternion3d currentDragRotation;

	public static boolean yAxisBlocked = false;

	public CameraRotationInteraction(MainPanel compenentView, Camera camera) {
		super(compenentView, camera);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		this.currentRotationEndPoint = rayTrace.cast(e.getX(), e.getY(), mainPanel).getHitpoint();
		if (yAxisBlocked) {
			this.currentRotationEndPoint = new Vector3d(currentRotationEndPoint.x, currentRotationStartPoint.y, currentRotationEndPoint.z);
		} 
		currentDragRotation = Quaternion3d.calcRotation(
					currentRotationEndPoint, currentRotationStartPoint);
		currentDragRotation.rotate(mainPanel.getRotation());
		camera.setRotation(currentDragRotation);
		currentRotationStartPoint = currentRotationEndPoint;		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		this.currentRotationStartPoint = rayTrace.cast(e.getX(), e.getY(), mainPanel).getHitpoint();
	}
}
