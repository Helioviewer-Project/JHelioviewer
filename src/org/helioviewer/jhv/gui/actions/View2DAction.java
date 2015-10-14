package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;

import org.helioviewer.jhv.opengl.camera.CameraMode;

public class View2DAction extends AbstractAction
{
    public View2DAction()
    {
    }

    public void actionPerformed(@Nullable ActionEvent e)
    {
    	CameraMode.set2DMode();
    }
}
