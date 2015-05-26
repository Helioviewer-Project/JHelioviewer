package org.helioviewer.jhv.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.helioviewer.jhv.gui.components.newComponents.MainFrame;
import org.helioviewer.jhv.viewmodel.view.opengl.MainPanel;

public class SetCameraTrackAction extends AbstractAction{ 

	/**
	 * 
	 */
	private static final long serialVersionUID = -2900463240807466097L;

	public SetCameraTrackAction() {
		super("Track");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		MainFrame.MAIN_PANEL.toggleTrack();
	}
}
