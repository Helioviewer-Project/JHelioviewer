package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.annotation.Nullable;
import javax.swing.AbstractAction;

import org.helioviewer.jhv.opengl.camera.CameraRotationInteraction;

public class SetCameraYAxisBlockedAction extends AbstractAction
{
	public SetCameraYAxisBlockedAction()
	{
		super("Lock Y-axis");
	}

	@Override
	public void actionPerformed(@Nullable ActionEvent e)
	{
		CameraRotationInteraction.yAxisBlocked = !CameraRotationInteraction.yAxisBlocked;
	}
}
