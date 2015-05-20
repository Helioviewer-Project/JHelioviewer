package org.helioviewer.jhv.gui.components.newComponents;

import java.awt.Dimension;
import java.util.ArrayList;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.layers.LayerInterface;
import org.helioviewer.jhv.layers.Layers;
import org.helioviewer.jhv.layers.NewLayer;
import org.helioviewer.jhv.opengl.camera.CameraInteraction;
import org.helioviewer.jhv.opengl.camera.CameraPanInteraction;
import org.helioviewer.jhv.opengl.camera.CameraRotationInteraction;
import org.helioviewer.jhv.opengl.camera.CameraZoomInteraction;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;

public class OverViewPanel extends MainPanel{

	private ArrayList<MainPanel> mainViews;
	
	public OverViewPanel() {
    	super();
		mainViews = new ArrayList<MainPanel>();
    	
    	this.cameraInteractions = new CameraInteraction[2];
	}
	
	@Override
	public void setRotationInteraction(){
		this.cameraInteractions[1] = new CameraRotationInteraction(this, mainViews.get(0));
	}
	
	@Override
	public void setPanInteraction(){
		this.cameraInteractions[1] = new CameraPanInteraction(this, mainViews.get(0));
	}
	
	private void zoomToFit(){
    	LayerInterface activeLayer = Layers.LAYERS.getActiveLayer();
        if (activeLayer != null && activeLayer.getMetaData() != null){
        	PhysicalRegion region = activeLayer.getMetaData().getPhysicalRegion();
            if (region != null) {
                double halfWidth = region.getHeight() / 2;
                Dimension canvasSize = this.getSize();
                double aspect = canvasSize.getWidth() / canvasSize.getHeight();
                halfWidth = aspect > 1 ? halfWidth * aspect : halfWidth;
                double halfFOVRad = Math.toRadians(OverViewPanel.FOV / 2.0);
                double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
                this.translation = new Vector3d(0, 0, distance);
            }
        }
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		this.zoomToFit();
		super.display(drawable);
		GL2 gl = drawable.getGL().getGL2();
		displayRect(gl);
	}
	
	@Override
	public void displayLayer(GL2 gl, NewLayer layer) {
		System.out.println("repaint");
		this.rotation = MainFrame.MAIN_PANEL.getRotation();
		super.displayLayer(gl, layer);
	}
	
	public void displayRect(GL2 gl){
		for (MainPanel mainView : mainViews){
			double[][] bounds = mainView.getRectBounds();
		gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glVertex3d(bounds[0][0], bounds[0][1], bounds[0][2]);
			gl.glVertex3d(bounds[1][0], bounds[1][1], bounds[1][2]);
			gl.glVertex3d(bounds[2][0], bounds[2][1], bounds[2][2]);
			gl.glVertex3d(bounds[3][0], bounds[3][1], bounds[3][2]);
		gl.glEnd();
		}
	}
	
	public void addMainView(MainPanel compenentView){
		mainViews.add(compenentView);
    	this.cameraInteractions[0] = new CameraZoomInteraction(this, compenentView);
    	this.cameraInteractions[1] = new CameraRotationInteraction(this, compenentView);
	}
}
