package org.helioviewer.jhv.opengl.camera;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

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
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D){
			this.mousePressed3DFunction(e, camera);
		}
		else {
			this.mousePressed2DFunction(e, camera);
		}
	}

	private void mousePressed3DFunction(MouseEvent e, GL3DCamera camera){
		GL3DVec3d p = this.getHitPoint(e.getPoint());
		if (p != null) {
			this.z = camera.getZTranslation()
					+ this.camera.getRotation().toMatrix().inverse()
							.multiply(p).z;

			Dimension canvasSize = GuiState3DWCS.mainComponentView.getCanavasSize();
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
	
	private void mousePressed2DFunction(MouseEvent e, GL3DCamera camera){		
		if (LayersModel.getSingletonInstance().getActiveView() != null){
		Region region = LayersModel.getSingletonInstance().getActiveView().getAdapter(MetaDataView.class).getMetaData().getPhysicalRegion();
		double halfWidth = region.getWidth() / 2;
		double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
		double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad)
				/ Math.sin(halfFOVRad);
		double scaleFactor = -camera.getZTranslation() / distance;
		double aspect = camera.getAspect();

		double width = region.getWidth() * scaleFactor * aspect;
		double height = region.getHeight() * scaleFactor;

		if (region != null){
		Dimension canvasSize = GuiState3DWCS.mainComponentView.getComponent().getSize();
		this.meterPerPixelWidth = width / canvasSize.getWidth();
		this.meterPerPixelHeight = height / canvasSize.getHeight();
		this.defaultTranslation = camera.getTranslation().copy();
		this.defaultTranslation.x -= this.meterPerPixelWidth * e.getX();
		this.defaultTranslation.y += this.meterPerPixelHeight * e.getY();
		}
		}
	}
	
	public void mouseDragged(MouseEvent e, GL3DCamera camera) {
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
			this.mouseDragged3DFunction(e, camera);
		else this.mouseDragged2DFunction(e, camera);
	}

	private void mouseDragged3DFunction(MouseEvent e, GL3DCamera camera){
		if (defaultTranslation != null) {
			Dimension canvasSize = GuiState3DWCS.mainComponentView.getCanavasSize();

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
	
	private void mouseDragged2DFunction(MouseEvent e, GL3DCamera camera){
		if (defaultTranslation != null){
			camera.translation.x= this.defaultTranslation.x + this.meterPerPixelWidth * e.getX();
			camera.translation.y = this.defaultTranslation.y - this.meterPerPixelHeight * e.getY();
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
