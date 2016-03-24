package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.base.Telemetry;
import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.MainFrame;

public class ToggleFullscreenAction extends AbstractAction
{
    public ToggleFullscreenAction()
    {
        super("Toggle Fullscreen", IconBank.getIcon(JHVIcon.FULLSCREEN, 16, 16));
        putValue(SHORT_DESCRIPTION, "Toggle fullscreen");
        putValue(MNEMONIC_KEY, KeyEvent.VK_T);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_MASK));
    }

    public void actionPerformed(@Nullable ActionEvent e)
    {
        Telemetry.trackEvent("Fullscreen activated");
        
        if(MainFrame.SINGLETON.MAIN_PANEL.isFullscreen())
        	MainFrame.SINGLETON.MAIN_PANEL.switchToWindowed();
        else
        	MainFrame.SINGLETON.MAIN_PANEL.switchToFullscreen();
    }
}
