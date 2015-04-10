package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.viewmodel.region.Region;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * Default {@link GL3DInteraction} class that provides a reference to the
 * {@link GL3DSceneGraphView}. Default behavior includes camera reset on double
 * click.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public abstract class GL3DDefaultInteraction extends GL3DInteraction {
    private static final double ZOOM_WHEEL_FACTOR = 1.0 / 20;

    protected GL3DSceneGraphView sceneGraphView;

    protected GL3DDefaultInteraction(GL3DCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera);
        this.sceneGraphView = sceneGraph;
        this.reset();
    }

    public void reset(GL3DCamera camera) {
        reset();
    }

    public void mouseClicked(MouseEvent e, GL3DCamera camera) {
        if (e.getClickCount() == 2) {
            reset();
        }
    }

    public void reset() {
        MetaDataView view = sceneGraphView.getAdapter(MetaDataView.class);
        if (view != null && view.getMetaData() != null) {
            Region region = view.getMetaData().getPhysicalRegion();
            double halfWidth = region.getWidth() / 2;
            double halfFOVRad = Math.toRadians(camera.getFOV() / 2);
            double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
            distance = -distance - camera.getZTranslation();
            // Log.debug("GL3DZoomFitAction: Distance = "+distance+" Existing Distance: "+camera.getZTranslation());
            this.camera.getRotation().clear();
            camera.addCameraAnimation(new GL3DCameraZoomAnimation(distance, 500));
            camera.addCameraAnimation(new GL3DCameraPanAnimation(this.camera.getTranslation().negate()));
        } else if (LayersModel.getSingletonInstance().getActiveView() == null){
            camera.setZTranslation(-GL3DTrackballCamera.DEFAULT_CAMERA_DISTANCE);
            this.camera.getRotation().clear();
            this.camera.updateCameraTransformation();
        }
    }

    public void mouseWheelMoved(MouseWheelEvent e, GL3DCamera camera) {
        double zoomDistance = -(e.getUnitsToScroll()) * camera.getDistanceToSunSurface() * GL3DDefaultInteraction.ZOOM_WHEEL_FACTOR;
        camera.addCameraAnimation(new GL3DCameraZoomAnimation(zoomDistance));
    }

}
