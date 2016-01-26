package org.helioviewer.jhv.opengl.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.RayTrace.Ray;
import org.helioviewer.jhv.opengl.camera.CameraMode.MODE;

public class CameraPanInteraction extends CameraInteraction
{
	private double scale;
	private double meterPerPixelWidth;
	private double meterPerPixelHeight;
	private @Nullable Point lastPosition;
	private boolean dragged;
	
	public CameraPanInteraction(MainPanel mainPanel, Camera camera, double _scale) 
	{
		super(mainPanel, camera);
		scale=_scale;
	}

	@Override
	public void mousePressed(MouseEvent e, Ray _ray)
	{
		double z;
		dragged = false;
		if (CameraMode.mode == MODE.MODE_3D)
			z = mainPanel.getTranslationCurrent().z - _ray.getHitpoint().z;
		else
			z = mainPanel.getTranslationCurrent().z;
		
		double width = Math.tan(Math.toRadians(MainPanel.FOV/ 2.0)) * z * 2;
		double height = width / mainPanel.getAspect();
		meterPerPixelWidth = width/(double)mainPanel.getWidth();
		meterPerPixelHeight = height/(double)mainPanel.getHeight();
		lastPosition = e.getPoint();
	}


	@SuppressWarnings("null")
	public void mouseDragged(MouseEvent e, Ray _ray)
	{
		if (lastPosition!=null && !e.getPoint().equals(lastPosition))
		{
			dragged = true;
			double xTranslation = (lastPosition.getX() - e.getX()) * meterPerPixelWidth;
			double yTranslation = (lastPosition.getY() - e.getY()) * meterPerPixelHeight;
			Vector3d translation = camera.getTranslationCurrent();
			lastPosition = e.getPoint();
			camera.abortAllAnimations();
			camera.setTranslationCurrent(new Vector3d(translation.x + xTranslation*scale, translation.y + yTranslation*scale, translation.z));
			camera.setTranslationEnd(new Vector3d(translation.x + xTranslation*scale, translation.y + yTranslation*scale, translation.z));
		}
	}

	@SuppressWarnings("null")
	@Override
	public void mouseReleased(MouseEvent e, Ray _ray)
	{
		// If never dragged, pan to the point where should be clicked
		if (!dragged && lastPosition!=null && scale<0)
		{
			double xTranslation = (lastPosition.getX() - mainPanel.getWidth() / 2) * meterPerPixelWidth;
			double yTranslation = (lastPosition.getY() - mainPanel.getHeight() / 2) * meterPerPixelHeight;
			Vector3d translation = mainPanel.getTranslationCurrent();
			camera.abortAllAnimations();
			camera.setTranslationCurrent(new Vector3d(translation.x - xTranslation*scale, translation.y - yTranslation*scale, camera.getTranslationCurrent().z));
			camera.setTranslationEnd(new Vector3d(translation.x - xTranslation*scale, translation.y - yTranslation*scale, camera.getTranslationCurrent().z));
		}
	}
}