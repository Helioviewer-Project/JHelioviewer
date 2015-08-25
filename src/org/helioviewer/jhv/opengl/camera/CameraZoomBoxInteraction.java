package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.opengl.MainPanel;
import org.helioviewer.jhv.opengl.camera.animation.CameraTranslationAnimation;
import org.helioviewer.jhv.opengl.raytrace.RayTrace;

import com.jogamp.opengl.GL2;

public class CameraZoomBoxInteraction extends CameraInteraction {

	private RayTrace rayTrace;

	private Vector3d start;
	private Vector3d end;

	public CameraZoomBoxInteraction(MainPanel mainPanel, Camera camera) {
		super(mainPanel, camera);
		this.rayTrace = new RayTrace();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		start = rayTrace.cast(e.getX(), e.getY(), mainPanel).getHitpoint();
		start = start.scale(new Vector3d(1, -1, 1));
		camera.repaintMain(20);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		end = rayTrace.cast(e.getX(), e.getY(), mainPanel).getHitpoint();
		end = end.scale(new Vector3d(1, -1, 1));
		camera.repaintMain(20);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (start != null && end != null){
		Vector3d newPoint = start.add(end).scale(0.5);
		newPoint = new Vector3d(newPoint.x, -newPoint.y, camera.getTranslation().z);
		Vector3d rect = start.add(end.negate());
		double width;
		if (rect.x > rect.y){
			width = Math.abs(rect.x) / 2.0;
		}
		else {
			width = Math.abs(rect.y * camera.getAspect()) / 2.0;
		}
		
		double z = Math.max(width / Math.tan(Math.toRadians(MainPanel.FOV/2)), MainPanel.MIN_DISTANCE);
		
		newPoint = new Vector3d(newPoint.x, newPoint.y, z);

		start = null;
		end = null;
		camera.addCameraAnimation(new CameraTranslationAnimation(newPoint, camera));
		}
	}

	@Override
	public void renderInteraction(GL2 gl) {
		if (start != null && end != null) {
			gl.glColor3d(1, 1, 1);
			gl.glEnable(GL2.GL_LINE_STIPPLE);
			gl.glDisable(GL2.GL_DEPTH_TEST);
			gl.glDisable(GL2.GL_LIGHTING);
			gl.glDisable(GL2.GL_TEXTURE_2D);

			gl.glLineWidth(2.0f);
			gl.glLineStipple(1, (short) 255);
			gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glVertex2d(start.x, start.y);
			gl.glVertex2d(start.x, end.y);
			gl.glVertex2d(end.x, end.y);
			gl.glVertex2d(end.x, start.y);
			gl.glEnd();

			gl.glLineWidth(1.0f);
			gl.glDisable(GL2.GL_LINE_STIPPLE);
		}
	}
}
