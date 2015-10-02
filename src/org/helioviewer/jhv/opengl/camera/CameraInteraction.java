package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.RayTrace.Ray;

import com.jogamp.opengl.GL2;

public abstract class CameraInteraction
{
	protected MainPanel mainPanel;
	protected Camera camera;
	
	public CameraInteraction(MainPanel _mainPanel, Camera _camera)
	{
		mainPanel = _mainPanel;
		camera = _camera;
	}

	public void mouseWheelMoved(MouseWheelEvent e, Ray _ray)
	{
	}
	
	public void mousePressed(MouseEvent e, Ray _ray)
	{
	}
	
	public void mouseDragged(MouseEvent e, Ray _ray)
	{
	}
	
	public void mouseReleased(MouseEvent e, Ray _ray)
	{
	}
	
	public void renderInteraction(GL2 gl)
	{
	}
}
