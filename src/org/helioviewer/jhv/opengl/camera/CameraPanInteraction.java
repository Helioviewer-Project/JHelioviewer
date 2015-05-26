package org.helioviewer.jhv.opengl.camera;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.gui.controller.Camera;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.opengl.raytrace.RayTrace.Ray;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class CameraPanInteraction extends CameraInteraction {
	private double meterPerPixelWidth;
	private double meterPerPixelHeight;
	private double z;
	private Vector3d defaultTranslation;

	public CameraPanInteraction(MainPanel compenentView, Camera camera) {
		super(compenentView, camera);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D){
		this.mousePressed3DFunction(e);
		/*
		 * } else { this.mousePressed2DFunction(e); }
		 */
	}

	private void mousePressed3DFunction(MouseEvent e) {
		RayTrace rayTrace = new RayTrace();
		Ray ray = rayTrace.cast(e.getX(), e.getY(), componentView);
		Vector3d p = ray.getHitpoint();
		if (p != null) {
			this.z = componentView.getTranslation().z
					+ componentView.getRotation().toMatrix().inverse()
							.multiply(p).z;

			Dimension canvasSize = MainFrame.MAIN_PANEL
					.getCanavasSize();
			double halfClipNearHeight = Math.tanh(Math
					.toRadians(MainPanel.FOV / 2))
					* MainPanel.CLIP_NEAR;
			double halfClipNearWidth = halfClipNearHeight
					/ canvasSize.getHeight() * canvasSize.getWidth();

			meterPerPixelHeight = halfClipNearHeight * 2
					/ (canvasSize.getHeight());
			meterPerPixelWidth = halfClipNearWidth * 2
					/ (canvasSize.getWidth());

			double x = e.getPoint().getX() * canvasSize.getWidth()
					/ MainFrame.MAIN_PANEL.getWidth();
			double y = e.getPoint().getY()
					* canvasSize.getHeight()
					/ MainFrame.MAIN_PANEL
							.getHeight();
			double yMeterInNearPlane = (y - canvasSize.getHeight() / 2.)
					* meterPerPixelHeight;
			double xMeterInNearPlane = (x - canvasSize.getWidth() / 2.)
					* meterPerPixelWidth;
			double yAngle = Math.atan2(yMeterInNearPlane,
					MainPanel.CLIP_NEAR);
			double xAngle = Math.atan2(xMeterInNearPlane,
					MainPanel.CLIP_NEAR);
			double yPosition = Math.tanh(yAngle) * z;
			double xPosition = Math.tanh(xAngle) * z;

			this.defaultTranslation = componentView.getTranslation();
			this.defaultTranslation = new Vector3d(this.defaultTranslation.x
					- xPosition, this.defaultTranslation.y - yPosition,
					this.defaultTranslation.z);
		}
	}

	private void mousePressed2DFunction(MouseEvent e) {

		PhysicalRegion region = Layers.LAYERS.getActiveLayer()
				.getMetaData().getPhysicalRegion();
		double halfWidth = region.getWidth() / 2;
		double halfFOVRad = Math.toRadians(MainPanel.CLIP_NEAR / 2.0);
		double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad)
				/ Math.sin(halfFOVRad);
		double scaleFactor = -componentView.getTranslation().z / distance;
		double aspect = componentView.getAspect();

		double width = region.getWidth() * scaleFactor * aspect;
		double height = region.getHeight() * scaleFactor;

		if (region != null) {
			Dimension canvasSize = MainFrame.MAIN_PANEL
					.getSize();
			this.meterPerPixelWidth = width / canvasSize.getWidth();
			this.meterPerPixelHeight = height / canvasSize.getHeight();
			this.defaultTranslation = componentView.getTranslation();
			this.defaultTranslation = new Vector3d(this.defaultTranslation.x
					- this.meterPerPixelWidth * e.getX(),
					this.defaultTranslation.y - this.meterPerPixelHeight
							* e.getY(), this.defaultTranslation.z);
		}
	}

	public void mouseDragged(MouseEvent e) {
		// if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
		this.mouseDragged3DFunction(e);
		/* else this.mouseDragged2DFunction(e); */
	}

	private void mouseDragged3DFunction(MouseEvent e) {
		if (defaultTranslation != null) {
			Dimension canvasSize = componentView.getCanavasSize();
			double x = e.getPoint().getX() * canvasSize.getWidth()
					/ MainFrame.MAIN_PANEL.getWidth();
			double y = e.getPoint().getY()
					* canvasSize.getHeight()
					/ MainFrame.MAIN_PANEL
							.getHeight();

			double yMeterInNearPlane = (y - canvasSize.getHeight() / 2.)
					* meterPerPixelHeight;
			double xMeterInNearPlane = (x - canvasSize.getWidth() / 2.)
					* meterPerPixelWidth;
			double yAngle = Math.atan2(yMeterInNearPlane,
					MainPanel.CLIP_NEAR);
			double xAngle = Math.atan2(xMeterInNearPlane,
					MainPanel.CLIP_NEAR);
			double yPosition = Math.tanh(yAngle) * z;
			double xPosition = Math.tanh(xAngle) * z;

			camera.setTranslation(new Vector3d(
					defaultTranslation.x - xPosition, defaultTranslation.y
							- yPosition, camera.getTranslation().z));

		}
	}

	private void mouseDragged2DFunction(MouseEvent e) {
		if (defaultTranslation != null) {
			camera.setTranslation(new Vector3d(this.defaultTranslation.x
					+ this.meterPerPixelWidth * e.getX(),
					this.defaultTranslation.y - this.meterPerPixelHeight
							* e.getY(), camera.getTranslation().z));
		}
	}
}