package org.helioviewer.gl3d.camera;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.media.opengl.GL2;

import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRay;
import org.helioviewer.gl3d.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.impl.SolarImageCoordinateSystem;

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
    private GL3DRayTracer rayTracer;
    private GL3DVec3d zoomBoxStartPoint;
    private GL3DVec3d zoomBoxEndPoint;

    public GL3DZoomBoxInteraction(GL3DTrackballCamera camera, GL3DSceneGraphView sceneGraph) {
        super(camera, sceneGraph);
        new SolarImageCoordinateSystem();
    }

    public void drawInteractionFeedback(GL3DState state, GL3DCamera camera) {
        if (this.isValidZoomBox()) {
            double x0, x1, y0, y1;
            if (this.zoomBoxEndPoint.x > this.zoomBoxStartPoint.x) {
                x0 = this.zoomBoxStartPoint.x;
                x1 = this.zoomBoxEndPoint.x;
            } else {
                x1 = this.zoomBoxStartPoint.x;
                x0 = this.zoomBoxEndPoint.x;
            }
            if (this.zoomBoxEndPoint.y > this.zoomBoxStartPoint.y) {
                y0 = this.zoomBoxStartPoint.y;
                y1 = this.zoomBoxEndPoint.y;
            } else {
                y1 = this.zoomBoxStartPoint.y;
                y0 = this.zoomBoxEndPoint.y;
            }

            GL2 gl = state.gl;
            gl.glColor3d(1, 1, 0);
            gl.glDisable(GL2.GL_DEPTH_TEST);
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_TEXTURE_2D);

            gl.glLineWidth(2.0f);
            gl.glBegin(GL2.GL_LINE_LOOP);

            gl.glVertex3d(x0, y0, 0);
            gl.glVertex3d(x1, y0, 0);
            gl.glVertex3d(x1, y1, 0);
            gl.glVertex3d(x0, y1, 0);

            gl.glEnd();

            gl.glLineWidth(1.0f);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glEnable(GL2.GL_TEXTURE_2D);
        }
    }

    public void mousePressed(MouseEvent e, GL3DCamera camera) {
        this.zoomBoxStartPoint = getHitPoint(e.getPoint());
    }

    public void mouseDragged(MouseEvent e, GL3DCamera camera) {
        this.zoomBoxEndPoint = getHitPoint(e.getPoint());
    }

    public void mouseReleased(MouseEvent e, GL3DCamera camera) {
        if (this.isValidZoomBox()) {
            camera.addCameraAnimation(createZoomAnimation());
            long x = Math.round(-(this.zoomBoxEndPoint.x + this.zoomBoxStartPoint.x) / 2);
            long y = Math.round(-(this.zoomBoxEndPoint.y + this.zoomBoxStartPoint.y) / 2);
            camera.addCameraAnimation(createPanAnimation(x, y));
        }
        this.zoomBoxEndPoint = null;
        this.zoomBoxStartPoint = null;
    }

    private GL3DCameraPanAnimation createPanAnimation(long x, long y) {
        GL3DVec3d distanceToMove = new GL3DVec3d((x - camera.translation.x), (y - camera.translation.y), 0);
        // Log.debug("GL3DZoomBoxInteraction: Panning "+distanceToMove);
        return new GL3DCameraPanAnimation(distanceToMove);
    }

    private GL3DCameraZoomAnimation createZoomAnimation() {
        double halfWidth = Math.abs(this.zoomBoxEndPoint.x - this.zoomBoxStartPoint.x) / 2;
        double halfFOVRad = Math.toRadians(camera.getFOV() / 2);
        double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
        distance = -distance - camera.getZTranslation();

        return new GL3DCameraZoomAnimation(distance, 700);
    }

    private boolean isValidZoomBox() {
        return this.zoomBoxEndPoint != null && this.zoomBoxStartPoint != null;
    }

    protected GL3DVec3d getHitPoint(Point p) {
        this.rayTracer = new GL3DRayTracer(sceneGraphView.getHitReferenceShape(), this.camera);
        GL3DRay ray = this.rayTracer.cast(p.x, p.y);
        GL3DVec3d hitPoint = ray.getHitPoint();
        return hitPoint;
    }
}
