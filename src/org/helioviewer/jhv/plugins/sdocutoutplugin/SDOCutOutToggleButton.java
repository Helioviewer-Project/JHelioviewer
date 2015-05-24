package org.helioviewer.jhv.plugins.sdocutoutplugin;

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import org.helioviewer.jhv.gui.components.newComponents.MainFrame;

public class SDOCutOutToggleButton extends Component implements
		PropertyChangeListener {

	private static final long serialVersionUID = 1L;

	private JToggleButton sdoCutOutButton;

	public SDOCutOutToggleButton() {

		installButton();
	}

	private void initVisualComponents() {

		createButton();

		// register as layers listener
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
		MainFrame.SINGLETON.addTopToolBarPlugin(this,
				sdoCutOutButton);
	}

	public void removeButton() {
		Container parent = sdoCutOutButton.getParent();

		if (parent != null) {
			parent.remove(sdoCutOutButton);
			parent.repaint();
		}
	}

	@Override
	/*
	 * This method is called by the event firePropertyChange to add the plugin
	 * button in the TopToolBar
	 */
	public void propertyChange(PropertyChangeEvent evt) {
	}
}
