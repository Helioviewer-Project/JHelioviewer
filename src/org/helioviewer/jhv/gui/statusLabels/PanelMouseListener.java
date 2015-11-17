package org.helioviewer.jhv.gui.statusLabels;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.opengl.RayTrace.Ray;

public interface PanelMouseListener
{
	void mouseExited();
	void mouseMoved(MouseEvent e, Ray ray);
}