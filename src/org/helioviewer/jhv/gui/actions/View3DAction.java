package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.opengl.camera.CameraMode;

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
    	CameraMode.set3DMode();
    }
}
