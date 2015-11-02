package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.RayTrace.Ray;

public class CameraRotationInteraction extends CameraInteraction
{
	private @Nullable Vector3d currentRotationStartPoint;
	private @Nullable Vector3d currentRotationEndPoint;
	private @Nullable Quaternion3d currentDragRotation;

	public static boolean yAxisBlocked = false;

	public CameraRotationInteraction(MainPanel _mainPanel, Camera _camera)
	{
		super(_mainPanel, _camera);
	}

	@SuppressWarnings("null")
	@Override
	public void mouseDragged(MouseEvent e, Ray _ray)
	{
		if (currentRotationStartPoint == null)
			return;
		
		currentRotationEndPoint = _ray.getHitpoint();
		
		//FIXME: does not work at the moment
		if (yAxisBlocked)
			currentRotationEndPoint = new Vector3d(currentRotationEndPoint.x, currentRotationStartPoint.y, currentRotationEndPoint.z);
		
		// TODO: are the parameters in the correct order?
		// Quaternion3d.calcRotation expects (startPoint,endPoint)
		currentDragRotation = Quaternion3d.calcRotation(currentRotationEndPoint.normalize(), currentRotationStartPoint.normalize());
		Quaternion3d currentCam = mainPanel.getRotationCurrent().rotate(currentDragRotation);
		
		// currentDragRotation.rotate(mainPanel.getRotation());
		camera.stopAllAnimations();
		camera.setRotationCurrent(currentCam);
		camera.setRotationEnd(currentCam);
		currentRotationStartPoint = currentRotationEndPoint;
	}

	@Override
	public void mousePressed(MouseEvent e, Ray _ray)
	{
		currentRotationStartPoint = _ray.getHitpoint();
	}
}
