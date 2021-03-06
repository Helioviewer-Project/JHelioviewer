package org.helioviewer.jhv.gui;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import javax.annotation.Nullable;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.camera.CameraInteraction;
import org.helioviewer.jhv.opengl.camera.CameraPanInteraction;
import org.helioviewer.jhv.opengl.camera.CameraRotationInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomBoxInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomInteraction;
import org.helioviewer.jhv.opengl.camera.animation.CameraZoomAnimation;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;

public class OverviewPanel extends MainPanel
{
	private ArrayList<MainPanel> mainViews;

	public OverviewPanel(GLContext _context)
	{
		super(_context);
		mainViews = new ArrayList<>();

		cameraInteractionsLeft = new CameraInteraction[0];
		cameraInteractionsRight = new CameraInteraction[0];
		cameraInteractionsMiddle = new CameraInteraction[0];
	}

	@Override
	public void activateRotationInteraction()
	{
		cameraInteractionsLeft = new CameraInteraction[]
				{
						new CameraZoomInteraction(this, mainViews.get(0)),
						new CameraRotationInteraction(this, mainViews.get(0))
				};
		cameraInteractionsMiddle = new CameraInteraction[]
				{
						new CameraPanInteraction(this, mainViews.get(0), -1)
				};
		cameraInteractionsRight = new CameraInteraction[]
				{
					new CameraPanInteraction(this, mainViews.get(0), -1)
				};
	}

	@Override
	public void activatePanInteraction()
	{
		cameraInteractionsLeft = new CameraInteraction[]
				{
						new CameraZoomInteraction(this, mainViews.get(0)),
						new CameraPanInteraction(this, mainViews.get(0), -1)
				};
		cameraInteractionsMiddle = new CameraInteraction[]
				{
						new CameraRotationInteraction(this, mainViews.get(0))
				};
		cameraInteractionsRight = new CameraInteraction[]
				{
					new CameraRotationInteraction(this, mainViews.get(0))
				};
	}
	
	public void activateZoomBoxInteraction()
	{
		cameraInteractionsLeft = new CameraInteraction[]
				{
						new CameraZoomInteraction(this, mainViews.get(0)),
						new CameraZoomBoxInteraction(this, mainViews.get(0))
				};
		cameraInteractionsMiddle = new CameraInteraction[]
				{
						new CameraPanInteraction(this, mainViews.get(0), -1)
				};
		cameraInteractionsRight = new CameraInteraction[]
				{
					new CameraRotationInteraction(this, mainViews.get(0))
				};
	}

	private void zoomToFit()
	{
		ImageLayer activeLayer = Layers.getActiveImageLayer();
		if (activeLayer == null)
			return;
		
		MetaData md=activeLayer.getCurrentMetaData();
		if (md == null)
			return;
		
		Rectangle2D region = md.getPhysicalImageSize();
		if (region == null)
			return;
		
		double halfWidth = region.getHeight() / 2;
		Dimension canvasSize = this.getSize();
		double aspect = canvasSize.getWidth() / canvasSize.getHeight();
		halfWidth = aspect > 1 ? halfWidth * aspect : halfWidth;
		double halfFOVRad = Math.toRadians(OverviewPanel.FOV / 2.0);
		double distance = halfWidth
				* Math.sin(Math.PI / 2 - halfFOVRad)
				/ Math.sin(halfFOVRad);
		
		double delta = distance - getTranslationEnd().z;
		
		//only move the camera, if it has to move >0.1%
		if(Math.abs(delta) > Math.abs(distance)*0.001)
			addCameraAnimation(new CameraZoomAnimation(this, delta));
	}

	@Override
	public void display(@Nullable GLAutoDrawable drawable)
	{
		zoomToFit();
		super.display(drawable);
	}
	
	@Override
	protected void repaintInternal(boolean _synchronously)
	{
		//repainting will be invoked by the mainpanel
	}
	
	@Override
	protected void render(GL2 gl, boolean _showLoadingAnimation, Dimension _sizeForDecoder)
	{
		rotationNow = MainFrame.SINGLETON.MAIN_PANEL.getRotationCurrent();
		super.render(gl, false, _sizeForDecoder);
		
		if (Layers.getActiveImageLayer() != null)
		{
			gl.glPushMatrix();
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();
			
			//TODO: check, if correct. else-part wasn't there originally. although, this doesn't
			//affect correctness ATM, because overviewpanel will always have aspect>1.1, due to layout
			if(getAspect()>=1)
				gl.glScaled(1, getAspect(), 1);
			else
				gl.glScaled(1, 1/getAspect(), 1);
			
			double width = Math.tan(Math.toRadians(FOV / 2.0)) * this.translationNow.z;
			gl.glOrtho(-width, width, width, -width, -Constants.SUN_RADIUS, Constants.SUN_RADIUS);
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();
			gl.glEnable(GL2.GL_DEPTH_TEST);
			renderViewportOutline(gl, width / 100.0);
			gl.glPopMatrix();
		}
	}

	private void renderViewportOutline(GL2 gl, double radius)
	{
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glColor4f(0, 1, 0, 1);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_LINE_SMOOTH);
		
		for (MainPanel mainView : mainViews)
		{
			gl.glBegin(GL2.GL_LINE_LOOP);
			for (Vector3d bound : mainView.getVisibleAreaOutline())
				if(bound!=null)
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

	public void addMainView(MainPanel componentView)
	{
		mainViews.add(componentView);
		cameraInteractionsLeft = new CameraInteraction[]
				{
						new CameraZoomInteraction(this, componentView),
						new CameraRotationInteraction(this, componentView)
				};
		cameraInteractionsRight = new CameraInteraction[]
				{
					new CameraPanInteraction(this, componentView, -1)
				};
		cameraInteractionsMiddle = new CameraInteraction[0];
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
