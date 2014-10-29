package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraPanAnimation;
import org.helioviewer.gl3d.camera.GL3DCameraZoomAnimation;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.states.GuiState3DWCS;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.RegionView;
import org.helioviewer.viewmodel.view.View;
import org.helioviewer.viewmodel.view.jp2view.JHVJP2View;

/**
 * Action that zooms in or out to fit the currently displayed image layers to
 * the displayed viewport. For 3D this results in a change in the
 * {@link GL3DCamera}'s distance to the sun.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DZoom1to1Action extends AbstractAction {

	private static final long serialVersionUID = 1L;

	public GL3DZoom1to1Action(boolean small) {
		super("Zoom 1:1", small ? IconBank.getIcon(JHVIcon.ZOOM_1TO1_SMALL)
				: IconBank.getIcon(JHVIcon.ZOOM_1TO1));
		putValue(SHORT_DESCRIPTION, "Zoom to Native Resolution");
		putValue(MNEMONIC_KEY, KeyEvent.VK_Z);
		putValue(ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.ALT_MASK));
	}

	public void actionPerformed(ActionEvent e) {
		View view = LayersModel.getSingletonInstance().getActiveView();
		GL3DCamera camera = GL3DCameraSelectorModel.getInstance()
				.getCurrentCamera();
		if (view != null) {
			double unitsPerPixel = view.getAdapter(MetaDataView.class)
					.getMetaData().getUnitsPerPixel();
			JHVJP2View regionView = view.getAdapter(JHVJP2View.class);
			Region imageRegion = ((JHVJP2View) regionView).getNewestRegion();

			Region region = new GuiState3DWCS()
					.getMainComponentView().getAdapter(RegionView.class)
					.getRegion();

			if (region != null && imageRegion != null) {
				double distance = camera.getZTranslation()
						* (new GuiState3DWCS()
								.getMainComponentView().getCanavasSize()
								.getWidth() * unitsPerPixel)
						/ imageRegion.getWidth();
				distance = distance - camera.getZTranslation();
				Log.debug("GL3DZoom1to1Action: Distance = " + distance
						+ " Existing Distance: " + camera.getZTranslation());
				camera.addCameraAnimation(new GL3DCameraZoomAnimation(distance,
						500));
				camera.addCameraAnimation(new GL3DCameraPanAnimation(camera
						.getTranslation().copy().negate()));
			}
		}
	}

}
