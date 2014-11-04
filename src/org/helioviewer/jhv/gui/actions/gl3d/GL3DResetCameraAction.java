package org.helioviewer.jhv.gui.actions.gl3d;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.base.logging.Log;
import org.helioviewer.jhv.gui.GL3DCameraSelectorModel;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.opengl.camera.GL3DCamera;

/**
 * Action that resets the view transformation of the current {@link GL3DCamera}
 * to its default settings.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DResetCameraAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public GL3DResetCameraAction() {
        super("Reset Camera", IconBank.getIcon(JHVIcon.RESET));
        putValue(SHORT_DESCRIPTION, "Reset Camera Position to Default");
        // putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        // putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
        // KeyEvent.ALT_MASK));
    }

    public void actionPerformed(ActionEvent e) {
        GL3DCameraSelectorModel.getInstance().getCurrentCamera().reset();
        Log.debug("Reset Camera");
    }
}
