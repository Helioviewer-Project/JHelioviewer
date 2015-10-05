package org.helioviewer.jhv.gui.statusLabels;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.opengl.RayTrace.Ray;

public interface PanelMouseListener
{
	public void mouseExited(MouseEvent e, Ray ray);
	public void mouseMoved(MouseEvent e, Ray ray);
}