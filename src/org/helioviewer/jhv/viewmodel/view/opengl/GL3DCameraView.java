package org.helioviewer.jhv.viewmodel.view.opengl;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.GL3DKeyController;
import org.helioviewer.jhv.base.GL3DKeyController.GL3DKeyListener;
import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DCameraListener;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.viewmodel.changeevent.CameraChangeChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.SubimageDataView;
import org.helioviewer.jhv.viewmodel.view.View;

/**
 * The {@link GL3DCameraView} is responsible for applying the currently active
 * {@link GL3DCamera}. Since applying the view space transformation is the first
 * transformation to be applied in a scene, this view must be executed before
 * the {@link GL3DSceneGraphView}.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraView extends AbstractGL3DView implements GL3DView,
		GL3DCameraListener {
	private GL3DCamera camera;

	private List<GL3DCameraListener> listeners = new ArrayList<GL3DCameraListener>();

	public GL3DCameraView() {
		// Register short keys for changing the interaction
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				if (!e.isAltDown()){
				camera.setCurrentInteraction(camera.getPanInteraction());
				GuiState3DWCS.topToolBar.selectPan();
				}
			}
		}, KeyEvent.VK_P);
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				if (!e.isAltDown() && GL3DState.get().getState() == VISUAL_TYPE.MODE_3D){
				camera.setCurrentInteraction(camera.getRotateInteraction());
				GuiState3DWCS.topToolBar.selectRotation();
				}
			}
		}, KeyEvent.VK_R);
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				if (!e.isAltDown() && GL3DState.get().getState() == VISUAL_TYPE.MODE_2D){
				camera.setCurrentInteraction(camera.getZoomBoxInteraction());
				GuiState3DWCS.topToolBar.selectZoomBox();
				}
			}
		}, KeyEvent.VK_Z);
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				if (!e.isAltDown()){
					camera.setTrack(!camera.isTrack());
					GuiState3DWCS.topToolBar.setTrack(camera.isTrack());
				}
			}
		}, KeyEvent.VK_T);

		// Center Image when pressing alt+c
		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {

			public void keyHit(KeyEvent e) {
				if (e.isAltDown()) {
					camera.setPanning(0, 0);
					camera.updateCameraTransformation();
				}
			}
		}, KeyEvent.VK_C);

	}

	public void render3D(GL3DState state) {
		GL2 gl = state.gl;

		if (this.camera != null) {
			state.activeCamera = this.camera;
			GL3DState.get().checkGLErrors("GL3DCameraView.afterRender3D");
			if (this.getView() != null) {
				this.renderChild(gl);
			}
		}
	}

	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
	}

	public GL3DCamera getCurrentCamera() {
		return this.camera;
	}

	public void setCurrentCamera(GL3DCamera cam) {
		if (this.camera != null) {
			this.camera.removeCameraListener(this);
			this.camera.deactivate();
		}
		cam.activate(this.camera);
		this.camera = cam;
		this.camera.addCameraListener(this);
		Log.debug("GL3DCameraView: Set Current Camera to " + this.camera);
		notifyViewListeners(new ChangeEvent(new CameraChangeChangedReason(this,
				this.camera)));
	}

	public void cameraMoved(GL3DCamera camera) {
		if (camera != null){
			for (GL3DCameraListener l : this.listeners) {
				l.cameraMoved(camera);
			}
		}
	}

	public void cameraMoving(GL3DCamera camera) {
		for (GL3DCameraListener l : this.listeners) {
			l.cameraMoving(camera);
		}
	}

	public void addCameraListener(GL3DCameraListener listener) {
		this.listeners.add(listener);
	}

	public void removeCameraListener(GL3DCameraListener listener) {
		this.listeners.remove(listener);
	}

	protected void renderChild(GL2 gl) {
		if (view instanceof GLView) {
			((GLView) view).renderGL(gl, false);
		} else {
			TEXTURE_HELPER.renderImageDataToScreen(gl,
					view.getAdapter(RegionView.class).getRegion(), view
							.getAdapter(SubimageDataView.class)
							.getImageData());
		}
	}
}
