package org.helioviewer.jhv.gui;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.camera.*;
import org.helioviewer.jhv.viewmodel.TimeLine;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.util.ArrayList;

public class OverviewPanel extends MainPanel
{
	private ArrayList<MainPanel> mainViews;

	public OverviewPanel(GLContext _context)
	{
		super(_context);
		mainViews = new ArrayList<>();

		cameraInteractions = new CameraInteraction[2];
	}

	@Override
	public void activateRotationInteraction()
	{
		cameraInteractions[1] = new CameraRotationInteraction(this, mainViews.get(0));
	}

	@Override
	public void activatePanInteraction()
	{
		this.cameraInteractions[1] = new CameraViewportPanInteraction(this, mainViews.get(0));
	}

	public void activateZoomBoxInteraction()
	{
		this.cameraInteractions[1] = new CameraZoomBoxInteraction(this, mainViews.get(0));
	}

	private void zoomToFit()
	{
		ImageLayer activeLayer = Layers.getActiveImageLayer();
		if (activeLayer == null)
			return;
		
		LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
		MetaData md=activeLayer.getMetaData(currentDateTime);
		if (md != null)
		{
			Rectangle2D region = md.getPhysicalImageSize();
			if (region != null)
			{
				double halfWidth = region.getHeight() / 2;
				Dimension canvasSize = this.getSize();
				double aspect = canvasSize.getWidth() / canvasSize.getHeight();
				halfWidth = aspect > 1 ? halfWidth * aspect : halfWidth;
				double halfFOVRad = Math.toRadians(OverviewPanel.FOV / 2.0);
				double distance = halfWidth
						* Math.sin(Math.PI / 2 - halfFOVRad)
						/ Math.sin(halfFOVRad);
				this.translationNow = new Vector3d(0, 0, distance);
			}
		}
	}

	@Override
	public void display(@Nullable GLAutoDrawable drawable)
	{
		zoomToFit();
		super.display(drawable);
	}
	
	@Override
	protected float getDesiredRelativeResolution()
	{
		return 0.5f;
	}
	
	@Override
	protected void advanceFrame()
	{
	}

	@Override
	protected void render(GL2 gl, boolean _showLoadingAnimation)
	{
		rotationNow = MainFrame.SINGLETON.MAIN_PANEL.getRotationCurrent();
		super.render(gl, false);
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glScaled(1, getAspect(), 1);
		double width = Math.tan(Math.toRadians(FOV / 2.0)) * this.translationNow.z;
		gl.glOrtho(-width, width, width, -width, -Constants.SUN_RADIUS, Constants.SUN_RADIUS);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glEnable(GL2.GL_DEPTH_TEST);
		if (Layers.getActiveImageLayer() != null)
			displayRect(gl, width / 100.0);
		gl.glPopMatrix();
	}

	private void displayRect(GL2 gl, double radius)
	{
		gl.glDisable(GL2.GL_DEPTH_TEST);
		for (MainPanel mainView : mainViews)
		{
			gl.glColor4f(0, 1, 0, 1);
			gl.glDisable(GL2.GL_TEXTURE_2D);
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_LINE_SMOOTH);
			gl.glBegin(GL2.GL_LINE_LOOP);
			for (Vector3d bound : mainView.getVisibleAreaOutline())
				gl.glVertex3d(bound.x, bound.y, bound.z);
			gl.glEnd();
			
			gl.glBegin(GL2.GL_LINE_LOOP);
			for (int i = 0; i < 15; i++)
			{
				double x = Math.cos(i / 15d * 2 * Math.PI) * radius + mainView.getTranslationCurrent().x;
				double y = Math.sin(i / 15d * 2 * Math.PI) * radius + mainView.getTranslationCurrent().y;
				gl.glVertex2d(x, y);
			}
			gl.glEnd();
		}
		gl.glDisable(GL2.GL_LINE_SMOOTH);
		gl.glDisable(GL2.GL_BLEND);

	}

	public void addMainView(MainPanel compenentView)
	{
		mainViews.add(compenentView);
		this.cameraInteractions[0] = new CameraZoomInteraction(this, compenentView);
		this.cameraInteractions[1] = new CameraRotationInteraction(this, compenentView);
	}

	@Override
	protected void updateTrackRotation()
	{
	}

	@Override
	protected void calculateBounds()
	{
	}
}
