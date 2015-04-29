package org.helioviewer.jhv.gui.actions.newActions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.GuiState3DWCS;
import org.helioviewer.jhv.viewmodel.view.opengl.CompenentView;

public class SetCameraInteractionAction extends AbstractAction{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1199107637545677948L;
	private CompenentView.CAMERA_INTERACTION cameraInteraction;
	
	public SetCameraInteractionAction(String name, CompenentView.CAMERA_INTERACTION cameraInteraction) {
		super(name);
		this.cameraInteraction = cameraInteraction;
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		((CompenentView)GuiState3DWCS.mainComponentView).setActiveInteraction(cameraInteraction);
	}

}
