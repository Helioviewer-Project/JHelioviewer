package org.helioviewer.jhv.opengl.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.controller.Camera;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;
import org.helioviewer.jhv.opengl.raytrace.RayTrace.Ray;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class CameraRotationInteraction extends CameraInteraction{

	private Vector3d currentRotationStartPoint;
	private Vector3d currentRotationEndPoint;
	private volatile Quaternion3d currentDragRotation;
	private boolean played;

	public static boolean yAxisBlocked = false;
	
	public CameraRotationInteraction(MainPanel compenentView, Camera camera) {
		super(compenentView, camera);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		this.currentRotationEndPoint = getVectorFromSphere(e.getPoint());
		try {
			if (currentRotationStartPoint != null
					&& currentRotationEndPoint != null) {
				if (yAxisBlocked) {
					double s = currentRotationEndPoint.x
							* currentRotationStartPoint.z
							- currentRotationEndPoint.z
							* currentRotationStartPoint.x;
					double c = currentRotationEndPoint.x
							* currentRotationStartPoint.x
							+ currentRotationEndPoint.z
							* currentRotationStartPoint.z;
					double angle = Math.atan2(s, c);
					currentDragRotation = Quaternion3d.createRotation(angle,
							new Vector3d(0, 1, 0));
					componentView.getRotation().rotate(currentDragRotation);
					camera.setRotation(componentView.getRotation());
				} else {
					currentDragRotation = Quaternion3d.calcRotation(
							currentRotationStartPoint, currentRotationEndPoint);
					componentView.getRotation().rotate(currentDragRotation);
					camera.setRotation(componentView.getRotation());
					currentRotationStartPoint = currentRotationEndPoint;
				}
			}
		} catch (IllegalArgumentException exc) {
			System.out.println("GL3DTrackballCamera.mouseDragged: Illegal Rotation ignored!");
            exc.printStackTrace();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		this.currentRotationStartPoint = null;
		this.currentRotationEndPoint = null;
		
			TimeLine.SINGLETON.setPlaying(played);
	}

	@Override
	public void mousePressed(MouseEvent e) {
		this.played = TimeLine.SINGLETON.isPlaying();
		TimeLine.SINGLETON.setPlaying(false);
		// The start point of the rotation remains the same during a drag,
		// because the
		// mouse should always point to the same Point on the Surface of the
		// sphere.
		this.currentRotationStartPoint = getVectorFromSphere(e.getPoint());
	}

	protected Vector3d getVectorFromSphere(Point p) {
		RayTrace rayTrace = new RayTrace();
		Ray ray = rayTrace.cast(p.x, p.y, componentView);

		Vector3d hitPoint = ray.getHitpoint();
		return hitPoint;
	}

}
