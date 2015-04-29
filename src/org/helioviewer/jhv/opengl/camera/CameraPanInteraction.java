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
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class CameraPanInteraction extends CameraInteraction {
	private double meterPerPixelWidth;
	private double meterPerPixelHeight;
	private double z;
	private Vector3d defaultTranslation;

	public CameraPanInteraction(CompenentView compenentView) {
		super(compenentView);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D){
			this.mousePressed3DFunction(e);
		}
		else {
			this.mousePressed2DFunction(e);
		}
	}

	private void mousePressed3DFunction(MouseEvent e){
		RayTrace rayTrace = new RayTrace();
		Ray ray = rayTrace.cast(e.getX(), e.getY(), compenentView);
		Vector3d p = ray.getHitpoint();
		if (p != null) {
			this.z = compenentView.getTranslation().z
					+ compenentView.getRotation().toMatrix().inverse()
							.multiply(p).z;

			Dimension canvasSize = GuiState3DWCS.mainComponentView.getCanavasSize();
			double halfClipNearHeight = Math.tanh(Math.toRadians(CompenentView
					.FOV / 2)) * CompenentView.CLIP_NEAR;
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
			double yAngle = Math.atan2(yMeterInNearPlane, CompenentView.CLIP_NEAR);
			double xAngle = Math.atan2(xMeterInNearPlane, CompenentView.CLIP_NEAR);
			double yPosition = Math.tanh(yAngle) * z;
			double xPosition = Math.tanh(xAngle) * z;

			this.defaultTranslation = compenentView.getTranslation();
			this.defaultTranslation = new Vector3d(
			        this.defaultTranslation.x + xPosition,
			        this.defaultTranslation.y - yPosition,
			        this.defaultTranslation.z);
		}
	}
	
	private void mousePressed2DFunction(MouseEvent e){		
		if (LayersModel.getSingletonInstance().getActiveView() != null){
		PhysicalRegion region = LayersModel.getSingletonInstance().getActiveView().getAdapter(MetaDataView.class).getMetaData().getPhysicalRegion();
		double halfWidth = region.getWidth() / 2;
		double halfFOVRad = Math.toRadians(CompenentView.CLIP_NEAR / 2.0);
		double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad)
				/ Math.sin(halfFOVRad);
		double scaleFactor = -compenentView.getTranslation().z / distance;
		double aspect = compenentView.getAspect();

		double width = region.getWidth() * scaleFactor * aspect;
		double height = region.getHeight() * scaleFactor;

		if (region != null){
		Dimension canvasSize = GuiState3DWCS.mainComponentView.getComponent().getSize();
		this.meterPerPixelWidth = width / canvasSize.getWidth();
		this.meterPerPixelHeight = height / canvasSize.getHeight();
		this.defaultTranslation = compenentView.getTranslation();
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
			Dimension canvasSize = compenentView.getCanavasSize();
			double x = e.getPoint().getX() * canvasSize.getWidth() / GuiState3DWCS.mainComponentView.getComponent().getWidth();
			double y = e.getPoint().getY() * canvasSize.getHeight() / GuiState3DWCS.mainComponentView.getComponent().getHeight();

			double yMeterInNearPlane = (y - canvasSize.getHeight() / 2.)
					* meterPerPixelHeight;
			double xMeterInNearPlane = (x - canvasSize.getWidth() / 2.)
					* meterPerPixelWidth;
			double yAngle = Math.atan2(yMeterInNearPlane, CompenentView.CLIP_NEAR);
			double xAngle = Math.atan2(xMeterInNearPlane, CompenentView.CLIP_NEAR);
			double yPosition = Math.tanh(yAngle) * z;
			double xPosition = Math.tanh(xAngle) * z;

			compenentView.setTranslation(new Vector3d(
			        defaultTranslation.x - xPosition,
			        defaultTranslation.y + yPosition,
			        compenentView.getTranslation().z));


		}
	}
	
	private void mouseDragged2DFunction(MouseEvent e){
		if (defaultTranslation != null){
		    compenentView.setTranslation(new Vector3d(
		            this.defaultTranslation.x + this.meterPerPixelWidth * e.getX(),
		            this.defaultTranslation.y - this.meterPerPixelHeight * e.getY(),
		            compenentView.getTranslation().z));
		}
	}
}