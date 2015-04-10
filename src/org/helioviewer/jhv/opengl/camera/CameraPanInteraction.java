package org.helioviewer.jhv.opengl.camera;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.opengl.raytrace.RayTrace.Ray;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;

public class CameraPanInteraction extends CameraInteraction {
	private double meterPerPixelWidth;
	private double meterPerPixelHeight;
	private double z;
	private Vector3d defaultTranslation;

	public CameraPanInteraction(Camera camera) {
		super(camera);
		// TODO Auto-generated constructor stub
	}

	public void mousePressed(MouseEvent e) {
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D){
			this.mousePressed3DFunction(e);
		}
		else {
			this.mousePressed2DFunction(e);
		}
	}

	private void mousePressed3DFunction(MouseEvent e){
		RayTrace rayTrace = new RayTrace(camera);
		Ray ray = rayTrace.cast(e.getX(), e.getY());
		Vector3d p = ray.getHitpoint();
		if (p != null) {
			this.z = camera.getTranslation().z
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

			double x = e.getPoint().getX() * canvasSize.getWidth() / GuiState3DWCS.mainComponentView.getComponent().getWidth();
			double y = e.getPoint().getY() * canvasSize.getHeight() / GuiState3DWCS.mainComponentView.getComponent().getHeight();
			double yMeterInNearPlane = (y - canvasSize.getHeight() / 2.)
					* meterPerPixelHeight;
			double xMeterInNearPlane = (x - canvasSize.getWidth() / 2.)
					* meterPerPixelWidth;
			double yAngle = Math.atan2(yMeterInNearPlane, camera.getClipNear());
			double xAngle = Math.atan2(xMeterInNearPlane, camera.getClipNear());
			double yPosition = Math.tanh(yAngle) * z;
			double xPosition = Math.tanh(xAngle) * z;

			this.defaultTranslation = camera.getTranslation();
			this.defaultTranslation = new Vector3d(
			        this.defaultTranslation.x + xPosition,
			        this.defaultTranslation.y - yPosition,
			        this.defaultTranslation.z);
		}
	}
	
	private void mousePressed2DFunction(MouseEvent e){		
		if (LayersModel.getSingletonInstance().getActiveView() != null){
		Region region = LayersModel.getSingletonInstance().getActiveView().getAdapter(MetaDataView.class).getMetaData().getPhysicalRegion();
		double halfWidth = region.getWidth() / 2;
		double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
		double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad)
				/ Math.sin(halfFOVRad);
		double scaleFactor = -camera.getTranslation().z / distance;
		double aspect = camera.getAspect();

		double width = region.getWidth() * scaleFactor * aspect;
		double height = region.getHeight() * scaleFactor;

		if (region != null){
		Dimension canvasSize = GuiState3DWCS.mainComponentView.getComponent().getSize();
		this.meterPerPixelWidth = width / canvasSize.getWidth();
		this.meterPerPixelHeight = height / canvasSize.getHeight();
		this.defaultTranslation = camera.getTranslation();
		this.defaultTranslation = new Vector3d(
		        this.defaultTranslation.x - this.meterPerPixelWidth * e.getX(),
		        this.defaultTranslation.y + this.meterPerPixelHeight * e.getY(),
		        this.defaultTranslation.z);
		}
		}
	}
	
	public void mouseDragged(MouseEvent e) {
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
			this.mouseDragged3DFunction(e);
		else this.mouseDragged2DFunction(e);
	}

	private void mouseDragged3DFunction(MouseEvent e){
		if (defaultTranslation != null) {
			Dimension canvasSize = GuiState3DWCS.mainComponentView.getCanavasSize();
			double x = e.getPoint().getX() * canvasSize.getWidth() / GuiState3DWCS.mainComponentView.getComponent().getWidth();
			double y = e.getPoint().getY() * canvasSize.getHeight() / GuiState3DWCS.mainComponentView.getComponent().getHeight();

			double yMeterInNearPlane = (y - canvasSize.getHeight() / 2.)
					* meterPerPixelHeight;
			double xMeterInNearPlane = (x - canvasSize.getWidth() / 2.)
					* meterPerPixelWidth;
			double yAngle = Math.atan2(yMeterInNearPlane, camera.getClipNear());
			double xAngle = Math.atan2(xMeterInNearPlane, camera.getClipNear());
			double yPosition = Math.tanh(yAngle) * z;
			double xPosition = Math.tanh(xAngle) * z;

			camera.setTranslation(new Vector3d(
			        defaultTranslation.x - xPosition,
			        defaultTranslation.y + yPosition,
			        camera.getTranslation().z));


			camera.fireCameraMoving();
		}
	}
	
	private void mouseDragged2DFunction(MouseEvent e){
		if (defaultTranslation != null){
		    camera.setTranslation(new Vector3d(
		            this.defaultTranslation.x + this.meterPerPixelWidth * e.getX(),
		            this.defaultTranslation.y - this.meterPerPixelHeight * e.getY(),
		            camera.getTranslation().z));
			camera.fireCameraMoving();
		}
	}
@Override
	public void mouseReleased(MouseEvent e) {
		camera.fireCameraMoving();
	}


}