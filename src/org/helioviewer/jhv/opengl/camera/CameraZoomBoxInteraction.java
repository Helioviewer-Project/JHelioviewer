package org.helioviewer.jhv.opengl.camera;

import java.awt.event.MouseEvent;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.RayTrace.Ray;
import org.helioviewer.jhv.opengl.camera.animation.CameraTranslationAnimation;

import com.jogamp.opengl.GL2;

public class CameraZoomBoxInteraction extends CameraInteraction
{
	private @Nullable Vector3d start;
	private @Nullable Vector3d end;

	public CameraZoomBoxInteraction(MainPanel mainPanel, Camera camera)
	{
		super(mainPanel, camera);
	}

	@Override
	public void mousePressed(MouseEvent e, Ray _ray)
	{
		start = _ray.getHitpoint().scale(new Vector3d(1, -1, 1));
		camera.repaint();
	}

	@Override
	public void mouseDragged(MouseEvent e, Ray _ray)
	{
		end = _ray.getHitpoint().scale(new Vector3d(1, -1, 1));
		camera.repaint();
	}

	@SuppressWarnings("null")
	@Override
	public void mouseReleased(MouseEvent e, Ray _ray)
	{
		if (start == null || end == null)
			return;
		
		Vector3d newPoint = start.add(end).scale(0.5);
		newPoint = new Vector3d(newPoint.x, -newPoint.y, camera.getTranslationCurrent().z);
		Vector3d rect = start.add(end.negate());
		
		double width;
		if (rect.x > rect.y)
			width = Math.abs(rect.x) / 2.0;
		else
			width = Math.abs(rect.y * camera.getAspect()) / 2.0;
		
		double z = Math.max(width / Math.tan(Math.toRadians(MainPanel.FOV/2)), MainPanel.MIN_DISTANCE);
		newPoint = new Vector3d(newPoint.x, newPoint.y, z);
		
		start = null;
		end = null;
		camera.addCameraAnimation(new CameraTranslationAnimation(MainFrame.MAIN_PANEL, newPoint.subtract(camera.getTranslationEnd())));
	}

	@SuppressWarnings("null")
	@Override
	public void renderInteraction(GL2 gl)
	{
		if (start != null && end != null)
		{
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
