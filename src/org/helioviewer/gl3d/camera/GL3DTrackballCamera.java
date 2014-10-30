package org.helioviewer.gl3d.camera;

import java.util.Date;
import java.util.GregorianCalendar;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.base.physics.DifferentialRotation;
import org.helioviewer.base.physics.HeliocentricCartesianCoordinatesFromEarth;
import org.helioviewer.base.physics.StonyhurstHeliographicCoordinates;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.gl3d.scenegraph.math.GL3DMat4d;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateSystem;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.gl3d.wcs.HeliocentricCartesianCoordinateSystem;
import org.helioviewer.gl3d.wcs.conversion.SolarSphereToStonyhurstHeliographicConversion;
import org.helioviewer.gl3d.wcs.impl.SolarSphereCoordinateSystem;
import org.helioviewer.gl3d.wcs.impl.StonyhurstHeliographicCoordinateSystem;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayersModel;
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
 * The trackball camera provides a trackball rotation behavior (
 * {@link GL3DTrackballRotationInteraction}) when in rotation mode. It is
 * currently the default camera.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DTrackballCamera extends GL3DCamera implements ViewListener{
    public static final double DEFAULT_CAMERA_DISTANCE = 12 * Constants.SunRadius;
    private boolean track;
    private GL3DRay lastMouseRay;

    // protected CoordinateSystem viewSpaceCoordinateSystem = new
    // HEECoordinateSystem();
    protected CoordinateSystem viewSpaceCoordinateSystem = new HeliocentricCartesianCoordinateSystem();
    // protected CoordinateSystem viewSpaceCoordinateSystem = new
    // HEEQCoordinateSystem(new Date());
    // protected CoordinateSystem viewSpaceCoordinateSystem = new
    // SolarSphereCoordinateSystem();

    private GL3DTrackballRotationInteraction rotationInteraction;
    private GL3DPanInteraction panInteraction;
    private GL3DZoomBoxInteraction zoomBoxInteraction;

    protected GL3DSceneGraphView sceneGraphView;

    protected GL3DInteraction currentInteraction;

	private Date startDate = null;

	private boolean solarTrackingState = false;
	private CoordinateVector startPosition = null;

	private Date currentDate = null;
	private double currentRotation = 0.0;

	private StonyhurstHeliographicCoordinateSystem stonyhurstCoordinateSystem = new StonyhurstHeliographicCoordinateSystem();
	private SolarSphereCoordinateSystem solarSphereCoordinateSystem = new SolarSphereCoordinateSystem();
	private SolarSphereToStonyhurstHeliographicConversion stonyhurstConversion = (SolarSphereToStonyhurstHeliographicConversion) solarSphereCoordinateSystem
			.getConversion(stonyhurstCoordinateSystem);
	private boolean startPositionIsInsideDisc;
	private GL3DVec3d startPosition2D;
	private GL3DVec3d defaultTranslation;
	
    public GL3DTrackballCamera(GL3DSceneGraphView sceneGraphView) {
        this.sceneGraphView = sceneGraphView;
        this.rotationInteraction = new GL3DTrackballRotationInteraction(this, sceneGraphView);
        this.panInteraction = new GL3DPanInteraction(this, sceneGraphView);
        this.zoomBoxInteraction = new GL3DZoomBoxInteraction(this, sceneGraphView);

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
					&& (timestampReason.getView() instanceof TimedMovieView)
					&& LinkedMovieManager.getActiveInstance().isMaster(
							(TimedMovieView) timestampReason.getView())) {

					currentDate = timestampReason.getNewDateTime().getTime();
					if (startPosition != null) {
						long timediff = (currentDate.getTime() - startDate
								.getTime()) / 1000;

						double theta = startPosition
								.getValue(StonyhurstHeliographicCoordinateSystem.THETA);
						double rotation = DifferentialRotation
								.calculateRotationInRadians(theta, timediff);
						System.out.println("startPosition : " + startPosition2D);
						GL3DQuatd newRotation = GL3DQuatd.createRotation(currentRotation
								- rotation, new GL3DVec3d(0, 1, 0));
						GL3DVec3d newPosition = newRotation.toMatrix().multiply(startPosition2D);
						System.out.println("newPosition   : " + newPosition);
						System.out.println("translation   : " + translation);
						if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
						this.getRotation().rotate(
								GL3DQuatd.createRotation(currentRotation
										- rotation, new GL3DVec3d(0, 1, 0)));
						else{
							this.translation.x += newPosition.x - startPosition2D.x;
							this.translation.y += newPosition.y - startPosition2D.y;
						}
						fireCameraMoved();
						this.updateCameraTransformation();
						this.currentRotation = rotation;
					} else {
						currentRotation = 0.0;
						resetStartPosition();
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
		this.startPosition2D = position;
		this.defaultTranslation = this.translation.copy();
		this.defaultTranslation.x += startPosition2D.x;
		this.defaultTranslation.y -= startPosition2D.y;
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

    public GL3DInteraction getZoomInteraction() {
        return this.zoomBoxInteraction;
    }

    public GL3DRay getLastMouseRay() {
        return lastMouseRay;
    }

    public CoordinateSystem getViewSpaceCoordinateSystem() {
        return this.viewSpaceCoordinateSystem;
    }

    public GL3DMat4d getVM() {
        GL3DMat4d c = this.getCameraTransformation().copy();
        return c;
    }

    public String getName() {
        return "Trackball";
    }

	@Override
	public void setTrack(boolean track) {
		this.track = track;	
	}
}