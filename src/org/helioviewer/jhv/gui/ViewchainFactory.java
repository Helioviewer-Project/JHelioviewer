package org.helioviewer.jhv.gui;

import java.util.AbstractList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.helioviewer.gl3d.view.GL3DLayeredView;
import org.helioviewer.jhv.gui.components.MoviePanel;
import org.helioviewer.jhv.gui.components.QualitySpinner;
import org.helioviewer.jhv.internal_plugins.selectedLayer.SelectedLayerPanel;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.GLInfo;
import org.helioviewer.viewmodel.factory.GLViewFactory;
import org.helioviewer.viewmodel.factory.ViewFactory;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.view.ComponentView;
import org.helioviewer.viewmodel.view.FilterView;
import org.helioviewer.viewmodel.view.HelioviewerGeometryView;
import org.helioviewer.viewmodel.view.ImageInfoView;
import org.helioviewer.viewmodel.view.LayeredView;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.ModifiableInnerViewView;
import org.helioviewer.viewmodel.view.MovieView;
import org.helioviewer.viewmodel.view.StandardSolarRotationTrackingView;
import org.helioviewer.viewmodel.view.SubimageDataView;
import org.helioviewer.viewmodel.view.SynchronizeView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.ViewListener;
import org.helioviewer.viewmodel.view.jp2view.JP2View;
import org.helioviewer.viewmodel.view.opengl.GLFilterView;
import org.helioviewer.viewmodel.view.opengl.GLHelioviewerGeometryView;
import org.helioviewer.viewmodel.view.opengl.GLLayeredView;
import org.helioviewer.viewmodel.view.opengl.GLOverlayView;
import org.helioviewer.viewmodelplugin.controller.PluginManager;
import org.helioviewer.viewmodelplugin.filter.FilterContainer;
import org.helioviewer.viewmodelplugin.filter.FilterTab;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor;
import org.helioviewer.viewmodelplugin.filter.FilterTabDescriptor.Type;
import org.helioviewer.viewmodelplugin.filter.FilterTabList;
import org.helioviewer.viewmodelplugin.filter.FilterTabPanelManager;
import org.helioviewer.viewmodelplugin.overlay.OverlayContainer;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponent;
import org.helioviewer.viewmodelplugin.overlay.OverlayControlComponentManager;
import org.helioviewer.viewmodelplugin.overlay.OverlayPanel;

/**
 * This class handles the buildup of a view chain.
 * 
 * The class follows the factory pattern. It is responsible to build up a view
 * chain in the right structure. It adds the corresponding views depending on
 * the chosen mode (software mode or OpenGL mode).
 * 
 * @author Markus Langenberg
 * @author Stephan Pagel
 * 
 */
public class ViewchainFactory {

	private ViewFactory viewFactory;

	/**
	 * Default constructor.
	 * 
	 * This constructor calls the constructor {@link #ViewchainFactory(boolean)}
	 * with the argument false. This results in that a BufferedImage view chain
	 * (Software Mode) will be created.
	 */
	public ViewchainFactory() {
		this(false);
	}

	/**
	 * Constructor which creates the view chain factory depending on the chosen
	 * and available mode.
	 * 
	 * Depending on the passed parameter value the constructor creates the used
	 * view factory as a OpenGL view factory (OpenGL mode) or a BufferedImage
	 * view factory (Software mode). If OpenGL mode is not available the
	 * parameter will be ignored and the software mode will be used.
	 * 
	 * @param useBufferedImageViewChain
	 *            indicates if the software mode has to be used.
	 */
	public ViewchainFactory(boolean useBufferedImageViewChain) {
		viewFactory = new GLViewFactory();

	}

	/**
	 * This method creates the main view chain depending on the selected mode.
	 * 
	 * The mode was defined when the instance of the class was created.
	 * <p>
	 * If the passed parameter value is null a new main view chain will be
	 * created. If the passed parameter value represents a
	 * {@link org.helioviewer.viewmodel.view.ComponentView} instance (the
	 * topmost view of the view chain) the existing view chain will be
	 * transfered with all its settings to a new view chain.
	 * 
	 * @param currentMainImagePanelView
	 *            instance of the ComponentView which is the topmost view of the
	 *            view chain which has to be transfered to the new one.
	 * @param keepSource
	 *            If true, the source view chain stays untouched, otherwise it
	 *            will be unusable afterwards
	 * @return instance of the ComponentView of the new created view chain.
	 */
	public ComponentView createViewchainMain(
			ComponentView currentMainImagePanelView, boolean keepSource) {
			return createNewViewchainMain();
		
	}

	/**
	 * This method creates the overview view chain always in software mode and
	 * if a main view chain already exists.
	 * 
	 * If there is no main view chain (represented by there topmost view, the
	 * ComponentView) than the method did nothing and returns a null value. The
	 * instance is needed to make synchronization available.
	 * <p>
	 * If a ComponentView of an existing view chain was passed to the method the
	 * whole view chain will be transfered with all its settings to a new view
	 * chain. If no ComponentView instance was passed the method creates a new
	 * overview view chain.
	 * 
	 * @param mainImagePanelView
	 *            the ComponentView instance of the main view chain which acts
	 *            as the observed view chain.
	 * @param currentOverviewImagePanelView
	 *            the ComponentView instance (or null) of an existing overview
	 *            view chain.
	 * @param keepSource
	 *            If true, the source view chain stays untouched, otherwise it
	 *            will be unusable afterwards
	 * @return the ComponentView of the new overview view chain or null if it
	 *         could not be created.
	 */
	/*
	public ComponentView createViewchainOverview(
			ComponentView mainImagePanelView,
			ComponentView currentOverviewImagePanelView, boolean keepSource) {
		if (mainImagePanelView == null)
			return null;

		//if (currentOverviewImagePanelView == null) {
		//	return createNewViewchainOverview(mainImagePanelView);
		} else {
			return createViewchainFromExistingViewchain(
					currentOverviewImagePanelView, mainImagePanelView,
					keepSource);
		}
	}*/

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

	

	/**
	 * Creates a new main view chain with the minimal needed views.
	 * 
	 * @return a instance of a ComponentView which is the topmost view of the
	 *         new chain.
	 */
	protected ComponentView createNewViewchainMain() {
		// Layered View
		GL3DLayeredView layeredView = new GL3DLayeredView();

		// Solar Rotation Tracking View
		StandardSolarRotationTrackingView trackingView = viewFactory
				.createNewView(StandardSolarRotationTrackingView.class);
		trackingView.setView(layeredView);

		// Component View
		ComponentView componentView = viewFactory
				.createNewView(ComponentView.class);
		componentView.setView(trackingView);

		return componentView;
	}
}
