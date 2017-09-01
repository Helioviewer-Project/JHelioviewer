package org.helioviewer.jhv.plugins.samp;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.MainFrame;
import org.helioviewer.jhv.gui.MainPanel;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.opengl.camera.animation.CameraZoomAnimation;

public abstract class NotifySamp extends AbstractAction
{
	private static final String DESCRIPTION = "send metadata over SAMP";
	
    public NotifySamp(boolean small, boolean isMenuItem)
    {
        super(
        		isMenuItem ? DESCRIPTION : "SAMP"
        		,small ? IconBank.getIcon(JHVIcon.SAMP, 16, 16) : IconBank.getIcon(JHVIcon.SAMP, 24, 24));
        putValue(SHORT_DESCRIPTION, DESCRIPTION);
    }
}