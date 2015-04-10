package org.helioviewer.jhv.viewmodel.view.opengl;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.media.opengl.GL2;

import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.RegionChangedReason;
import org.helioviewer.jhv.viewmodel.changeevent.ViewChainChangedReason;
import org.helioviewer.jhv.viewmodel.renderer.physical.GLPhysicalRenderGraphics;
import org.helioviewer.jhv.viewmodel.view.LayeredView;
import org.helioviewer.jhv.viewmodel.view.OverlayView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewHelper;
/**
 * Implementation of OverlayView for rendering in OpenGL mode.
 * 
 * <p>
 * This class provides the capability to draw overlays in OpenGL2. Therefore it
 * manages a {@link PhysicalRenderer}, which is passed to the registered
 * renderer.
 * 
 * <p>
 * For further information about the role of the OverlayView within the view
 * chain, see {@link org.helioviewer.jhv.viewmodel.view.OverlayView}.
 * 
 * @author Markus Langenberg
 */
public class GLOverlayView extends AbstractGLView implements OverlayView{

	private LayeredView layeredView;
	private CopyOnWriteArrayList<OverlayPluginContainer> overlays = new CopyOnWriteArrayList<OverlayPluginContainer>();
	
	/**
	 * {@inheritDoc}
	 */
	protected void setViewSpecificImplementation(View newView,
			ChangeEvent changeEvent) {
		layeredView = ViewHelper.getViewAdapter(view, LayeredView.class);
	}

	/**
	 * {@inheritDoc}
	 */
	public void renderGL(GL2 gl, boolean nextView) {
		renderChild(gl);
		
		if (nextView) {

			GLPhysicalRenderGraphics glRenderGraphics = new GLPhysicalRenderGraphics(gl, view);
			Iterator<OverlayPluginContainer> iterator = this.overlays.iterator();
			while(iterator.hasNext()){
				OverlayPluginContainer overlay = iterator.next();
				if (overlay.getRenderer3d() != null && (layeredView == null || layeredView.getNumLayers() > 0)){ 
					overlay.getRenderer3d().render(glRenderGraphics);
				}
			}

		}
	}

	public void postRender3D(GL2 gl) {
		Iterator<OverlayPluginContainer> iterator = this.overlays.iterator();
		
		while(iterator.hasNext()){
			OverlayPluginContainer overlay = iterator.next();
			if (overlay.getRenderer3d() != null && (layeredView == null || layeredView.getNumLayers() > 0) && overlay.getPostRender()){
				GLPhysicalRenderGraphics glRenderGraphics = new GLPhysicalRenderGraphics(gl, view);
				overlay.getRenderer3d().render(glRenderGraphics);
			}
		}
	}

	public void preRender3D(GL2 gl) {
		GLPhysicalRenderGraphics glRenderGraphics = new GLPhysicalRenderGraphics(gl, view);
		Iterator<OverlayPluginContainer> iterator = this.overlays.iterator();
		
		while(iterator.hasNext()){
			OverlayPluginContainer overlay = iterator.next();
			if (overlay.getRenderer3d() != null && (layeredView == null || layeredView.getNumLayers() > 0) && !overlay.getPostRender()){
				overlay.getRenderer3d().render(glRenderGraphics);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void viewChanged(View sender, ChangeEvent aEvent) {
		//Log.debug("viewChange: sender : " + sender);
		if (aEvent != null && aEvent.reasonOccurred(RegionChangedReason.class)){
				
			Iterator<OverlayPluginContainer> iterator = this.overlays.iterator();
			//Log.debug("sender : " + sender);
			
			while(iterator.hasNext()){
				OverlayPluginContainer overlay = iterator.next();
				if (overlay.getRenderer3d() != null){
					overlay.getRenderer3d().viewChanged(sender);
				}
			}
		}
		if (aEvent != null && aEvent.reasonOccurred(ViewChainChangedReason.class)) {
			layeredView = ViewHelper.getViewAdapter(view, LayeredView.class);
		}

		super.viewChanged(sender, aEvent);
	}



	@Override
	public void addOverlay(OverlayPluginContainer overlayPluginContainer) {
		this.overlays.add(overlayPluginContainer);
	}

	@Override
	public CopyOnWriteArrayList<OverlayPluginContainer> getOverlays() {
		// TODO Auto-generated method stub
		return overlays;
	}

	@Override
	public void removeOverlay(int index) {
		// TODO Auto-generated method stub
		
	}

	@Override
	// Just implemented for exist plugin, for new one, pls don't use this function
	public void setRenderer(GLPhysicalRenderGraphics renderer) {
		// TODO Auto-generated method stub
		OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
		this.overlays.add(overlayPluginContainer);
	}
	
	public View getView(){
		return view;
	}
    
	@Override
	public void setOverlays(
			CopyOnWriteArrayList<OverlayPluginContainer> overlays) {
		this.overlays = overlays;
	}	
}
