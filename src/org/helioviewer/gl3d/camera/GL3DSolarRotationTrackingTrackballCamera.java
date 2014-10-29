package org.helioviewer.gl3d.camera;

import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.base.physics.HeliocentricCartesianCoordinatesFromEarth;
import org.helioviewer.base.physics.StonyhurstHeliographicCoordinates;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.conversion.SolarSphereToStonyhurstHeliographicConversion;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.StonyhurstHeliographicCoordinateSystem;
import org.helioviewer.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.region.StaticRegion;
import org.helioviewer.viewmodel.view.LinkedMovieManager;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.TimedMovieView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.datetime.ImmutableDateTime;

/**
 * This camera is used when solar rotation tracking is enabled. It extends the
 * {@link GL3DTrackballCamera} by automatically rotating the camera around the
 * Y-Axis (pointing to solar north) by an amount calculated through
 * {@link DifferentialRotation}.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSolarRotationTrackingTrackballCamera extends
		GL3DTrackballCamera implements ViewListener {
	private Date startDate = null;

	private boolean solarTrackingState = false;
	private CoordinateVector startPosition = null;

	private Date currentDate = null;
	private double currentRotation = 0.0;

	private StonyhurstHeliographicCoordinateSystem stonyhurstCoordinateSystem = new StonyhurstHeliographicCoordinateSystem();
	private SolarSphereCoordinateSystem solarSphereCoordinateSystem = new SolarSphereCoordinateSystem();
	private SolarSphereToStonyhurstHeliographicConversion stonyhurstConversion = (SolarSphereToStonyhurstHeliographicConversion) solarSphereCoordinateSystem
			.getConversion(stonyhurstCoordinateSystem);

	public GL3DSolarRotationTrackingTrackballCamera(
			GL3DSceneGraphView sceneGraphView) {
		super(sceneGraphView);
	}

	public void activate(GL3DCamera precedingCamera) {
		super.activate(precedingCamera);
		sceneGraphView.addViewListener(this);
	}

	public void deactivate() {
		sceneGraphView.removeViewListener(this);
		this.startPosition = null;
		this.startDate = null;
	};

	public String getName() {
		return "Solar Rotation Tracking Camera";
	}

	public void viewChanged(View sender, ChangeEvent aEvent) {
		if (solarTrackingState) {
			TimestampChangedReason timestampReason = aEvent
					.getLastChangedReasonByType(TimestampChangedReason.class);
			if ((timestampReason != null)
					&& (timestampReason.getView() instanceof TimedMovieView)
					&& LinkedMovieManager.getActiveInstance().isMaster(
							(TimedMovieView) timestampReason.getView())) {

				if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D) {
					currentDate = timestampReason.getNewDateTime().getTime();
					if (startPosition != null) {
						long timediff = (currentDate.getTime() - startDate
								.getTime()) / 1000;

						double theta = startPosition
								.getValue(StonyhurstHeliographicCoordinateSystem.THETA);
						double rotation = DifferentialRotation
								.calculateRotationInRadians(theta, timediff);

						this.getRotation().rotate(
								GL3DQuatd.createRotation(currentRotation
										- rotation, new GL3DVec3d(0, 1, 0)));
						this.updateCameraTransformation();
						this.currentRotation = rotation;
					} else {
						currentRotation = 0.0;
						resetStartPosition();
					}

				} else {
					currentDate = timestampReason.getNewDateTime().getTime();

					GregorianCalendar currentCalendarDate = new GregorianCalendar();
					currentCalendarDate.setTime(currentDate);

					if (startPosition != null) {
						long timeDiff = (currentDate.getTime() - startDate
								.getTime()) / 1000;

						StonyhurstHeliographicCoordinates stonyhurstHeliographicCoordinates = new StonyhurstHeliographicCoordinates(
								startPosition
										.getValue(StonyhurstHeliographicCoordinateSystem.THETA),
								startPosition
										.getValue(StonyhurstHeliographicCoordinateSystem.PHI),
								startPosition
										.getValue(StonyhurstHeliographicCoordinateSystem.RADIUS));
						StonyhurstHeliographicCoordinates currentPosition = DifferentialRotation
								.calculateNextPosition(
										stonyhurstHeliographicCoordinates,
										timeDiff);

						// Move to "parking position" while on the back side of
						// the
						// sun
						if ((currentPosition.phi > 90 && currentPosition.phi < 180)
								|| (currentPosition.phi > -270 && currentPosition.phi < -180)) {

							currentPosition = new StonyhurstHeliographicCoordinates(
									currentPosition.theta, 90,
									currentPosition.r);

						} else if ((currentPosition.phi >= 180 && currentPosition.phi < 270)
								|| (currentPosition.phi >= -180 && currentPosition.phi < -90)) {

							currentPosition = new StonyhurstHeliographicCoordinates(
									currentPosition.theta, 270,
									currentPosition.r);
						}

						HeliocentricCartesianCoordinatesFromEarth currentHCC = new HeliocentricCartesianCoordinatesFromEarth(
								currentPosition, currentCalendarDate);

						RegionView regionView = sender
								.getAdapter(RegionView.class);
						Region currentRegion = regionView.getRegion();
						Vector2dDouble currentCenter = currentHCC
								.getCartesianCoordinatesOnDisc();
						Region newRegion = StaticRegion.createAdaptedRegion(
								currentCenter.subtract(currentRegion.getSize()
										.scale(0.5)), currentRegion.getSize());
						this.addPanning(
								newRegion.getCornerX()
										- currentRegion.getCornerY(),
								newRegion.getCornerY()
										- currentRegion.getCornerY());
					} else {
						resetStartPosition();
					}
				}
			}
		}
	}

	private void resetStartPosition() {
		this.startDate = getStartDate();

		GL3DRayTracer positionTracer = new GL3DRayTracer(
				sceneGraphView.getHitReferenceShape(), this);
		GL3DRay positionRay = positionTracer.castCenter();

		GL3DVec3d position = positionRay.getHitPoint();

		if (position != null) {
			CoordinateVector solarSpherePosition = solarSphereCoordinateSystem
					.createCoordinateVector(position.x, position.y, position.z);
			CoordinateVector stonyhurstPosition = stonyhurstConversion
					.convert(solarSpherePosition);
			// Log.debug("GL3DSolarRotationTrackingCam: StonyhurstPosition="+stonyhurstPosition);
			this.startPosition = stonyhurstPosition;

			Log.debug("GL3DSolarRotationTracking.Set Start hitpoint! "
					+ positionRay.getDirection());
		} else {
			Log.debug("GL3DSolarRotationTracking.cannot reset hitpoint! "
					+ positionRay.getDirection());

		}

	}

	private Date getStartDate() {
		if (LinkedMovieManager.getActiveInstance() == null) {
			return null;
		}
		TimedMovieView masterView = LinkedMovieManager.getActiveInstance()
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

	public void setSolarTracking(boolean solarTrackingState) {
		this.solarTrackingState = solarTrackingState;
	}
}
