package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.Settings;
import org.helioviewer.jhv.gui.components.newComponents.MainFrame;

public class View3DAction extends AbstractAction
{

    private static final long serialVersionUID=1L;

    /**
     * Default constructor.
     */
    public View3DAction()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent e)
    {
        Settings.setProperty("startup.cameramode","3D");
        
        MainFrame.TOP_TOOL_BAR.set3DMode();
        MainFrame.MAIN_PANEL.repaintViewAndSynchronizedViews();
    }
}
