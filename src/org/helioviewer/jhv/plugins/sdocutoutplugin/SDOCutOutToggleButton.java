package org.helioviewer.jhv.plugins.sdocutoutplugin;

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.ImageViewerGui;
import org.helioviewer.jhv.layers.LayersListener;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.View;

public class SDOCutOutToggleButton extends Component implements LayersListener,
		PropertyChangeListener {

	private static final long serialVersionUID = 1L;

	private JToggleButton sdoCutOutButton;

	public SDOCutOutToggleButton() {

		installButton();
	}

	private void initVisualComponents() {

		createButton();

		// register as layers listener
		LayersModel.getSingletonInstance().addLayersListener(this);
	}

	private void createButton() {
		sdoCutOutButton = new JToggleButton(new SDOCutOutAction());
		sdoCutOutButton.setSelected(false);
		sdoCutOutButton.setIcon(new ImageIcon(SDOCutOutPlugin3D
				.getResourceUrl(SDOCutOutSettings.ICON_FILENAME)));
		sdoCutOutButton.setToolTipText("Connect to SDO Cut-Out Service");
		sdoCutOutButton.setEnabled(true);
		sdoCutOutButton.setVerticalTextPosition(SwingConstants.BOTTOM);
		sdoCutOutButton.setHorizontalAlignment(SwingConstants.CENTER);
		sdoCutOutButton.setHorizontalTextPosition(SwingConstants.CENTER);
	}

	public void installButton() {
		initVisualComponents();
		ImageViewerGui.getSingletonInstance().addTopToolBarPlugin(this,
				sdoCutOutButton);
	}

	public void removeButton() {
		Container parent = sdoCutOutButton.getParent();

		if (parent != null) {
			LayersModel.getSingletonInstance().removeLayersListener(this);
			parent.remove(sdoCutOutButton);
			parent.repaint();
		}
	}

	@Override
	public void activeLayerChanged(int idx) {
		if (idx >= 0) {
			if (LayersModel.getSingletonInstance().getLayer(idx) != null) {
				MetaDataView metaDataView = LayersModel.getSingletonInstance()
						.getLayer(idx).getAdapter(MetaDataView.class);
				if (metaDataView != null && metaDataView.getMetaData() != null && metaDataView.getMetaData().getObservatory() != null){
				// FIXME: remove string comparison
				sdoCutOutButton.setEnabled(LayersModel.getSingletonInstance()
						.getLayer(idx).getAdapter(MetaDataView.class)
						.getMetaData().getObservatory().contains("SDO"));
				}
			}
			return;
		}
		sdoCutOutButton.setEnabled(false);
	}

	@Override
	public void layerAdded(int idx) {
	}

	@Override
	public void layerChanged(int idx) {
	}

	@Override
	public void layerDownloaded(int idx) {
	}

	@Override
	public void layerRemoved(View oldView, int oldIdx) {
	}

	@Override
	public void subImageDataChanged(int idx) {
	}

	@Override
	public void timestampChanged(int idx) {
	}

	@Override
	public void viewportGeometryChanged() {
	}

	@Override
	/*
	 * This method is called by the event firePropertyChange to add the plugin
	 * button in the TopToolBar
	 */
	public void propertyChange(PropertyChangeEvent evt) {
	}
}
