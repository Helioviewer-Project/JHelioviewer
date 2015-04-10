package org.helioviewer.jhv.opengl.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.math.Matrix4d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.wcs.impl.SolarImageCoordinateSystem;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * The zoom box interaction allows the user to select a region of interest in
 * the scene by dragging. The camera then moves accordingly so that only the
 * selected region is contained within the view frustum. If the zoom box is
 * restricted to the solar disk, the camera panning will be reset and a rotation
 * is applied. When the zoom box intersects with the corona the rotation is
 * reset and only a panning is applied.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DZoomBoxInteraction extends GL3DDefaultInteraction {
    private Vector3d zoomBoxStartPoint;
    private Vector3d zoomBoxEndPoint;
	private double metetPerPixel;

    public GL3DZoomBoxInteraction(GL3DTrackballCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera, sceneGraph);
        new SolarImageCoordinateSystem();
    }

    public void drawInteractionFeedback(GL3DState state, GL3DCamera camera) {
        if (this.isValidZoomBox()) {
        	Matrix4d rot = camera.getRotation().toMatrix().inverse();
        	Vector3d p0 = new Vector3d(zoomBoxStartPoint);
        	Vector3d p1 = new Vector3d(zoomBoxEndPoint.x, zoomBoxStartPoint.y, 0);
        	Vector3d p2 = new Vector3d(zoomBoxEndPoint);
        	Vector3d p3 = new Vector3d(zoomBoxStartPoint.x, zoomBoxEndPoint.y, 0);
        	p0 = rot.multiply(p0);
        	p1 = rot.multiply(p1);
        	p2 = rot.multiply(p2);
        	p3 = rot.multiply(p3);
        	
        	GL2 gl = state.gl;
            gl.glColor3d(1, 1, 0);
            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_TEXTURE_2D);

            gl.glLineWidth(2.0f);
            gl.glBegin(GL2.GL_LINE_LOOP);

            gl.glVertex3d(p0.x, p0.y, p0.z);
            gl.glVertex3d(p1.x, p1.y, p1.z);
            gl.glVertex3d(p2.x, p2.y, p2.z);
            gl.glVertex3d(p3.x, p3.y, p3.z);

            gl.glEnd();

            gl.glLineWidth(1.0f);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glEnable(GL2.GL_TEXTURE_2D);
        }
    }

    public void mousePressed(MouseEvent e, GL3DCamera camera) {
    	calculatePixelPerMeter(camera);
        this.zoomBoxStartPoint = calculatePoint(e.getPoint(),camera);
        this.camera.fireCameraMoving();
    }


	public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        this.zoomBoxEndPoint = calculatePoint(e.getPoint(), camera);
        this.camera.fireCameraMoving();
    }

    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        if (this.isValidZoomBox()) {
            camera.addCameraAnimation(createZoomAnimation());
            double x = -(this.zoomBoxEndPoint.x + this.zoomBoxStartPoint.x) / 2.0;
            double y = -(this.zoomBoxEndPoint.y + this.zoomBoxStartPoint.y) / 2.0;
            Vector3d distanceToMove = new Vector3d((x - camera.translation.x), (y - camera.translation.y), 0);
            camera.addCameraAnimation(new GL3DCameraPanAnimation(distanceToMove, 300));;
        }
        this.zoomBoxEndPoint = null;
        this.zoomBoxStartPoint = null;
        this.camera.fireCameraMoved();
    }

    private void calculatePixelPerMeter(GL3DCamera camera) {
		double height = Math.tan(Math.toRadians(camera.getFOV())) * camera.getZTranslation();
		this.metetPerPixel = height / (double)GuiState3DWCS.mainComponentView.getComponent().getHeight();
	}
    
    private Vector3d calculatePoint(Point p, GL3DCamera camera){
    	double x = p.getX() - GuiState3DWCS.mainComponentView.getComponent().getWidth()/2.0;
    	double y = p.getY() - GuiState3DWCS.mainComponentView.getComponent().getHeight()/2.0;
    	return new Vector3d((-x*this.metetPerPixel)-camera.getTranslation().x, (y*this.metetPerPixel)-camera.getTranslation().y, 0);
    }

    private GL3DCameraZoomAnimation createZoomAnimation() {
        double width = Math.abs(this.zoomBoxEndPoint.y - this.zoomBoxStartPoint.y);
        double fOVRad = Math.toRadians(camera.getFOV());
        double distance = width / Math.tan(fOVRad);
        distance = -distance - camera.getZTranslation();

        return new GL3DCameraZoomAnimation(distance, 700);
    }

    private boolean isValidZoomBox() {
        return this.zoomBoxEndPoint != null && this.zoomBoxStartPoint != null;
    }
    
}
