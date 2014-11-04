package org.helioviewer.jhv.gui.actions.gl3d;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;
import org.helioviewer.jhv.opengl.camera.GL3DCameraZoomAnimation;

/**
 * Action that zooms out, which increases the {@link GL3DCamera}'s distance to
 * the sun.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DZoomOutAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DZoomOutAction(boolean small) {
        super("Zoom out", small ? IconBank.getIcon(JHVIcon.ZOOM_OUT_SMALL) : IconBank.getIcon(JHVIcon.ZOOM_OUT));
        putValue(SHORT_DESCRIPTION, "Zoom out x2");
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.ALT_MASK));
    }

    public void actionPerformed(ActionEvent e) {
        GL3DCamera camera = GL3DCameraSelectorModel.getInstance().getCurrentCamera();

        double distance = -camera.getDistanceToSunSurface() / 2;
        GL3DCameraSelectorModel.getInstance().getCurrentCamera().addCameraAnimation(new GL3DCameraZoomAnimation(distance, 500));
    }

}
