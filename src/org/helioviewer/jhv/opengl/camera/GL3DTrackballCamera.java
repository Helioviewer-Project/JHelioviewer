package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;
import java.util.Date;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.HeliocentricCartesianCoordinateSystem;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;
import org.helioviewer.jhv.viewmodel.view.jp2view.ImmutableDateTime;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * The trackball camera provides a trackball rotation behavior (
 * {@link GL3DTrackballRotationInteraction}) when in rotation mode. It is
 * currently the default camera.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DTrackballCamera extends GL3DCamera implements ViewListener {
	public static final double DEFAULT_CAMERA_DISTANCE = 12 * Constants.SUN_RADIUS;
	private boolean track;
	protected CoordinateSystem viewSpaceCoordinateSystem = new HeliocentricCartesianCoordinateSystem();
	private GL3DTrackballRotationInteraction rotationInteraction;
	private GL3DPanInteraction panInteraction;
	private GL3DZoomBoxInteraction zoomBoxInteraction;

	protected GL3DSceneGraphView sceneGraphView;

	protected GL3DInteraction currentInteraction;

	private Date startDate = null;

	private Date currentDate = null;
	private double currentRotation = 0.0;

	private Vector3d startPosition2D;
	private boolean outside;
	private Vector3d lastMouseHitPoint;

	public GL3DTrackballCamera(GL3DSceneGraphView sceneGraphView) {
		this.sceneGraphView = sceneGraphView;
		this.rotationInteraction = new GL3DTrackballRotationInteraction(this,
				sceneGraphView);
		this.panInteraction = new GL3DPanInteraction(this, sceneGraphView);
		this.zoomBoxInteraction = new GL3DZoomBoxInteraction(this,
				sceneGraphView);

		this.currentInteraction = this.rotationInteraction;
	}

	public void activate(GL3DCamera precedingCamera) {
		super.activate(precedingCamera);
		sceneGraphView.addViewListener(this);
	}

	public void viewChanged(View sender, ChangeEvent aEvent) {
		if (track) {
			TimestampChangedReason timestampReason = aEvent
					.getLastChangedReasonByType(TimestampChangedReason.class);
			if ((timestampReason != null)
					&& (timestampReason.getView() instanceof JHVJPXView)
					&& LinkedMovieManager.getActiveInstance().isMaster(
							(JHVJPXView) timestampReason.getView())) {
				if (!LinkedMovieManager.getActiveInstance().isPlaying()) {
					this.resetStartPosition();
				}
				currentDate = timestampReason.getNewDateTime().getTime();

				if (startDate == null)
					this.startDate = getStartDate();

				long timediff = (currentDate.getTime() - startDate.getTime()) / 1000;

				double rotation = DifferentialRotation
						.calculateRotationInRadians(0, timediff);

				Quaternion3d newRotation = Quaternion3d.createRotation(
						currentRotation - rotation, new Vector3d(0, 1, 0));
				Vector3d newPosition = newRotation.toMatrix().multiply(
						startPosition2D);

				Vector3d tmp = newPosition.subtract(startPosition2D);
				this.startPosition2D = newPosition;
				if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
					this.getRotation().rotate(
							Quaternion3d.createRotation(currentRotation
									- rotation, new Vector3d(0, 1, 0)));

				else if (!outside) {
					this.translation = new Vector3d(this.translation.x + tmp.x,
							this.translation.y - tmp.y, this.translation.z);

				}
				// fireCameraMoved();
				this.updateCameraTransformation();
				this.currentRotation = rotation;
				this.startPosition2D = newPosition;
			} else {
				currentRotation = 0.0;
				resetStartPosition();
			}

		}
	}

	private void resetStartPosition() {
		this.startDate = getStartDate();

		double x = this.getTranslation().x;
		double y = this.getTranslation().y;
		if (x * x + y * y < Constants.SUN_RADIUS * Constants.SUN_RADIUS) {
			double z = Math.sqrt(Constants.SUN_RADIUS * Constants.SUN_RADIUS
					- x * x - y * y);
			this.outside = false;
			this.startPosition2D = new Vector3d(x, y, z);
			this.startPosition2D = this.getRotation().toMatrix()
					.multiply(this.startPosition2D);
		} else
			this.outside = true;
	}

	private Date getStartDate() {
		if (LinkedMovieManager.getActiveInstance() == null) {
			return null;
		}
		JHVJPXView masterView = LinkedMovieManager.getActiveInstance()
				.getMasterMovie();
		if (masterView == null) {
			return null;
		}
		ImmutableDateTime idt = masterView.getCurrentFrameDateTime();
		if (idt == null) {
			return null;
		}
		return idt.getTime();
	}

	public void applyCamera(GL3DState state) {
		// ((HEEQCoordinateSystem)this.viewSpaceCoordinateSystem).setObservationDate(state.getCurrentObservationDate());
		super.applyCamera(state);
	}

	public void setSceneGraphView(GL3DSceneGraphView sceneGraphView) {
		this.sceneGraphView = sceneGraphView;
	}

	public void reset() {
		this.currentInteraction.reset(this);
	}

	public double getDistanceToSunSurface() {
		return -this.getCameraTransformation().translation().z;
	}

	public GL3DInteraction getPanInteraction() {
		return this.panInteraction;
	}

	public GL3DInteraction getRotateInteraction() {
		return this.rotationInteraction;
	}

	public GL3DInteraction getCurrentInteraction() {
		return this.currentInteraction;
	}

	public void setCurrentInteraction(GL3DInteraction currentInteraction) {
		this.currentInteraction = currentInteraction;
	}

	public GL3DInteraction getZoomBoxInteraction() {
		return this.zoomBoxInteraction;
	}

	public CoordinateSystem getViewSpaceCoordinateSystem() {
		return this.viewSpaceCoordinateSystem;
	}

	public Matrix4d getVM() {
		Matrix4d c = this.getCameraTransformation().copy();
		return c;
	}

	public String getName() {
		return "Trackball";
	}

	@Override
	public void setTrack(boolean track) {
		this.track = track;
		this.resetStartPosition();
	}

	@Override
	public boolean isTrack() {
		return track;
	}

	@Override
	public void mouseRay(MouseEvent e) {
		int x = (int) (e.getX()
				/ GuiState3DWCS.mainComponentView.getComponent().getSize()
						.getWidth() * GuiState3DWCS.mainComponentView
				.getComponent().getSurfaceWidth());
		int y = (int) (e.getY()
				/ GuiState3DWCS.mainComponentView.getComponent().getSize()
						.getHeight() * GuiState3DWCS.mainComponentView
				.getComponent().getSurfaceHeight());
		if (sceneGraphView != null && GL3DState.get() != null) {
			GL3DRayTracer rayTracer = new GL3DRayTracer(
					sceneGraphView.getHitReferenceShape(), this);
			GL3DRay ray = rayTracer.cast(x, y);
			Vector3d earthToSun = new Vector3d(0, 0,
					Constants.SUN_MEAN_DISTANCE_TO_EARTH);
			if (ray != null) {
				earthToSun = earthToSun.subtract(ray.getHitPoint());
				double r = earthToSun.length();
				double theta = Math.atan(earthToSun.x
						/ Math.sqrt(earthToSun.y * earthToSun.y + earthToSun.z
								* earthToSun.z));
				double phi = Math.atan2(earthToSun.y, earthToSun.z);

				this.lastMouseHitPoint = new Vector3d(-theta, -phi, r);
			}
		}
	}

	@Override
	public Vector3d getLastMouseHitPoint() {
		return lastMouseHitPoint;
	}
}