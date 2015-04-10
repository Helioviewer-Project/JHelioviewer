package org.helioviewer.jhv.viewmodel.view.opengl;

import org.helioviewer.jhv.viewmodel.renderer.physical.PhysicalRenderer3d;

public class OverlayPluginContainer
{
	private PhysicalRenderer3d renderer3d = null;
	private boolean postRender = true;
	
	public PhysicalRenderer3d getRenderer3d() {
		return renderer3d;
	}
	public void setRenderer3d(PhysicalRenderer3d renderer3d) {
		this.renderer3d = renderer3d;
	}
	
	public void setPostRender(boolean postRender){
		this.postRender = postRender;
	}
	
	public boolean getPostRender(){
		return this.postRender;
	}
}
