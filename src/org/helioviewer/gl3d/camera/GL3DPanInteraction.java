package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
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
    private Point lastMousePoint;
    private double meterPerPixel;
    
    protected GL3DPanInteraction(GL3DTrackballCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera, sceneGraph);
    }
    
    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        this.lastMousePoint = e.getPoint();
        Region region = LayersModel.getSingletonInstance().getActiveView().getAdapter(RegionView.class).getRegion();
        meterPerPixel = region.getHeight()/StateController.getInstance().getCurrentState().getMainComponentView().getCanavasSize().getWidth();
    }

    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        int x = (e.getPoint().x - this.lastMousePoint.x);
        int y = (e.getPoint().y - this.lastMousePoint.y);
        if (sceneGraphView.getAdapter(RegionView.class).getRegion() != null) {
            camera.translation.x += x * meterPerPixel;
            camera.translation.y -= y * meterPerPixel;
        } else {
            camera.translation.x += x / 100.0 * Constants.SunRadius;
            camera.translation.y -= y / 100.0 * Constants.SunRadius;
        }
        this.lastMousePoint = e.getPoint();
        camera.updateCameraTransformation();
        
        camera.fireCameraMoving();
    }
    
    @Override
    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
    	camera.fireCameraMoved();
    }
}
