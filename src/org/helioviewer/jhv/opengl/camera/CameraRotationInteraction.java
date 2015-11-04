package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.Quaternion;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.RayTrace.Ray;

public class CameraRotationInteraction extends CameraInteraction
{
	private @Nullable Vector3d startPoint;
	private @Nullable Vector3d yAxis;
	
	public static boolean yAxisBlocked = false;

	public CameraRotationInteraction(MainPanel _mainPanel, Camera _camera)
	{
		super(_mainPanel, _camera);
	}

	@SuppressWarnings("null")
	@Override
	public void mouseDragged(MouseEvent _e, Ray _ray)
	{
		if (startPoint == null)
			return;
		
		Vector3d endPoint = _ray.getHitpoint().normalized();
		
		Quaternion rotation;
		if (yAxis==null)
			rotation = Quaternion.calcRotationBetween(endPoint, startPoint);
		else
		{
			//TODO: doesn't move the right amount, but probably good enough atm
			double angle = Quaternion.calcRotationBetween(
					endPoint.projectedToPlane(yAxis).normalized(),
					startPoint.projectedToPlane(yAxis).normalized()
			).getAngle() * -Math.signum(endPoint.projectedToPlane(yAxis).cross(startPoint.projectedToPlane(yAxis)).z);
			
			rotation = Quaternion.createRotation(angle, yAxis);
		}
		Quaternion newRotation = mainPanel.getRotationCurrent().rotate(rotation);
		
		camera.stopAllAnimations();
		camera.setRotationCurrent(newRotation);
		camera.setRotationEnd(newRotation);
		startPoint = endPoint;
	}

	@Override
	public void mousePressed(MouseEvent e, Ray _ray)
	{
		startPoint = _ray.getHitpoint().normalized();
		if(yAxisBlocked)
			yAxis = mainPanel.getRotationCurrent().inversed().toMatrix().multiply(new Vector3d(0,1,0)).normalized();
		else
			yAxis = null;
	}
}
