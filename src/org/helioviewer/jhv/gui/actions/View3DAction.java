package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.opengl.camera.CameraMode;

public class View3DAction extends AbstractAction
{
    public View3DAction()
    {
    }

    public void actionPerformed(ActionEvent e)
    {
    	CameraMode.set3DMode();
    }
}
