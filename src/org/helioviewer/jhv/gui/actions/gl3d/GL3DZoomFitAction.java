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
import org.helioviewer.jhv.opengl.camera.GL3DCameraZoomAnimation;
import org.helioviewer.jhv.viewmodel.region.PhysicalRegion;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJPXView;

/**
 * Action that zooms in or out to fit the currently displayed image layers to
 * the displayed viewport. For 3D this results in a change in the
 * {@link GL3DCamera}'s distance to the sun.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DZoomFitAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DZoomFitAction(boolean small) {
        super("Zoom to Fit", small ? IconBank.getIcon(JHVIcon.ZOOM_FIT_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_FIT));
        putValue(SHORT_DESCRIPTION, "Zoom to Fit");
        putValue(MNEMONIC_KEY, KeyEvent.VK_F);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.ALT_MASK));
    }

    public void actionPerformed(ActionEvent e) {
        View view = LayersModel.getSingletonInstance().getActiveView();
        GL3DCamera camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();
        if (view != null) {
            PhysicalRegion region = view.getAdapter(JHVJPXView.class).getMetaData().getPhysicalRegion();
            if (region != null) {
                double halfWidth = region.getWidth() / 2;
                Dimension canvasSize = GuiState3DWCS.mainComponentView.getCanavasSize();
                double aspect = canvasSize.getWidth() / canvasSize.getHeight();
                halfWidth = aspect < 1 ? halfWidth/aspect : halfWidth;
                double halfFOVRad = Math.toRadians(camera.getFOV() / 2.0);
                double distance = halfWidth * Math.sin(Math.PI / 2 - halfFOVRad) / Math.sin(halfFOVRad);
                distance = -distance - camera.getZTranslation();
                System.out.println("GL3DZoomFitAction: Distance = " + distance + " Existing Distance: " + camera.getZTranslation());
                camera.addCameraAnimation(new GL3DCameraZoomAnimation(distance, 500));
            }
        }
    }

}
