package org.helioviewer.jhv.gui.opengl;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.helioviewer.jhv.JHVException.MetaDataException;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.layers.AbstractImageLayer;
import org.helioviewer.jhv.layers.ImageLayer;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.opengl.camera.CameraInteraction;
import org.helioviewer.jhv.opengl.camera.CameraPanInteraction;
import org.helioviewer.jhv.opengl.camera.CameraRotationInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomBoxInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomInteraction;
import org.helioviewer.jhv.plugins.plugin.AbstractPlugin.RENDER_MODE;
import org.helioviewer.jhv.plugins.plugin.UltimatePluginInterface;
import org.helioviewer.jhv.viewmodel.timeline.TimeLine;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

/**
 * The OverViewPanel is used for the current overviewpanel. It extend the
 * MainPanel to used the same renderloop
 * 
 * @author stefanmeier
 *
 */
public class OverViewPanel extends MainPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2662016428464982455L;
	private ArrayList<MainPanel> mainViews;

	public OverViewPanel() {
		super();
		mainViews = new ArrayList<MainPanel>();

		this.cameraInteractions = new CameraInteraction[2];
	}

	@Override
	public void setRotationInteraction() {
		this.cameraInteractions[1] = new CameraRotationInteraction(this,
				mainViews.get(0));
	}

	@Override
	public void setPanInteraction() {
		this.cameraInteractions[1] = new CameraPanInteraction(this,
				mainViews.get(0));
	}

	public void setZoomBoxInteraction() {
		this.cameraInteractions[1] = new CameraZoomBoxInteraction(this,
				mainViews.get(0));
	}

	private void zoomToFit() {
		AbstractImageLayer activeLayer = Layers.getActiveImageLayer();
		if (activeLayer != null){
			Rectangle2D region;
			try {
				LocalDateTime currentDateTime = TimeLine.SINGLETON.getCurrentDateTime();
				region = activeLayer.getMetaData(currentDateTime).getPhysicalImageSize();
				if (region != null) {
					double halfWidth = region.getHeight() / 2;
					Dimension canvasSize = this.getSize();
					double aspect = canvasSize.getWidth()
							/ canvasSize.getHeight();
					halfWidth = aspect > 1 ? halfWidth * aspect : halfWidth;
					double halfFOVRad = Math.toRadians(OverViewPanel.FOV / 2.0);
					double distance = halfWidth
							* Math.sin(Math.PI / 2 - halfFOVRad)
							/ Math.sin(halfFOVRad);
					this.translation = new Vector3d(0, 0, distance);
				}
			} catch (MetaDataException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		this.zoomToFit();
		super.display(drawable);

	}

	@Override
	protected void render(GL2 gl) {
		super.render(gl);
		gl.glPushMatrix();
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glScaled(1, this.getAspect(), 1);
		double width = Math.tan(Math.toRadians(FOV / 2.0)) * this.translation.z;
		gl.glOrtho(-width, width, width, -width, -Constants.SUN_RADIUS,
				Constants.SUN_RADIUS);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
		gl.glEnable(GL2.GL_DEPTH_TEST);
		if (Layers.getActiveImageLayer() != null)
			displayRect(gl, width / 100.0);
		gl.glPopMatrix();
	}

	@Override
	public boolean displayLayer(GL2 gl, ImageLayer layer) {
		this.rotation = MainFrame.MAIN_PANEL.getRotation();
		return super.displayLayer(gl, layer);
	}

	@Override
	protected void renderPlugins(GL2 gl) {
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LESS);
		gl.glDepthMask(false);
		UltimatePluginInterface.SINGLETON.renderPlugin(gl,
				RENDER_MODE.OVERVIEW_PANEL);
		gl.glDepthMask(false);
	}

	private void displayRect(GL2 gl, double radius) {
		gl.glDisable(GL2.GL_DEPTH_TEST);
		for (MainPanel mainView : mainViews) {
			double[][] bounds = mainView.getRectBounds();
			gl.glColor4f(0, 1, 0, 1);
			gl.glDisable(GL2.GL_TEXTURE_2D);
			gl.glEnable(GL2.GL_BLEND);
			gl.glEnable(GL2.GL_LINE_SMOOTH);
			gl.glBegin(GL2.GL_LINE_LOOP);
			for (double[] bound : bounds) {
				gl.glVertex3d(bound[0], bound[1], bound[2]);
			}
			gl.glEnd();

			gl.glBegin(GL2.GL_LINE_LOOP);
			for (int i = 0; i < 30; i++) {
				double x = Math.cos(i / 30.0 * 2 * Math.PI) * radius
						+ mainView.getTranslation().x;
				double y = Math.sin(i / 30.0 * 2 * Math.PI) * radius
						+ mainView.getTranslation().y;
				gl.glVertex2d(x, y);
			}
			gl.glEnd();
		}
		gl.glDisable(GL2.GL_LINE_SMOOTH);
		gl.glDisable(GL2.GL_BLEND);

	}

	public void addMainView(MainPanel compenentView) {
		mainViews.add(compenentView);
		this.cameraInteractions[0] = new CameraZoomInteraction(this,
				compenentView);
		this.cameraInteractions[1] = new CameraRotationInteraction(this,
				compenentView);
	}

	@Override
	protected void calculateTrackRotation() {
	}

	@Override
	protected void calculateBounds() {
	}
}
