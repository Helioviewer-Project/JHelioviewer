package org.helioviewer.jhv.gui.states;

import java.util.AbstractList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.gui.GL3DCameraMouseController;
import org.helioviewer.gl3d.gui.GL3DCameraSelectorModel;
import org.helioviewer.gl3d.view.GL3DCameraView;
import org.helioviewer.gl3d.view.GL3DComponentView;
import org.helioviewer.gl3d.view.GL3DLayeredView;
import org.helioviewer.gl3d.view.GL3DSceneGraphView;
import org.helioviewer.gl3d.view.GL3DViewportView;
import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.gui.ViewListenerDistributor;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.QualitySpinner;
import org.helioviewer.jhv.gui.components.SideContentPane;
import org.helioviewer.jhv.gui.components.TopToolBar;
import org.helioviewer.jhv.gui.components.statusplugins.RenderModeStatusPanel;
import org.helioviewer.jhv.gui.interfaces.ImagePanelInputController;
import org.helioviewer.jhv.internal_plugins.selectedLayer.SelectedLayerPanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JP2View;
import org.helioviewer.viewmodel.view.opengl.GLFilterView;
import org.helioviewer.viewmodel.view.opengl.GLHelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.filter.FilterContainer;
import org.helioviewer.viewmodelplugin.filter.FilterTab;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.viewmodelplugin.filter.FilterTabList;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor.Type;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponent;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;
import org.helioviewer.viewmodelplugin.overlay.OverlayPanel;

public class GuiState3DWCS implements State {

    // private JPanel gl3dSettingsPanel;
    protected TopToolBar topToolBar;

    protected ComponentView mainComponentView;
    protected ComponentView overviewComponentView;

    protected RenderModeStatusPanel renderModeStatus;


    
    public GuiState3DWCS() {
        // Override the viewchainFactory with a specific 3D implementation
    }    

    public void addStateSpecificComponents(SideContentPane sideContentPane) {
        GL3DCameraSelectorModel.getInstance();
    }

    public void removeStateSpecificComponents(SideContentPane sideContentPane) {
        //this.modelPanel.destroy();
    }


    public void activate() {
        GL3DCameraSelectorModel.getInstance().activate(this.mainComponentView.getAdapter(GL3DSceneGraphView.class));
    }


    public void createViewChains() {
        Log.info("Start creating view chains");

        // Layered View
        GL3DLayeredView layeredView = new GL3DLayeredView();

        GLOverlayView overlayView = new GLOverlayView();
        overlayView.setView(layeredView);
        
        GL3DCameraView cameraView = new GL3DCameraView();
        cameraView.setView(overlayView);
        
        GL3DViewportView viewportView = new GL3DViewportView();
        viewportView.setView(cameraView);

        GL3DSceneGraphView sceneGraph = new GL3DSceneGraphView();
        sceneGraph.setView(viewportView);
        sceneGraph.setGLOverlayView(overlayView);
        
        mainComponentView = new GL3DComponentView();
        mainComponentView.setView(sceneGraph);
                
        // add Overlays (OvwelayView added before LayeredView and after
        // GL3DCameraView)
        updateOverlayViewsInViewchainMain(overlayView);

        ViewListenerDistributor.getSingletonInstance().setView(mainComponentView);
        // imageSelectorPanel.setLayeredView(mainComponentView.getAdapter(LayeredView.class));

    }


    public ViewStateEnum getType() {
        return ViewStateEnum.View3D;
    }

    public TopToolBar getTopToolBar() {
        if (topToolBar == null) {
            topToolBar = new TopToolBar();
        }
        return topToolBar;
    }

    public ComponentView getMainComponentView() {
        return mainComponentView;
    }

    public ComponentView getOverviewComponentView() {
        return overviewComponentView;
    }

    public RenderModeStatusPanel getRenderModeStatus() {
        return renderModeStatus;
    }

    public ImagePanelInputController getDefaultInputController() {
        return new GL3DCameraMouseController();
    }

    public boolean isOverviewPanelInteractionEnabled() {
        return false;
    }
    
    /**
	 * Updates all enabled overlay views in a given view chain. The method
	 * removes all existing from the view chain and after this it adds all
	 * enabled overlays.
	 * 
	 * @param componentView
	 *            the ComponentView instance of the view chain where to update
	 *            the overlay views.
	 */
	public void updateOverlayViewsInViewchainMain(GLOverlayView overlayView) {
		// /////////////
		// Remove all existing overlays
		// /////////////

		// remove overlay control components from GUI
		ImageViewerGui.getSingletonInstance().getLeftContentPane()
				.remove(OverlayPanel.class);

		// /////////////
		// Add all enabled overlays to view chain
		// /////////////

		// add overlay view to view chain
		// S. Meier, must be fixed, use just one overlayView with more then one
		// overlaysPlugin
		AbstractList<OverlayContainer> overlayContainerList = PluginManager
				.getSingeltonInstance().getOverlayContainers(true);
		OverlayControlComponentManager manager = new OverlayControlComponentManager();

		// FIXME: Simon Sp�rri, check if this can be done more nicely
		// View nextView = componentView.getView();

		for (int i = overlayContainerList.size() - 1; i >= 0; i--) {
			OverlayContainer container = overlayContainerList.get(i);
			container.installOverlay(overlayView, manager);

		}

		// add overlay control components to view chain
		for (OverlayControlComponent comp : manager.getAllControlComponents()) {
			ImageViewerGui.getSingletonInstance().getLeftContentPane()
					.add(comp.getTitle(), comp.getOverlayPanel(), false);
		}
	}

	/**
	 * Adds a new ImageInfoView to the main view chain and creates the
	 * corresponding user interface components.
	 * 
	 * The ImageInfoViews are always the undermost views in the view chain so
	 * they will be added as a new layer to the LayeredView. For this reason
	 * this method creates also the complete sub view chain (including the
	 * needed filter views) and add it to the LayeredView.
	 * <p>
	 * If one of the passed parameter values is null nothing will happen.
	 * 
	 * @param newLayer
	 *            new ImageInfoView for which to create the sub view chain as a
	 *            new layer.
	 * @param attachToViewchain
	 *            a view of the main view chain which is equal or over the
	 *            LayeredView.
	 */
	public void addLayerToViewchainMain(ImageInfoView newLayer,
			View attachToViewchain) {
		if (newLayer == null || attachToViewchain == null)
			return;

		// Fetch LayeredView
		LayeredView layeredView = attachToViewchain
				.getAdapter(LayeredView.class);

		synchronized (layeredView) {
			// wait until image is loaded
			while (newLayer.getAdapter(SubimageDataView.class)
					.getSubimageData() == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

			// Get meta data
			MetaData metaData = null;

			if (newLayer.getAdapter(MetaDataView.class) != null)
				metaData = newLayer.getAdapter(MetaDataView.class)
						.getMetaData();

			// Create list which manages all filter tabs
			FilterTabList tabList = new FilterTabList();

			// Adjust Panel for basic functions
			JPanel adjustPanel = new JPanel();
			adjustPanel.setLayout(new BoxLayout(adjustPanel,
					BoxLayout.PAGE_AXIS));

			FilterTabPanelManager compactPanelManager = new FilterTabPanelManager();
			tabList.add(new FilterTab(FilterTabDescriptor.Type.COMPACT_FILTER,
					"Internal Plugins", compactPanelManager));

			// If JP2View, add QualitySlider
			if (newLayer instanceof JP2View) {
				compactPanelManager.add(new QualitySpinner((JP2View) newLayer));
			}

			compactPanelManager.add(new SelectedLayerPanel(newLayer));

			// Add filter to view chain
			AbstractList<FilterContainer> filterContainerList = PluginManager
					.getSingeltonInstance().getFilterContainers(true);
			View nextView = newLayer;

			for (int i = filterContainerList.size() - 1; i >= 0; i--) {
				FilterContainer container = filterContainerList.get(i);

				GLFilterView filterView = new GLFilterView();
				filterView.setView(nextView);

				container.installFilter(filterView, tabList);

				if (filterView.getFilter() != null) {
					nextView = filterView;
				} else {
					filterView.setView(null);
				}
			}

			// Geometry
			if (metaData != null) {
				GLHelioviewerGeometryView geometryView = new GLHelioviewerGeometryView();
				geometryView.setView(nextView);
				nextView = geometryView;
			}

			// Add layer
			layeredView.addLayer(nextView);

			// Add JTabbedPane
			JTabbedPane tabbedPane = new JTabbedPane() {

				private static final long serialVersionUID = 1L;

				/**
				 * Override the setEnabled method in order to keep the
				 * containing components' enabledState synced with the
				 * enabledState of this component.
				 */
				public void setEnabled(boolean enabled) {
					for (int i = 0; i < this.getTabCount(); i++) {
						this.getComponentAt(i).setEnabled(enabled);
					}
				}
			};

			// tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);

			for (int i = 0; i < tabList.size(); i++) {
				FilterTab filterTab = tabList.get(i);
				if (filterTab.getType() == Type.COMPACT_FILTER) {
					tabbedPane.add(filterTab.getTitle(), filterTab
							.getPaneManager().createCompactPanel());
				}
			}
			for (int i = 0; i < tabList.size(); i++) {
				FilterTab filterTab = tabList.get(i);
				if (filterTab.getType() != Type.COMPACT_FILTER) {
					tabbedPane.add(filterTab.getTitle(), filterTab
							.getPaneManager().createPanel());
				}
			}

			ImageInfoView imageInfoView = nextView
					.getAdapter(ImageInfoView.class);

			// If MoviewView, add MoviePanel
			if (newLayer instanceof MovieView) {
				MoviePanel moviePanel = new MoviePanel((MovieView) newLayer);
				if (LayersModel.getSingletonInstance().isTimed(newLayer)) {
					LayersModel.getSingletonInstance().setLink(newLayer, true);
				}

				ImageViewerGui.getSingletonInstance().getMoviePanelContainer()
						.addLayer(imageInfoView, moviePanel);
			} else {
				MoviePanel moviePanel = new MoviePanel(null);
				ImageViewerGui.getSingletonInstance().getMoviePanelContainer()
						.addLayer(imageInfoView, moviePanel);
			}

			ImageViewerGui.getSingletonInstance().getFilterPanelContainer()
					.addLayer(imageInfoView, tabbedPane);
			ImageViewerGui
					.getSingletonInstance()
					.getLeftContentPane()
					.expand(ImageViewerGui.getSingletonInstance()
							.getFilterPanelContainer());
			LayersModel.getSingletonInstance().setActiveLayer(imageInfoView);
		}
	}

}
