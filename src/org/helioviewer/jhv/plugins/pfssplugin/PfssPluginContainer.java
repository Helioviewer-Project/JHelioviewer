package org.helioviewer.jhv.plugins.pfssplugin;

import org.helioviewer.jhv.plugins.pfssplugin.data.PfssCache;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayControlComponent;
import org.helioviewer.jhv.plugins.viewmodelplugin.overlay.OverlayControlComponentManager;
import org.helioviewer.jhv.viewmodel.renderer.physical.PhysicalRenderer3d;
import org.helioviewer.jhv.viewmodel.view.OverlayView;
import org.helioviewer.jhv.viewmodel.view.opengl.OverlayPluginContainer;

/**
 * Plugincontainer for Pfss
 * 
 * @author Stefan Meier
 */
public class PfssPluginContainer extends OverlayContainer {

	private PfssCache pfssCache;
	private PfssPluginPanel pfssPluginPanel;

	public PfssPluginContainer() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void installOverlayImpl(OverlayView overlayView,
			OverlayControlComponentManager controlList) {
		pfssCache = new PfssCache();
		pfssPluginPanel = new PfssPluginPanel(pfssCache);
		OverlayPluginContainer overlayPluginContainer = new OverlayPluginContainer();
		overlayPluginContainer
				.setRenderer3d(new PfssPlugin3dRenderer(pfssCache));
		overlayView.addOverlay(overlayPluginContainer);
		controlList
				.add(new OverlayControlComponent(pfssPluginPanel, getName()));

	}

	/**
	 * {@inheritDoc}
	 */
	public String getName() {
		return "PFSS";
	}


	@Override
	public Class<? extends PhysicalRenderer3d> getOverlayClass() {
		return PfssPlugin3dRenderer.class;
	}

}
