package org.helioviewer.jhv.gui.statusLabels;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.opengl.raytrace.RayTrace.Ray;

public class StatusLabelInterfaces {

	public interface StatusLabelMouse{
		public void mouseExited(MouseEvent e, Ray ray);
		public void mouseMoved(MouseEvent e, Ray ray);
	}
	
	public interface StatusLabelCamera{
		public void cameraChanged();
	}

}
