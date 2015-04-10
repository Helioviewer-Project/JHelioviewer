package org.helioviewer.jhv.opengl.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * This interaction is used by the {@link GL3DTrackballCamera} as its rotation
 * interaction. The calculation of the rotation done by creating a rotation
 * Quaternion between two points on a sphere. These points are retrieved by
 * using the raycasting mechanism provided by {@link GL3DRayTracer}.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class NewGL3DTrackballRotationInteraction extends GL3DDefaultInteraction {
	private Vector3d currentRotationStartPoint;
	private Vector3d currentRotationEndPoint;
	private volatile Quaternion3d currentDragRotation;
	private boolean yAxisBlocked = false;
	private boolean played;

	protected NewGL3DTrackballRotationInteraction(GL3DTrackballCamera camera,
			GL3DSceneGraphView sceneGraph) {
		super(camera, sceneGraph);
	}

	public void mouseDragged(MouseEvent e, GL3DCamera camera) {
		this.currentRotationEndPoint = getVectorFromSphere(e.getPoint(), camera);
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
					camera.getRotation().rotate(currentDragRotation);
					this.camera.updateCameraTransformation(false);

				} else {
					currentDragRotation = Quaternion3d.calcRotation(
							currentRotationStartPoint, currentRotationEndPoint);
					camera.getRotation().rotate(currentDragRotation);
					this.camera.updateCameraTransformation(false);
				}
			}
		} catch (IllegalArgumentException exc) {
			System.out.println("GL3DTrackballCamera.mouseDragged: Illegal Rotation ignored!");
            exc.printStackTrace();
		}

		camera.fireCameraMoving();
	}

	public void mouseReleased(MouseEvent e, GL3DCamera camera) {
		this.currentRotationStartPoint = null;
		this.currentRotationEndPoint = null;
		
		camera.fireCameraMoved();
		if (this.played){
			LinkedMovieManager.getActiveInstance().playLinkedMovies();
		}
	}

	public void mousePressed(MouseEvent e, GL3DCamera camera) {
		this.played = LinkedMovieManager.getActiveInstance().isPlaying();
		if (played){
			LinkedMovieManager.getActiveInstance().pauseLinkedMovies();
		}
		// The start point of the rotation remains the same during a drag,
		// because the
		// mouse should always point to the same Point on the Surface of the
		// sphere.
		this.currentRotationStartPoint = getVectorFromSphere(e.getPoint(),
				camera);
	}

	protected Vector3d getVectorFromSphere(Point p, GL3DCamera camera) {
		GL3DRayTracer sunTracer = new GL3DRayTracer(
				sceneGraphView.getHitReferenceShape(), camera);
		int mouseX = (int) (p.x
				/ GuiState3DWCS.mainComponentView.getComponent().getSize()
						.getWidth() * GuiState3DWCS.mainComponentView
				.getCanavasSize().getWidth());
		int mouseY = (int) (p.y
				/ GuiState3DWCS.mainComponentView.getComponent().getSize()
						.getHeight() * GuiState3DWCS.mainComponentView
				.getCanavasSize().getHeight());
		GL3DRay ray = sunTracer.cast(mouseX, mouseY);

		Vector3d hitPoint;

		if (ray.isOnSun) {
			// Log.debug("GL3DTrackballRotationInteraction: Ray is Inside!");
			hitPoint = ray.getHitPoint();
		} else {
			// Log.debug("GL3DTrackballRotationInteraction: Ray is Outside!");
			double y = (camera.getHeight() / 2 - mouseY) / camera.getHeight();
			double x = (mouseX - camera.getWidth() / 2) / camera.getWidth();
			// Transform the Point so it lies on the plane that is aligned to
			// the viewspace (not the sphere)
			hitPoint = camera.getRotation().toMatrix().inverse()
					.multiply(new Vector3d(x, y, 0));
		}
		return hitPoint;
	}

	@Override
	public void setYAxisBlocked(boolean value) {
		this.yAxisBlocked = value;
	}
}
