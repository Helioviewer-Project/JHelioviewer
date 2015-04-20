package org.helioviewer.jhv.layers;

import java.awt.Rectangle;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.camera.Camera;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.viewmodel.region.Region;

public class LayerRayTrace {
	private RayTrace rayTrace;
	
	private Rectangle rectangle = null;
	
	private final int MAX_X_POINTS = 11;
	private final int MAX_Y_POINTS = 11;
	
	public LayerRayTrace(Camera camera) {
		rayTrace = new RayTrace(camera);
	}
	
	private void getCurrentRegion(){
		for (int i = 1; i <= MAX_X_POINTS; i++){
			for (int j = 1; j <= MAX_Y_POINTS; i++){
				rayTrace.cast(GuiState3DWCS.mainComponentView.getComponent().getSurfaceWidth() / i, GuiState3DWCS.mainComponentView.getComponent().getSurfaceWidth() / j);
			}
		}
	}
	
}
