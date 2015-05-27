package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.helioviewer.jhv.gui.IconBank;
import org.helioviewer.jhv.gui.IconBank.JHVIcon;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;

/**
 * Action to switch fullscreen mode on and off.
 * 
 * <p>
 * In this case, "fullscreen" means hiding the panel at left side of the window.
 * 
 * @author Markus Langenberg
 */
public class ToggleFullscreenAction extends AbstractAction {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public ToggleFullscreenAction() {
        super("Toggle Fullscreen", IconBank.getIcon(JHVIcon.FULLSCREEN, 16, 16));
        putValue(SHORT_DESCRIPTION, "Toggle fullscreen");
        putValue(MNEMONIC_KEY, KeyEvent.VK_T);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.ALT_MASK));
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e) {
    	if (MainFrame.MAIN_PANEL.fullScreenMode)
    		MainFrame.MAIN_PANEL.toFullscreen();
    	else MainFrame.MAIN_PANEL.escapeFullscreen();
    }

}
