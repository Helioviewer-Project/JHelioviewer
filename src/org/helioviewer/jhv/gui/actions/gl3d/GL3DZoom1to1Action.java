package org.helioviewer.jhv.gui.actions.gl3d;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.layers.LayersModel;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DCameraPanAnimation;
import org.helioviewer.jhv.opengl.camera.GL3DCameraZoomAnimation;
import org.helioviewer.jhv.viewmodel.metadata.MetaData;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.MetaDataView;
import org.helioviewer.jhv.viewmodel.view.View;

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
			PhysicalRegion region = metaData.getPhysicalRegion();
            
			if (region != null) {
				Dimension dimension = GuiState3DWCS.mainComponentView.getCanavasSize();
				double minCanvasDimension = dimension.getHeight();
				
				/*
				 * > Die 1:1-Skalierung der Low-Resolution-Texturen ist in der Tat unterschiedlich:
				 * > Erst wenn man auf 200% heranzoomt, zeigt die halb-aufgel�ste Textur jedes SDO-Pixel
				 * > (da 0.5 x 200% = 100%). Sollen wir das �ndern, dass der 1:1-Knopf nur auf 100% Zoomt
				 * > und man mit Low-resolution-Texturen dann nur die H�lfte der SDO-Daten sieht? 
				 * 
				 * Ja, ich denke schon. Wenn der User �use high-res. textures� deaktiviert, weiss er,
				 * dass die r�umliche Informationsdichte reduziert wird, aber die Darstellungsskala sollte
				 * sich nicht �ndern. Falls das mentale UI-Modell vieler User anders sein sollte, werden wir
				 * das schnell h�ren, aber f�r den Moment denke ich, dass die meisten User erwarten, dass
				 * die r�umliche Darstellungsskala konstant bleibt.
				 */
				
		        /*if (!Boolean.parseBoolean(Settings.getProperty("default.display.highDPI")))
		        	minCanvasDimension /= 2.0;*/
	            double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
	            double distance = (minCanvasDimension/2.0 * unitsPerPixel) / Math.tan(halfFOVRad);
	            distance = -distance - camera.getZTranslation();
	            System.out.println("GL3DZoom1to1Action: Distance = " + distance
                + " Existing Distance: " + camera.getZTranslation());
				camera.addCameraAnimation(new GL3DCameraZoomAnimation(distance,
						500));
				/*camera.addCameraAnimation(new GL3DCameraPanAnimation(camera
                .getTranslation().negate()));*/
			}
		}
	}

}
