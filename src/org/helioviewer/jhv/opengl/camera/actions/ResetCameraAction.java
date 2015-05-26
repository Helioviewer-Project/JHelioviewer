package org.helioviewer.jhv.opengl.camera.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;

public class ResetCameraAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    public ResetCameraAction() {
        super("Reset Camera", IconBank.getIcon(JHVIcon.NEW_CAMERA, 24, 24));
        putValue(SHORT_DESCRIPTION, "Reset Camera Position to Default");
        // putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        // putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
        // KeyEvent.ALT_MASK));
    }

    public void actionPerformed(ActionEvent e) {
    	MainFrame.MAIN_PANEL.resetCamera();
    }
}