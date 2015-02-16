package org.helioviewer.jhv.gui.controller;

import java.awt.Dimension;

import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewportView;
import org.helioviewer.jhv.viewmodel.viewport.Viewport;

/**
 * Collection of several zooming functions.
 * 
 * <p>
 * This class provides several zooming functions. The controller is used by
 * several classes, such as {@link org.helioviewer.jhv.gui.actions.ZoomInAction}, {@link org.helioviewer.jhv.gui.actions.ZoomOutAction},
 * {@link org.helioviewer.jhv.gui.actions.ZoomFitAction},
 * {@link org.helioviewer.jhv.gui.actions.Zoom1to1Action} and
 * {@link org.helioviewer.jhv.gui.controller.MainImagePanelMouseController}.
 */
public class ZoomController {

	public static double getZoom(View view, Region outerRegion,
			Viewport viewport) {

		View activeView = LayersModel.getSingletonInstance().getActiveView();
		GL3DCamera camera = GL3DCameraSelectorModel.getInstance()
				.getCurrentCamera();
		if (activeView != null) {
			MetaData metaData = activeView.getAdapter(MetaDataView.class)
					.getMetaData();
			double unitsPerPixel = metaData.getUnitsPerPixel();
			Region region = metaData.getPhysicalRegion();

			if (region != null && camera != null) {
				Dimension dimension = GuiState3DWCS.mainComponentView.getCanavasSize();
				double minCanvasDimension = dimension.getHeight();
	            double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
	            double distance = (minCanvasDimension/2.0 * unitsPerPixel) / Math.tan(halfFOVRad);

	            return -distance / camera.getZTranslation();
			}
		}
		return 1.0;
	}

	public static double getZoom(View view, Viewport viewport) {
		return getZoom(view, null, viewport);
	}

	public static double getZoom(View view) {
		ViewportView viewportView = view.getAdapter(ViewportView.class);
		if (viewportView != null) {
			Viewport viewport = viewportView.getViewport();
			return getZoom(view, viewport);
		}
		return 1.0;
	}

}
