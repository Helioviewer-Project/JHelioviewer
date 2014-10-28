package org.helioviewer.gl3d.camera;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.jhv.gui.states.StateController;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.ViewHelper;
import org.helioviewer.viewmodel.view.ViewportView;
import org.helioviewer.viewmodel.viewport.Viewport;

/**
 * Standard panning interaction, moves the camera proportionally to the mouse
 * movement when dragging
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DPanInteraction extends GL3DDefaultInteraction {
	private double meterPerPixelWidth;
	private double meterPerPixelHeight;
	private double z;
	private GL3DVec3d defaultTranslation;
	private GL3DRayTracer rayTracer;

	protected GL3DPanInteraction(GL3DTrackballCamera camera,
			GL3DSceneGraphView sceneGraph) {
		super(camera, sceneGraph);
	}

	public void mousePressed(MouseEvent e, GL3DCamera camera) {
		GL3DVec3d p = this.getHitPoint(e.getPoint());
		if (p != null) {
			this.z = camera.getZTranslation() + p.z;

			Dimension canvasSize = StateController.getInstance()
					.getCurrentState().getMainComponentView().getCanavasSize();
			double halfClipNearHeight = Math.tanh(Math.toRadians(camera
					.getFOV() / 2)) * camera.getClipNear();
			double halfClipNearWidth = halfClipNearHeight
					/ canvasSize.getHeight() * canvasSize.getWidth();

			meterPerPixelHeight = halfClipNearHeight * 2
					/ (canvasSize.getHeight());
			meterPerPixelWidth = halfClipNearWidth * 2
					/ (canvasSize.getWidth());

			double yMeterInNearPlane = (e.getY() - canvasSize.getHeight() / 2)
					* meterPerPixelHeight;
			double xMeterInNearPlane = (e.getX() - canvasSize.getWidth() / 2)
					* meterPerPixelWidth;
			double yAngle = Math.atan2(yMeterInNearPlane, camera.getClipNear());
			double xAngle = Math.atan2(xMeterInNearPlane, camera.getClipNear());
			double yPosition = Math.tanh(yAngle) * z;
			double xPosition = Math.tanh(xAngle) * z;

			this.defaultTranslation = camera.getTranslation().copy();
			this.defaultTranslation.x += xPosition;
			this.defaultTranslation.y -= yPosition;
		}
	}

	public void mouseDragged(MouseEvent e, GL3DCamera camera) {
		if (defaultTranslation != null) {
			Dimension canvasSize = StateController.getInstance()
					.getCurrentState().getMainComponentView().getCanavasSize();

			double yMeterInNearPlane = (e.getY() - canvasSize.getHeight() / 2)
					* meterPerPixelHeight;
			double xMeterInNearPlane = (e.getX() - canvasSize.getWidth() / 2)
					* meterPerPixelWidth;
			double yAngle = Math.atan2(yMeterInNearPlane, camera.getClipNear());
			double xAngle = Math.atan2(xMeterInNearPlane, camera.getClipNear());
			double yPosition = Math.tanh(yAngle) * z;
			double xPosition = Math.tanh(xAngle) * z;

			camera.translation.x = defaultTranslation.x - xPosition;
			camera.translation.y = defaultTranslation.y + yPosition;

			camera.updateCameraTransformation();

			camera.fireCameraMoving();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e, GL3DCamera camera) {
		camera.fireCameraMoved();
	}

	protected GL3DVec3d getHitPoint(Point p) {
		this.rayTracer = new GL3DRayTracer(
				sceneGraphView.getHitReferenceShape(), this.camera);
		GL3DRay ray = this.rayTracer.cast(p.x, p.y);
		GL3DVec3d hitPoint = ray.getHitPoint();
		return hitPoint;
	}

}
