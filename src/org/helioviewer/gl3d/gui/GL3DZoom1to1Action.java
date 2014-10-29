package org.helioviewer.gl3d.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.base.logging.Log;
import org.helioviewer.gl3d.camera.GL3DCamera;
import org.helioviewer.gl3d.camera.GL3DCameraPanAnimation;
import org.helioviewer.gl3d.camera.GL3DCameraZoomAnimation;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.viewmodel.metadata.MetaData;
import org.helioviewer.viewmodel.region.Region;
import org.helioviewer.viewmodel.view.MetaDataView;
import org.helioviewer.viewmodel.view.View;

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

						MetaData metaData = view.getAdapter(MetaDataView.class)
					.getMetaData();
			double unitsPerPixel = metaData.getUnitsPerPixel();
			Region region = metaData.getPhysicalRegion();
            
			if (region != null) {
				double halfWidth = region.getWidth() / 2;
	            double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
	            double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
	            distance = distance / region.getWidth() * (GuiState3DWCS.mainComponentView.getCanavasSize().getWidth() * unitsPerPixel);
	            distance = -distance - camera.getZTranslation();
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
