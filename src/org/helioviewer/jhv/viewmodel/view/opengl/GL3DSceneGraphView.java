package org.helioviewer.jhv.viewmodel.view.opengl;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.helioviewer.jhv.base.GL3DKeyController;
import org.helioviewer.jhv.base.GL3DKeyController.GL3DKeyListener;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.base.math.Vector4d;
import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DCameraZoomAnimation;
import org.helioviewer.jhv.opengl.model.GL3DFramebufferImage;
import org.helioviewer.jhv.opengl.model.GL3DHitReferenceShape;
import org.helioviewer.jhv.opengl.model.GL3DImageLayer;
import org.helioviewer.jhv.opengl.model.GL3DImageLayers;
import org.helioviewer.jhv.opengl.model.GL3DImageMesh;
import org.helioviewer.jhv.opengl.scenegraph.GL3DDrawBits.Bit;
import org.helioviewer.jhv.opengl.scenegraph.GL3DGroup;
import org.helioviewer.jhv.opengl.scenegraph.GL3DModel;
import org.helioviewer.jhv.opengl.scenegraph.GL3DNode;
import org.helioviewer.jhv.opengl.scenegraph.GL3DShape;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRayTracer;
import org.helioviewer.jhv.opengl.scenegraph.visuals.GL3DArrow;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.LayerChangedReason;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.LayeredView;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.RegionView;
import org.helioviewer.jhv.viewmodel.view.SubimageDataView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;

/**
 * This is the most important view in the 3D viewchain. It assembles all 3D
 * Models in a hierarchical scene graph. Also it automatically adds new nodes (
 * {@link GL3DImageMesh}) to the scene when a new layer is added to the
 * {@link GL3DLayeredView}. Furthermore it takes care of setting the currently
 * active image region by performing a ray casting using the
 * {@link GL3DRayTracer} to find the maximally spanning image region within the
 * displayed scene.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DSceneGraphView extends AbstractGL3DView implements GL3DView {
	private GL3DGroup root;

	// private GL3DImageGroup imageMeshes;
	private GLOverlayView overlayView = null;
	private GL3DImageLayers imageLayers;
	private GL3DHitReferenceShape hitReferenceShape;
	private GL3DFramebufferImage framebuffer;
	private GL3DGroup artificialObjects;

	private List<GL3DImageTextureView> layersToAdd = new ArrayList<GL3DImageTextureView>();
	private List<GL3DImageTextureView> layersToRemove = new ArrayList<GL3DImageTextureView>();

	private List<GL3DNode> nodesToDelete = new ArrayList<GL3DNode>();

	public GL3DSceneGraphView() {
		this.root = createRoot();
		printScenegraph();

		GL3DKeyController.getInstance().addListener(new GL3DKeyListener() {
			public void keyHit(KeyEvent e) {
				toggleCoronaVisibility();
				GuiState3DWCS.mainComponentView.getComponent().repaint();
				GuiState3DWCS.topToolBar.toogleCoronaButton();
				System.out.println("Toggling Corona Visibility");
			}
		}, KeyEvent.VK_X);
		
	}

	public void render3D(GL3DState state) {
		// set visible of arrows
		if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
			artificialObjects.drawBits.off(Bit.Hidden);
		else
			artificialObjects.drawBits.on(Bit.Hidden);

		GL2 gl = state.gl;

		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		gl.glBlendEquation(GL.GL_FUNC_ADD);
		deleteNodes(state);
		GL3DState.get().checkGLErrors("GL3DSceneGraph.afterDeleteNodes");

		if (this.getView() != null) {
			state.pushMV();
			this.renderChild(gl);
			GL3DState.get().checkGLErrors("GL3DSceneGraph.afterRenderChild");
			this.addLayersToSceneGraph(state);
			this.removeLayersFromSceneGraph(state);

			state.popMV();
		}

		GL3DState.get().checkGLErrors(
				"GL3DSceneGraph.afterApplyLayersToSceneGraph");

		if (state.activeCamera == null) {
			System.out.println("GL3DSceneGraph: Camera not ready, aborting renderpass");
			return;
		}
		// gl.glBlendFunc(GL.GL_ONE, GL.GL_DST_ALPHA);
		gl.glDisable(GL.GL_BLEND);
		gl.glEnable(GL.GL_DEPTH_TEST);

		state.pushMV();
		state.loadIdentity();
		this.root.update(state);
		state.popMV();
		GL3DState.get().checkGLErrors("GL3DSceneGraph.afterRootUpdate");

		state.pushMV();
		state.activeCamera.applyPerspective(state);
		state.activeCamera.applyCamera(state);
		GL3DState.get().checkGLErrors("GL3DSceneGraph.afterApplyCamera");

		if (overlayView != null)
			overlayView.preRender3D(state.gl);
		GL3DState.get().checkGLErrors("GL3DSceneGraph.afterPreRender3D");

		this.root.draw(state);

		if (overlayView != null)
			overlayView.postRender3D(state.gl);
		GL3DState.get().checkGLErrors("GL3DSceneGraph.afterPostRender3D");

		// Draw the camera or its interaction feedbacks
		state.activeCamera.drawCamera(state);

		// Resume Previous Projection
		state.activeCamera.resumePerspective(state);

		state.popMV();

		gl.glEnable(GL.GL_BLEND);
	}

	private void deleteNodes(GL3DState state) {
		for (GL3DNode node : this.nodesToDelete) {
			node.delete(state);
		}
		this.nodesToDelete.clear();
	}

	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		System.out.println("GL3DSceneGraphView.ViewChanged: Sender=" + newView
        + " Event=" + changeEvent);

		// Add Handler of Layer Events. Automatically add new Meshes for each
		// Layer
		if (newView.getAdapter(LayeredView.class) != null) {
			LayeredView layeredView = ((LayeredView) newView
					.getAdapter(LayeredView.class));
			layeredView.addViewListener(new ViewListener() {

				public void viewChanged(View sender, ChangeEvent aEvent) {
					// Log.debug("viewChange: sender : " + sender);
					if (aEvent.reasonOccurred(LayerChangedReason.class)) {
						LayerChangedReason reason = aEvent
								.getLastChangedReasonByType(LayerChangedReason.class);
						handleLayerChange(reason);
					}
				}
			});

			for (int i = 0; i < layeredView.getNumLayers(); i++) {
				View layer = layeredView.getLayer(i);
				if (layer != null)
					this.addNewLayer(layer
							.getAdapter(GL3DImageTextureView.class));
				System.out.println("GL3DSceneGraphView: Adding Layer to Scene form LayeredView "
                + layer);
			}
		}
	}

	private void handleLayerChange(LayerChangedReason reason) {
		GL3DImageTextureView imageTextureView = reason.getSubView().getAdapter(
				GL3DImageTextureView.class);
		if (imageTextureView != null) {

			switch (reason.getLayerChangeType()) {
			case LAYER_ADDED:
				addNewLayer(imageTextureView);
				break;
			case LAYER_REMOVED:
				removeLayer(imageTextureView);
				break;
			case LAYER_VISIBILITY:
				toggleLayerVisibility(imageTextureView);
				break;
			case LAYER_MOVED:
				moveLayerToIndex(imageTextureView, reason.getLayerIndex());
				break;
			case LAYER_DOWNLOADED:
				break;
			default:
				break;
			}
		} else {
			System.out.println("GL3DSceneGraphView: Cannot handle Layer Change for Layers without a GL3DImageTextureView!");
		}
	}

	private void moveLayerToIndex(GL3DImageTextureView view, int layerIndex) {
		System.out.println("GL3DSceneGraphView.moveLayerToIndex " + layerIndex);
		this.imageLayers.moveImages(view, layerIndex);
	}

	private void toggleLayerVisibility(GL3DImageTextureView view) {
		GL3DNode node = this.imageLayers.getImageLayerForView(view);
		if (node != null)
			node.drawBits.toggle(Bit.Hidden);
	}

	private void removeLayersFromSceneGraph(GL3DState state) {
		synchronized (this.layersToRemove) {
			for (GL3DImageTextureView imageTextureView : this.layersToRemove) {
				((GL3DCameraView) getAdapter(GL3DCameraView.class))
						.removeCameraListener(this.imageLayers
								.getImageLayerForView(imageTextureView));
				this.imageLayers.removeLayer(state, imageTextureView);
			}
			this.layersToRemove.clear();
		}
	}

	private void addLayersToSceneGraph(GL3DState state) {
		GL3DCamera camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();

		synchronized (this.layersToAdd) {
			for (GL3DImageTextureView imageTextureView : this.layersToAdd) {
				MetaData metaData = imageTextureView.getAdapter(
						MetaDataView.class).getMetaData();
				GL3DImageLayer imageLayer = new GL3DImageLayer(
						metaData.getFullName(), imageTextureView);

				((GL3DCameraView) getAdapter(GL3DCameraView.class))
						.addCameraListener(imageLayer);

				this.imageLayers.insertLayer(imageLayer);

				imageTextureView.addViewListener(framebuffer);

			}
			if (!this.layersToAdd.isEmpty()) {
				// If there is data, zoom to fit
				MetaDataView metaDataView = getAdapter(MetaDataView.class);
				if (metaDataView != null && metaDataView.getMetaData() != null) {
					View view = LayersModel.getSingletonInstance().getActiveView();
					PhysicalRegion region = view.getAdapter(MetaDataView.class).getMetaData()
							.getPhysicalRegion();
					double halfWidth = region.getWidth() / 2;
					double halfFOVRad = Math.toRadians(camera.getFOV() / 2);
					double distance = halfWidth
							* Math.sin(Math.PI / 2 - halfFOVRad)
							/ Math.sin(halfFOVRad);
					distance = -distance - camera.getZTranslation();
					// Log.debug("GL3DZoomFitAction: Distance = "+distance+" Existing Distance: "+camera.getZTranslation());
					camera.addCameraAnimation(new GL3DCameraZoomAnimation(
							distance, 500));
					GL3DCameraSelectorModel.getInstance().rotateToCurrentLayer(500);

				}
			}
			this.layersToAdd.clear();
		}
	}

	private void addNewLayer(GL3DImageTextureView imageTextureView) {
		synchronized (this.layersToAdd) {
			this.layersToAdd.add(imageTextureView);
		}
	}

	private void removeLayer(GL3DImageTextureView imageTextureView) {
		synchronized (this.layersToRemove) {
			if (!this.layersToRemove.contains(imageTextureView)
					&& this.imageLayers.getImageLayerForView(imageTextureView) != null) {
				this.layersToRemove.add(imageTextureView);
			}
		}
	}

	private GL3DGroup createRoot() {
		GL3DGroup root = new GL3DGroup("Scene Root");

		artificialObjects = new GL3DGroup("Artificial Objects");
		root.addNode(artificialObjects);

		this.imageLayers = new GL3DImageLayers();
		root.addNode(this.imageLayers);

		this.hitReferenceShape = new GL3DHitReferenceShape(true);
		root.addNode(this.hitReferenceShape);

		GL3DGroup indicatorArrows = new GL3DModel("Arrows");
		artificialObjects.addNode(indicatorArrows);

		GL3DShape north = new GL3DArrow("Northpole", Constants.SUN_RADIUS / 16,
				Constants.SUN_RADIUS, Constants.SUN_RADIUS / 2, 32,
				new Vector4d(1.0f, 0.2f, 0.1f, 1.0f));
		north.modelView().rotate(-Math.PI / 2, new Vector3d(1,0,0));
		indicatorArrows.addNode(north);

		GL3DShape south = new GL3DArrow("Southpole", Constants.SUN_RADIUS / 16,
				Constants.SUN_RADIUS, Constants.SUN_RADIUS / 2, 32,
				new Vector4d(0.1f, 0.2f, 1.0f, 1.0f));
		south.modelView().rotate(Math.PI / 2, new Vector3d(1,0,0));
		indicatorArrows.addNode(south);

		GL3DModel sunModel = new GL3DModel("Sun");
		artificialObjects.addNode(sunModel);
		// Create the sungrid
		// this.sun = new GL3DSphere("Sun-grid", Constants.SunRadius, 200, 200,
		// new GL3DVec4d(1.0f, 0.0f, 0.0f, 0.0f));
		// this.sun = new GL3DSunGrid(Constants.SunRadius,200,200, new
		// GL3DVec4d(0.8f, 0.8f, 0, 0.0f));

		// sunModel.addNode(this.sun);

		framebuffer = new GL3DFramebufferImage();
		artificialObjects.addNode(framebuffer);
		framebuffer.drawBits.on(Bit.Hidden);

		return root;
	}

	public GL3DHitReferenceShape getHitReferenceShape() {
		return hitReferenceShape;
	}

	public void toggleCoronaVisibility() {
		this.imageLayers.setCoronaVisibility(!this.imageLayers
				.getCoronaVisibility());
	}

	public void printScenegraph() {
		System.out.println("PRINTING SCENEGRAPH =======================>");

		printNode(root, 0);

	}

	public void setGLOverlayView(GLOverlayView overlayView) {
		this.overlayView = overlayView;
	}

	private void printNode(GL3DNode node, int level) {
		for (int i = 0; i < level; ++i)
			System.out.print("   ");

		if (node == null) {
			System.out.println("NULL");
			return;
		}

		System.out.println(node.getClass().getName() + " (" + node.name
				+ ")");

		if (node instanceof GL3DGroup) {
			GL3DGroup grp = (GL3DGroup) node;
			for (int i = 0; i < grp.numChildNodes(); ++i) {
				printNode(grp.getChild(i), level + 1);
			}
		}
	}

	protected void renderChild(GL2 gl) {
		if (view instanceof GLView) {
			((GLView) view).renderGL(gl, true);
			GL3DState.get().checkGLErrors("GL3DSceneGraph.afterRenderGL");
		} else {
			TEXTURE_HELPER.renderImageDataToScreen(gl,
					view.getAdapter(RegionView.class).getLastDecodedRegion(), view
							.getAdapter(SubimageDataView.class)
							.getImageData());
		}
	}

	public GL3DImageLayers getLayers() {
		return imageLayers;
	}

	public void markLayersAsChanged() {
		this.imageLayers.markChildsAsChanged();
	}
}
