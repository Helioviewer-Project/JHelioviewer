package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;

import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.opengl.camera.animation.CameraRotationAnimation;
import org.helioviewer.jhv.opengl.camera.animation.CameraTranslationAnimation;

public class ResetCameraAction extends AbstractAction
{
    public ResetCameraAction()
    {
        super("Reset Camera", IconBank.getIcon(JHVIcon.NEW_CAMERA, 24, 24));
        putValue(SHORT_DESCRIPTION, "Reset Camera Position to Default");
        // putValue(MNEMONIC_KEY, KeyEvent.VK_R);
        // putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_COMMA,
        // KeyEvent.ALT_MASK));
    }

    public void actionPerformed(@Nullable ActionEvent e)
    {
		MainFrame.MAIN_PANEL.addCameraAnimation(new CameraRotationAnimation(MainFrame.MAIN_PANEL,MainFrame.MAIN_PANEL.getRotationEnd().inversed()));
		MainFrame.MAIN_PANEL.addCameraAnimation(new CameraTranslationAnimation(
				MainFrame.MAIN_PANEL,
				new Vector3d(0, 0, MainPanel.DEFAULT_CAMERA_DISTANCE)
					.subtract(MainFrame.MAIN_PANEL.getTranslationEnd())));
    }
}