package org.helioviewer.jhv.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;

import org.helioviewer.jhv.base.math.Quaternion3d;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.wcs.CoordinateConversion;
import org.helioviewer.jhv.base.wcs.CoordinateVector;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DCameraRotationAnimation;
import org.helioviewer.jhv.opengl.camera.GL3DTrackballCamera;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DCameraView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DCoordinateSystemView;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * Can be used as the global singleton for all available and the currently
 * active {@link GL3DCamera}. Also it implements the {@link ComboBoxModel} and
 * {@link ListModel} and can thus be used for GUI elements directly.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraSelectorModel extends AbstractListModel<Object>
		implements ComboBoxModel<Object>, LayersListener {

	private static final long serialVersionUID = 1L;

	private static GL3DCameraSelectorModel instance;

	private List<GL3DCamera> cameras = new ArrayList<GL3DCamera>();

	private GL3DCamera defaultCamera;

	private GL3DCamera lastCamera;

	private GL3DTrackballCamera trackballCamera;

	private VISUAL_TYPE visualType = VISUAL_TYPE.MODE_3D;

	public static GL3DCameraSelectorModel getInstance() {
		if (instance == null) {
			instance = new GL3DCameraSelectorModel();
		}
		return instance;
	}

	private GL3DCameraSelectorModel() {
		LayersModel.getSingletonInstance().addLayersListener(this);
	}

	public void activate(GL3DSceneGraphView sceneGraphView) {
		// GL3DSceneGraphView sceneGraphView =
		// getMainView().getAdapter(GL3DSceneGraphView.class);

		if (sceneGraphView != null) {
			trackballCamera = new GL3DTrackballCamera(sceneGraphView);
			cameras.add(trackballCamera);
			defaultCamera = trackballCamera;
			lastCamera = defaultCamera;
			trackballCamera.setSceneGraphView(sceneGraphView);

			if (getCameraView() != null) {
				setCurrentCamera(lastCamera);
			} else {
				System.out.println("Cannot set Current Camera, no GL3DCameraView yet!");
			}
		}
		getCameraView().setCurrentCamera(defaultCamera);
	}

	public GL3DCamera getCurrentCamera() {
		return getCameraView().getCurrentCamera();
	}

	public Object getElementAt(int index) {
		return cameras.get(index);
	}

	public int getSize() {
		return cameras.size();
	}

	public GL3DCamera getSelectedItem() {
		return getCameraView().getCurrentCamera();
	}

	public void setCurrentCamera(GL3DCamera camera) {
		lastCamera = camera;
		getCameraView().setCurrentCamera(camera);
	}

	public void setSelectedItem(Object anItem) {
		if (anItem instanceof GL3DCamera) {
			setCurrentCamera((GL3DCamera) anItem);
		} else {
			throw new IllegalArgumentException(
					"Cannot set Selected Camera to an object of Type other than "
							+ GL3DCamera.class + ". Given Object is " + anItem);
		}
	}

	private GL3DCameraView getCameraView() {
		return GuiState3DWCS.mainComponentView.getAdapter(GL3DCameraView.class);
	}

	public GL3DTrackballCamera getTrackballCamera() {
		return trackballCamera;
	}

	public void set3DMode() {
		this.visualType = VISUAL_TYPE.MODE_3D;
	}

	public void set2DMode() {
		this.getCurrentCamera().reset();
		this.visualType = VISUAL_TYPE.MODE_2D;
		if (LayersModel.getSingletonInstance().getActiveView() != null)
			this.rotateToCurrentLayer(700);
	}

	public void rotateToCurrentLayer(long duration) {
		View view = LayersModel.getSingletonInstance().getActiveView();
		if (view != null){GL3DCoordinateSystemView layer = view
				.getAdapter(GL3DCoordinateSystemView.class);
		GL3DState state = GL3DState.get();
		CoordinateVector orientationVector = layer.getOrientation();
		CoordinateConversion toViewSpace = layer.getCoordinateSystem()
				.getConversion(
						state.activeCamera.getViewSpaceCoordinateSystem());
		Vector3d orientation = toViewSpace.convert(orientationVector)
				.toVector3d().normalize();

		
		Quaternion3d phiRotation = Quaternion3d.calcRotation(orientation,
				new Vector3d(0, 0, 1));
		Quaternion3d targetRotation = phiRotation;
		
		this.getCurrentCamera().addCameraAnimation(
				new GL3DCameraRotationAnimation(targetRotation, duration));
		}
	}

	@Override
	public void layerAdded(int idx) {
		if (this.visualType == VISUAL_TYPE.MODE_2D) {
			this.rotateToCurrentLayer(700);
		}
	}

	@Override
	public void layerRemoved(View oldView, int oldIdx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void layerChanged(int idx) {

		// TODO Auto-generated method stub

	}

	@Override
	public void activeLayerChanged(int idx) {
		if (this.visualType == VISUAL_TYPE.MODE_2D && idx >= 0) {
			this.rotateToCurrentLayer(700);
		}
	}

	@Override
	public void viewportGeometryChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public void timestampChanged(int idx) {
		// TODO Auto-generated method stub

	}

	@Override
	public void subImageDataChanged() {
		// TODO Auto-generated method stub

	}

	@Override
	public void layerDownloaded(int idx) {
		// TODO Auto-generated method stub

	}

}