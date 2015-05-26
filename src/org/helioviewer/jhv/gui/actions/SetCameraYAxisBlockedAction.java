package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.opengl.camera.CameraRotationInteraction;

public class SetCameraYAxisBlockedAction extends AbstractAction{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7299338556983374213L;

	public SetCameraYAxisBlockedAction() {
		super("Lock Y-axis");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		CameraRotationInteraction.yAxisBlocked = !CameraRotationInteraction.yAxisBlocked;
	}
}
