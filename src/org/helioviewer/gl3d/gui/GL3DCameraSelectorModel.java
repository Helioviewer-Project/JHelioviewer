package org.helioviewer.gl3d.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.ListModel;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.GL3DHelper;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraRotationAnimation;
import org.helioviewer.gl3d.camera.GL3DSolarRotationTrackingTrackballCamera;
import org.helioviewer.gl3d.camera.GL3DTrackballCamera;
import org.helioviewer.gl3d.scenegraph.GL3DState;
import org.helioviewer.gl3d.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.gl3d.scenegraph.math.GL3DQuatd;
import org.helioviewer.gl3d.scenegraph.math.GL3DVec3d;
import org.helioviewer.gl3d.view.GL3DCameraView;
import org.helioviewer.gl3d.view.GL3DCoordinateSystemView;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.wcs.CoordinateConversion;
import org.helioviewer.gl3d.wcs.CoordinateVector;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.View;

/**
 * Can be used as the global singleton for all available and the currently
 * active {@link GL3DCamera}. Also it implements the {@link ComboBoxModel} and
 * {@link ListModel} and can thus be used for GUI elements directly.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DCameraSelectorModel extends AbstractListModel implements ComboBoxModel, LayersListener {
	
	
	private static final long serialVersionUID = 1L;

    private static GL3DCameraSelectorModel instance;

    private List<GL3DCamera> cameras = new ArrayList<GL3DCamera>();

    private GL3DCamera defaultCamera;

    private GL3DCamera lastCamera;

    private GL3DTrackballCamera trackballCamera;

    private GL3DSolarRotationTrackingTrackballCamera solarRotationCamera;
    private VISUAL_TYPE visualType = VISUAL_TYPE.MODE_3D;

    public static GL3DCameraSelectorModel getInstance() {
        if (instance == null) {
            instance = new GL3DCameraSelectorModel();
        }
        return instance;
    }

    private GL3DCameraSelectorModel() {
    	LayersModel.getSingletonInstance().addLayersListener(this);
        // StateController.getInstance().addStateChangeListener(new
        // StateChangeListener() {
        //
        // public void stateChanged(State newState, State oldState,
        // StateController stateController) {
        // if(newState.getType()==ViewStateEnum.View3D) {
        // //Needs to be checked, because if new State is 2D no CameraView is
        // available.
        //
        // } else {
        // Log.info("GL3DCameraSelectorModel: No camera change, no GL3DSceneGraphView available");
        // }
        // }
        // });
    }

    public void activate(GL3DSceneGraphView sceneGraphView) {
        // GL3DSceneGraphView sceneGraphView =
        // getMainView().getAdapter(GL3DSceneGraphView.class);

        if (sceneGraphView != null) {
            trackballCamera = new GL3DTrackballCamera(sceneGraphView);
            solarRotationCamera = new GL3DSolarRotationTrackingTrackballCamera(sceneGraphView);
            defaultCamera = solarRotationCamera;
            lastCamera = defaultCamera;
            cameras.add(trackballCamera);
            cameras.add(solarRotationCamera);
            defaultCamera = trackballCamera;
            trackballCamera.setSceneGraphView(sceneGraphView);
            solarRotationCamera.setSceneGraphView(sceneGraphView);

            if (getCameraView() != null) {
                setCurrentCamera(lastCamera);
            } else {
                Log.warn("Cannot set Current Camera, no GL3DCameraView yet!");
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
            throw new IllegalArgumentException("Cannot set Selected Camera to an object of Type other than " + GL3DCamera.class + ". Given Object is " + anItem);
        }
    }

    private ComponentView getMainView() {
        ImageViewerGui imageViewer = ImageViewerGui.getSingletonInstance();
        if (imageViewer == null)
            return null;
        return imageViewer.getMainView();
    }

    private GL3DCameraView getCameraView() {
        ComponentView mainView = getMainView();
        if (mainView != null) {
            return mainView.getAdapter(GL3DCameraView.class);
        }
        return null;
    }

    public GL3DTrackballCamera getTrackballCamera() {
        return trackballCamera;
    }

    public GL3DSolarRotationTrackingTrackballCamera getSolarRotationCamera() {
        return solarRotationCamera;
    }

    public void set3DMode(){
    	this.visualType = VISUAL_TYPE.MODE_3D;
    }
    
    public void set2DMode(){
    	this.getCurrentCamera().reset();
    	this.visualType = VISUAL_TYPE.MODE_2D;
    	if (LayersModel.getSingletonInstance().getActiveView() != null)
    		this.layerAdded(0);
    }
    
    private void rotateToCurrentLayer(){
		View view = LayersModel.getSingletonInstance().getActiveView();
		GL3DCoordinateSystemView layer = view.getAdapter(GL3DCoordinateSystemView.class);
		GL3DState state = GL3DState.get();
		CoordinateVector orientationVector = layer.getOrientation();
        CoordinateConversion toViewSpace = layer.getCoordinateSystem().getConversion(state.getActiveCamera().getViewSpaceCoordinateSystem());
        GL3DVec3d orientation = GL3DHelper.toVec(toViewSpace.convert(orientationVector)).normalize();
        
        GL3DQuatd phiRotation = GL3DQuatd.calcRotation(orientation,new GL3DVec3d(0,0,1));	        
        GL3DQuatd targetRotation = phiRotation;
        this.getCurrentCamera().addCameraAnimation(new GL3DCameraRotationAnimation(targetRotation, 700));
    }
    
	@Override
	public void layerAdded(int idx) {
		if (this.visualType == VISUAL_TYPE.MODE_2D){
			this.rotateToCurrentLayer();
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
		if (this.visualType == VISUAL_TYPE.MODE_2D){
			this.rotateToCurrentLayer();
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